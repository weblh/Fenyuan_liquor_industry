package com.fenyuan.liquor.modules.integration.cmcloud.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fenyuan.liquor.common.config.CmCloudProperties;
import com.fenyuan.liquor.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class CmCloudApiClient {

    private static final int PAGE_SIZE = 200;

    private final CmCloudProperties properties;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    public JSONArray fetchAll(String paramKey, Consumer<JSONObject> paramCustomizer) {
        List<JSONObject> all = new ArrayList<>();
        int pageIndex = 1;
        while (true) {
            JSONObject param = basePageParam(pageIndex);
            paramCustomizer.accept(param);
            JSONObject result = process(paramKey, param);
            JSONArray response = result.getJSONArray("response");
            if (response != null && !response.isEmpty()) {
                for (int i = 0; i < response.size(); i++) {
                    all.add(response.getJSONObject(i));
                }
            }
            if (response == null || response.isEmpty() || response.size() < PAGE_SIZE) {
                break;
            }
            pageIndex++;
        }
        return JSON.parseArray(JSON.toJSONString(all));
    }

    public JSONObject process(String paramKey, JSONObject paramJson) {
        String base = trimSlash(properties.getApiBaseUrl());
        String url = base + "/api/GraspServer/Process";
        try {
            if ("cloud".equalsIgnoreCase(properties.getApiMode())) {
                return callCloud(url, paramKey, paramJson);
            }
            return callLocal(url, paramKey, paramJson);
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("调用财贸 API 失败: {}", url, e);
            throw new BusinessException("财贸 API 不可达（请确认 CMCloud 已登录且 API 插件已启动:45100）: "
                    + e.getMessage());
        }
    }

    public void ping() {
        JSONObject param = basePageParam(1);
        param.put("VchType", 11);
        param.put("BeginDate", java.time.LocalDate.now().minusDays(1).toString());
        param.put("EndDate", java.time.LocalDate.now().toString());
        JSONObject result = process("DlySaleData", param);
        if (!"0".equals(String.valueOf(result.get("code")))) {
            throw new BusinessException("财贸 API 响应异常: " + result.getString("message"));
        }
    }

    private JSONObject callLocal(String url, String paramKey, JSONObject paramJson) throws IOException {
        FormBody body = new FormBody.Builder(StandardCharsets.UTF_8)
                .add("ParamKey", paramKey)
                .add("ParamJson", paramJson.toJSONString())
                .build();
        return execute(url, body);
    }

    private JSONObject callCloud(String url, String paramKey, JSONObject paramJson) throws IOException {
        if (!StringUtils.hasText(properties.getDbName())
                || !StringUtils.hasText(properties.getApiParam())
                || !StringUtils.hasText(properties.getSignKey())) {
            throw new BusinessException("云 API 模式需配置 cmcloud.db-name / api-param / sign-key");
        }
        Map<String, String> params = new LinkedHashMap<>();
        params.put("apiparam", properties.getApiParam());
        params.put("apitype", "query");
        params.put("dbname", properties.getDbName());
        params.put("interiorapi", "1");
        params.put("managename", "GraspCMServerApi.dll");
        params.put("mobile", StringUtils.hasText(properties.getMobile()) ? properties.getMobile() : "0");
        params.put("paramjson", paramJson.toJSONString());
        params.put("paramkey", paramKey);
        params.put("serviceid", StringUtils.hasText(properties.getServiceId()) ? properties.getServiceId() : "0");

        String signPlain = params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + e.getValue())
                .reduce("", String::concat);
        if (StringUtils.hasText(properties.getAppKey())) {
            signPlain += properties.getAppKey();
        }
        signPlain += properties.getSignKey();
        params.put("sign", DigestUtils.md5DigestAsHex(signPlain.getBytes(StandardCharsets.UTF_8)));

        FormBody.Builder builder = new FormBody.Builder(StandardCharsets.UTF_8);
        for (Map.Entry<String, String> e : params.entrySet()) {
            builder.add(e.getKey(), e.getValue());
        }
        return execute(url, builder.build());
    }

    private JSONObject execute(String url, FormBody body) throws IOException {
        Request request = new Request.Builder().url(url).post(body).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new BusinessException("财贸 API HTTP " + response.code());
            }
            JSONObject json = JSON.parseObject(response.body().string());
            String code = String.valueOf(json.get("code"));
            if (!"0".equals(code)) {
                throw new BusinessException("财贸 API 错误[" + code + "]: " + json.getString("message"));
            }
            return json;
        }
    }

    private JSONObject basePageParam(int pageIndex) {
        JSONObject param = new JSONObject();
        param.put("PageSize", PAGE_SIZE);
        param.put("PageIndex", pageIndex);
        return param;
    }

    private String trimSlash(String url) {
        if (!StringUtils.hasText(url)) {
            return "http://127.0.0.1:45100";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
