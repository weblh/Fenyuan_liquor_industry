package com.fenyuan.liquor.modules.business.pricecompare.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fenyuan.liquor.modules.business.common.BizErpRules;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 拉取京东/天猫官方店酒类实时价（汾酒元/件=瓶价×6；飞天茅台元/瓶）。
 */
@Slf4j
@Service
public class OnlinePriceCrawlService {

    private static final Pattern PRICE_NUM = Pattern.compile("([1-9][0-9]{1,4}(?:\\.[0-9]{1,2})?)");
    private static final Pattern JD_DATA_PRICE = Pattern.compile("data-price=[\"']([0-9]+(?:\\.[0-9]+)?)[\"']");
    private static final Pattern JD_JSON_PRICE = Pattern.compile(
            "\"(?:p|op|price|jdPrice|warePrice)\"\\s*:\\s*\"?([0-9]+(?:\\.[0-9]+)?)\"?");

    private static final Map<String, String> SEARCH_KEYWORDS = new LinkedHashMap<>();
    private static final Map<String, String> JD_SKU_IDS = new LinkedHashMap<>();

    static {
        SEARCH_KEYWORDS.put("42度青花20", "汾酒42度青花20 500ml");
        SEARCH_KEYWORDS.put("53度青花20", "汾酒53度青花20 500ml");
        SEARCH_KEYWORDS.put("53度青花30", "汾酒53度青花30 500ml");
        SEARCH_KEYWORDS.put("53度老白汾10", "汾酒53度老白汾10");
        SEARCH_KEYWORDS.put("53度巴拿马20", "汾酒53度巴拿马20 475ml");
        SEARCH_KEYWORDS.put("53度飞天茅台", "飞天茅台53度500ml");

        JD_SKU_IDS.put("53度青花20", "100012043978");
        JD_SKU_IDS.put("42度青花20", "100012043980");
        JD_SKU_IDS.put("53度飞天茅台", "100016034392");
    }

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(6, TimeUnit.SECONDS)
            .callTimeout(12, TimeUnit.SECONDS)
            .followRedirects(true)
            .retryOnConnectionFailure(false)
            .build();

    public PlatformPrice crawl(String canonicalProduct) {
        PlatformPrice result = new PlatformPrice();
        if (!StringUtils.hasText(canonicalProduct)) {
            return result;
        }
        BigDecimal jdBottle = fetchJdBottlePrice(canonicalProduct);
        BigDecimal tmallBottle = fetchTmallBottlePrice(canonicalProduct);
        // 天猫失败时不拿京东价冒充
        int pack = bottlesPerCase(canonicalProduct);
        if (jdBottle != null && jdBottle.compareTo(BigDecimal.ZERO) > 0) {
            result.setJdPrice(toCasePrice(jdBottle, pack));
        }
        if (tmallBottle != null && tmallBottle.compareTo(BigDecimal.ZERO) > 0) {
            result.setTmallPrice(toCasePrice(tmallBottle, pack));
        }
        return result;
    }

    private BigDecimal fetchJdBottlePrice(String canonical) {
        String sku = JD_SKU_IDS.get(canonical);
        if (StringUtils.hasText(sku)) {
            BigDecimal p = fetchJdPriceBySku(sku);
            if (p != null) {
                return p;
            }
            p = fetchJdItemPagePrice(sku);
            if (p != null) {
                return p;
            }
        }
        return fetchJdPriceBySearch(SEARCH_KEYWORDS.getOrDefault(canonical, canonical));
    }

    private BigDecimal fetchTmallBottlePrice(String canonical) {
        String keyword = SEARCH_KEYWORDS.getOrDefault(canonical, canonical);
        try {
            // 慢慢买公开比价页（对天猫/猫超较友好）
            String url = "https://s.manmanbuy.com/Default.aspx?key="
                    + URLEncoder.encode(keyword, "UTF-8");
            String html = httpGet(url, "https://www.manmanbuy.com/");
            if (!StringUtils.hasText(html)) {
                return null;
            }
            Matcher shop = Pattern.compile("天猫[\\s\\S]{0,160}?([1-9][0-9]{1,4}(?:\\.[0-9]{1,2})?)\\s*元")
                    .matcher(html);
            if (shop.find()) {
                return parsePrice(shop.group(1));
            }
            Matcher cat = Pattern.compile("猫超[\\s\\S]{0,160}?([1-9][0-9]{1,4}(?:\\.[0-9]{1,2})?)\\s*元")
                    .matcher(html);
            if (cat.find()) {
                return parsePrice(cat.group(1));
            }
            // 页面内通用现价
            Matcher now = Pattern.compile("现价[：:\\s]*([1-9][0-9]{1,4}(?:\\.[0-9]{1,2})?)")
                    .matcher(html);
            if (now.find()) {
                return parsePrice(now.group(1));
            }
        } catch (Exception e) {
            log.warn("天猫价抓取失败 {}: {}", canonical, e.getMessage());
        }
        return null;
    }

    private BigDecimal fetchJdPriceBySku(String skuId) {
        try {
            String url = "https://p.3.cn/prices/mgets?skuIds=J_" + skuId + "&type=1";
            String body = httpGet(url, "https://item.jd.com/" + skuId + ".html");
            if (!StringUtils.hasText(body)) {
                return null;
            }
            String trimmed = body.trim();
            if (trimmed.startsWith("(") && trimmed.endsWith(")")) {
                trimmed = trimmed.substring(1, trimmed.length() - 1);
            }
            JSONArray arr = JSON.parseArray(trimmed);
            if (arr == null || arr.isEmpty()) {
                return null;
            }
            JSONObject o = arr.getJSONObject(0);
            String p = firstNonBlank(o.getString("p"), o.getString("op"), o.getString("m"));
            return parsePrice(p);
        } catch (Exception e) {
            log.warn("京东SKU价失败 {}: {}", skuId, e.getMessage());
            return null;
        }
    }

    private BigDecimal fetchJdItemPagePrice(String skuId) {
        try {
            String html = httpGet("https://item.jd.com/" + skuId + ".html", "https://www.jd.com/");
            if (!StringUtils.hasText(html)) {
                return null;
            }
            Matcher m = Pattern.compile("\"p\"\\s*:\\s*\"([0-9]+(?:\\.[0-9]+)?)\"|price:\\s*\"?([0-9]+(?:\\.[0-9]+)?)\"?")
                    .matcher(html);
            if (m.find()) {
                return parsePrice(firstNonBlank(m.group(1), m.group(2)));
            }
            Matcher m2 = JD_DATA_PRICE.matcher(html);
            if (m2.find()) {
                return parsePrice(m2.group(1));
            }
        } catch (Exception e) {
            log.debug("京东商品页价失败 {}: {}", skuId, e.getMessage());
        }
        return null;
    }

    private BigDecimal fetchJdPriceBySearch(String keyword) {
        try {
            String url = "https://search.jd.com/Search?keyword="
                    + URLEncoder.encode(keyword, "UTF-8") + "&enc=utf-8";
            String html = httpGet(url, "https://www.jd.com/");
            if (!StringUtils.hasText(html)) {
                return null;
            }
            Matcher m1 = JD_DATA_PRICE.matcher(html);
            if (m1.find()) {
                return parsePrice(m1.group(1));
            }
            Matcher m2 = JD_JSON_PRICE.matcher(html);
            while (m2.find()) {
                BigDecimal p = parsePrice(m2.group(1));
                if (p != null && p.compareTo(new BigDecimal("80")) >= 0) {
                    return p;
                }
            }
        } catch (Exception e) {
            log.warn("京东搜索价失败 {}: {}", keyword, e.getMessage());
        }
        return null;
    }

    private String httpGet(String url, String referer) {
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                        + "(KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/json,application/xhtml+xml,*/*;q=0.8")
                .header("Accept-Language", "zh-CN,zh;q=0.9")
                .header("Referer", referer)
                .get()
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return null;
            }
            return response.body().string();
        } catch (Exception e) {
            log.debug("HTTP GET fail {}: {}", url, e.getMessage());
            return null;
        }
    }

    private static int bottlesPerCase(String canonical) {
        if (BizErpRules.isNetworkPriceOnlyProduct(canonical)
                || (canonical != null && canonical.contains("茅台"))) {
            return 1;
        }
        return 6;
    }

    private static BigDecimal toCasePrice(BigDecimal bottlePrice, int pack) {
        return bottlePrice.multiply(BigDecimal.valueOf(pack)).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal parsePrice(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String s = raw.trim();
        if ("0".equals(s) || "-1".equals(s)) {
            return null;
        }
        Matcher m = PRICE_NUM.matcher(s);
        if (!m.find()) {
            return null;
        }
        try {
            BigDecimal p = new BigDecimal(m.group(1));
            if (p.compareTo(new BigDecimal("30")) < 0 || p.compareTo(new BigDecimal("50000")) > 0) {
                return null;
            }
            return p;
        } catch (Exception e) {
            return null;
        }
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (StringUtils.hasText(v)) {
                return v.trim();
            }
        }
        return null;
    }

    @Data
    public static class PlatformPrice {
        private BigDecimal jdPrice;
        private BigDecimal tmallPrice;
    }
}
