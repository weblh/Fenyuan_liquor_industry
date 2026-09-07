package com.fenyuan.liquor.modules.kingdee.service;

import com.fenyuan.liquor.modules.kingdee.vo.KingdeeVoucherHeaderVO;

import com.fenyuan.liquor.modules.kingdee.vo.VoucherDetailVO;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
public class KingdeeVoucherQueryService {

    private static final Logger log = LoggerFactory.getLogger(KingdeeVoucherQueryService.class);

    private static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final String SESSION_HEADER = "kdservice-sessionid";
    private static final String EXECUTE_BILL_QUERY_PATH =
            "Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc";
    private static final String VIEW_PATH =
            "Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.View.common.kdsvc";

    public static final String ORG_FIELD_KEY = "FAccountBookID.FNumber";

    public static final String BANK_DIMENSION_FIELD = "FDETAILID.FFLEX14.FNumber";

    private static final int BASE_FIELD_COUNT = 14;

    private static final String BASE_FIELD_KEYS =
            "FDate,FYear,FPeriod,FVOUCHERGROUPID.FName,FVOUCHERGROUPNO,FExplanation,"
                    + "FAccountID.FNumber,FAccountID.FName,FCurrencyID.FName,FAmountFor,FDebit,FCredit,"
                    + "FCreatorId.FName,FCheckerId.FName";

    private static final List<String> ORG_FIELD_CANDIDATES = Arrays.asList(
            "FAccountBookID.FNumber",
            "FAccountBookID",
            "FAcctOrgId.FNumber",
            "FBookOrgId.FNumber",
            "FOrgId.FNumber",
            "FBookID.FNumber"
    );

    private static final Pattern ORG_KEY_PATTERN =
            Pattern.compile("(?i).*(Org|Book|AccountBook).*");

    private final OkHttpClient client;

    private volatile String resolvedOrgFieldKey = ORG_FIELD_KEY;

    public KingdeeVoucherQueryService() {
        this.client = KingdeeHttpSupport.newClient(30, 60, 60);
    }

    public String getResolvedOrgFieldKey() {
        return resolvedOrgFieldKey;
    }

    public Map<String, Object> probeOrgField(String kingdeeUrl, String sessionId, String formId,
                                              String year, String period, String orgCodeHint) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("formId", formId);
        result.put("probeSteps", new ArrayList<Map<String, Object>>());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> steps = (List<Map<String, Object>>) result.get("probeSteps");

        Map<String, Object> wildcardStep = runProbeStep(kingdeeUrl, sessionId, formId, year, period,
                "*", null, 1, "wildcard");
        steps.add(wildcardStep);

        if (Boolean.TRUE.equals(wildcardStep.get("success"))) {
            String orgField = extractOrgFieldFromWildcardResponse(
                    (String) wildcardStep.get("rawResponse"), orgCodeHint);
            if (orgField != null) {
                resolvedOrgFieldKey = orgField;
                result.put("success", true);
                result.put("orgFieldKey", orgField);
                result.put("method", "wildcard");
                result.put("rawResponsePreview", truncate((String) wildcardStep.get("rawResponse"), 2000));
                return result;
            }
        }

        if (orgCodeHint != null && !orgCodeHint.isEmpty()) {
            for (String candidate : ORG_FIELD_CANDIDATES) {
                Map<String, Object> candidateStep = runProbeStep(kingdeeUrl, sessionId, formId, year, period,
                        BASE_FIELD_KEYS + "," + candidate,
                        buildOrgFilter(candidate, orgCodeHint, year, period),
                        1, "candidate:" + candidate);
                steps.add(candidateStep);
                if (Boolean.TRUE.equals(candidateStep.get("success"))) {
                    resolvedOrgFieldKey = candidate.contains(".") ? candidate : candidate + ".FNumber";
                    result.put("success", true);
                    result.put("orgFieldKey", resolvedOrgFieldKey);
                    result.put("method", "candidate_filter");
                    result.put("rawResponsePreview", truncate((String) candidateStep.get("rawResponse"), 2000));
                    return result;
                }
            }
        }

        Map<String, Object> basicStep = runProbeStep(kingdeeUrl, sessionId, formId, year, period,
                BASE_FIELD_KEYS + ",FAccountBookID,FAccountBookID.FNumber",
                buildPeriodFilter(year, period), 3, "basic_no_org");
        steps.add(basicStep);

        if (Boolean.TRUE.equals(basicStep.get("success"))) {
            String orgField = inferOrgFieldFromBasicResponse(
                    (String) basicStep.get("rawResponse"), orgCodeHint);
            if (orgField != null) {
                resolvedOrgFieldKey = orgField;
                result.put("success", true);
                result.put("orgFieldKey", orgField);
                result.put("method", "basic_inference");
                result.put("rawResponsePreview", truncate((String) basicStep.get("rawResponse"), 2000));
                return result;
            }
        }

        Map<String, Object> viewStep = tryViewMetadata(kingdeeUrl, sessionId, formId);
        steps.add(viewStep);
        if (viewStep.get("orgFieldKey") != null) {
            resolvedOrgFieldKey = (String) viewStep.get("orgFieldKey");
            result.put("success", true);
            result.put("orgFieldKey", resolvedOrgFieldKey);
            result.put("method", "view_metadata");
            return result;
        }

        resolvedOrgFieldKey = ORG_FIELD_KEY;
        result.put("success", true);
        result.put("orgFieldKey", ORG_FIELD_KEY);
        result.put("method", "default_fallback");
        result.put("message", "通配符与 View 均不可用，已采用金蝶 GL_VOUCHER 标准账簿字段 FAccountBookID.FNumber");
        return result;
    }

    public List<VoucherDetailVO> queryVoucherDetails(String kingdeeUrl, String sessionId, String formId,
                                                     String accountingPeriod, String voucherNumberStart,
                                                     String voucherNumberEnd) {
        return queryVoucherDetails(kingdeeUrl, sessionId, formId, accountingPeriod,
                voucherNumberStart, voucherNumberEnd, "106", resolvedOrgFieldKey);
    }

    public List<VoucherDetailVO> queryVoucherDetails(String kingdeeUrl, String sessionId, String formId,
                                                     String accountingPeriod, String voucherNumberStart,
                                                     String voucherNumberEnd, String orgCompanyCode,
                                                     String orgFieldKey) {
        return queryVoucherDetails(kingdeeUrl, sessionId, formId, accountingPeriod,
                voucherNumberStart, voucherNumberEnd, orgCompanyCode, orgFieldKey, false);
    }

    public List<VoucherDetailVO> queryVoucherDetails(String kingdeeUrl, String sessionId, String formId,
                                                     String accountingPeriod, String voucherNumberStart,
                                                     String voucherNumberEnd, String orgCompanyCode,
                                                     String orgFieldKey, boolean includeBankDimension) {
        try {
            String queryUrl = kingdeeUrl + EXECUTE_BILL_QUERY_PATH;
            String effectiveOrgField = orgFieldKey != null && !orgFieldKey.isEmpty()
                    ? orgFieldKey : ORG_FIELD_KEY;
            List<VoucherDetailVO> all = new ArrayList<>();
            int startRow = 0;
            final int pageSize = 2000;
            // 分页拉取，避免单月凭证超过 Limit 时漏检银行分录
            while (true) {
                JSONObject requestBody = buildQueryRequestBody(formId, accountingPeriod,
                        voucherNumberStart, voucherNumberEnd, orgCompanyCode, effectiveOrgField,
                        includeBankDimension, startRow, pageSize);
                String responseBody = executePostRequest(queryUrl, sessionId, requestBody.toJSONString());
                List<VoucherDetailVO> page = parseResponse(responseBody, includeBankDimension);
                if (page == null || page.isEmpty()) {
                    break;
                }
                all.addAll(page);
                if (page.size() < pageSize) {
                    break;
                }
                startRow += pageSize;
                if (startRow > 20000) {
                    log.warn("凭证分录分页超过上限 period={} loaded={}", accountingPeriod, all.size());
                    break;
                }
            }
            return all;
        } catch (IOException e) {
            throw new RuntimeException("网络请求失败：" + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("查询凭证详情失败：" + e.getMessage(), e);
        }
    }

    private JSONObject buildQueryRequestBody(String formId, String accountingPeriod,
                                             String voucherNumberStart, String voucherNumberEnd,
                                             String orgCompanyCode, String orgFieldKey,
                                             boolean includeBankDimension) {
        return buildQueryRequestBody(formId, accountingPeriod, voucherNumberStart, voucherNumberEnd,
                orgCompanyCode, orgFieldKey, includeBankDimension, 0, 2000);
    }

    private JSONObject buildQueryRequestBody(String formId, String accountingPeriod,
                                             String voucherNumberStart, String voucherNumberEnd,
                                             String orgCompanyCode, String orgFieldKey,
                                             boolean includeBankDimension, int startRow, int limit) {
        JSONObject requestBody = new JSONObject();
        JSONObject data = new JSONObject();
        data.put("FormId", formId);
        String fieldKeys = BASE_FIELD_KEYS;
        if (includeBankDimension) {
            fieldKeys = fieldKeys + "," + BANK_DIMENSION_FIELD;
        }
        data.put("FieldKeys", fieldKeys + "," + orgFieldKey);
        data.put("Limit", limit);
        data.put("StartRow", startRow);
        data.put("TopRowCount", 0);

        StringBuilder filterString = new StringBuilder();
        if (accountingPeriod != null && !accountingPeriod.isEmpty()) {
            // FPeriod 在金蝶多为整数 7，避免传 07 导致期间过滤不到凭证
            String year = accountingPeriod.substring(0, 4);
            int periodNum = Integer.parseInt(accountingPeriod.substring(4).replaceAll("\\D", ""));
            filterString.append("FYear=").append(year)
                    .append(" AND FPeriod=").append(periodNum);
        }

        if (orgCompanyCode != null && !orgCompanyCode.isEmpty()) {
            if (filterString.length() > 0) {
                filterString.append(" AND ");
            }
            filterString.append(buildOrgFilter(orgFieldKey, orgCompanyCode, null, null));
        }

        if (voucherNumberStart != null && !voucherNumberStart.isEmpty()
                && voucherNumberEnd != null && !voucherNumberEnd.isEmpty()) {
            if (filterString.length() > 0) {
                filterString.append(" AND ");
            }
            filterString.append("FVOUCHERGROUPNO >= '").append(voucherNumberStart)
                    .append("' AND FVOUCHERGROUPNO <= '").append(voucherNumberEnd).append("'");
        }

        if (filterString.length() > 0) {
            data.put("FilterString", filterString.toString());
        }

        requestBody.put("data", data);
        return requestBody;
    }

    private String buildOrgFilter(String orgFieldKey, String orgCompanyCode, String year, String period) {
        String filter = orgFieldKey + " = '" + orgCompanyCode + "'";
        if (year != null && period != null) {
            return "FYear=" + year + " AND FPeriod=" + period + " AND " + filter;
        }
        return filter;
    }

    private String buildPeriodFilter(String year, String period) {
        if (year != null && period != null) {
            return "FYear=" + year + " AND FPeriod=" + period;
        }
        return "";
    }

    private Map<String, Object> runProbeStep(String kingdeeUrl, String sessionId, String formId,
                                             String year, String period, String fieldKeys,
                                             String filterString, int limit, String stepName) {
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("step", stepName);
        step.put("fieldKeys", fieldKeys);
        step.put("filterString", filterString);
        try {
            JSONObject data = new JSONObject();
            data.put("FormId", formId);
            data.put("FieldKeys", fieldKeys);
            data.put("Limit", limit);
            data.put("StartRow", 0);
            data.put("TopRowCount", 0);
            if (filterString != null && !filterString.isEmpty()) {
                data.put("FilterString", filterString);
            } else if (year != null && period != null) {
                data.put("FilterString", buildPeriodFilter(year, period));
            }
            JSONObject requestBody = new JSONObject();
            requestBody.put("data", data);

            String responseBody = executePostRequest(
                    kingdeeUrl + EXECUTE_BILL_QUERY_PATH, sessionId, requestBody.toJSONString());
            step.put("rawResponse", responseBody);

            if (isErrorResponse(responseBody)) {
                step.put("success", false);
                step.put("error", extractErrorMessage(responseBody));
            } else {
                step.put("success", true);
            }
        } catch (Exception e) {
            step.put("success", false);
            step.put("error", e.getMessage());
        }
        return step;
    }

    private Map<String, Object> tryViewMetadata(String kingdeeUrl, String sessionId, String formId) {
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("step", "view_metadata");
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("formid", formId);
            JSONObject data = new JSONObject();
            data.put("CreateOrgId", 0);
            data.put("Number", "");
            data.put("Id", "");
            requestBody.put("data", data);

            String responseBody = executePostRequest(
                    kingdeeUrl + VIEW_PATH, sessionId, requestBody.toJSONString());
            step.put("rawResponse", truncate(responseBody, 1000));

            if (!isErrorResponse(responseBody)) {
                JSONObject json = JSON.parseObject(responseBody);
                String orgField = findOrgFieldInJson(json, "106");
                if (orgField != null) {
                    step.put("success", true);
                    step.put("orgFieldKey", orgField);
                    return step;
                }
            }
            step.put("success", false);
            step.put("error", extractErrorMessage(responseBody));
        } catch (Exception e) {
            step.put("success", false);
            step.put("error", e.getMessage());
        }
        return step;
    }

    private String extractOrgFieldFromWildcardResponse(String responseBody, String orgCodeHint) {
        try {
            JSONArray outer = JSON.parseArray(responseBody);
            if (outer == null || outer.isEmpty()) {
                return null;
            }
            Object first = outer.get(0);
            if (first instanceof JSONObject) {
                return findOrgFieldInJson((JSONObject) first, orgCodeHint);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String inferOrgFieldFromBasicResponse(String responseBody, String orgCodeHint) {
        try {
            JSONArray outer = JSON.parseArray(responseBody);
            if (outer == null || outer.isEmpty()) {
                return null;
            }
            Object first = outer.get(0);
            if (first instanceof JSONArray) {
                JSONArray row = (JSONArray) first;
                if (row.size() >= 12 && orgCodeHint != null) {
                    String lastCol = row.getString(row.size() - 1);
                    String secondLast = row.getString(row.size() - 2);
                    if (orgCodeHint.equals(lastCol)) {
                        return ORG_FIELD_KEY;
                    }
                    if (orgCodeHint.equals(secondLast)) {
                        return "FAccountBookID";
                    }
                }
                if (row.size() >= 11) {
                    return ORG_FIELD_KEY;
                }
            }
        } catch (Exception ignored) {
        }
        return ORG_FIELD_KEY;
    }

    private String findOrgFieldInJson(Object node, String orgCodeHint) {
        if (node instanceof JSONObject) {
            JSONObject obj = (JSONObject) node;
            for (String key : obj.keySet()) {
                Object val = obj.get(key);
                if (ORG_KEY_PATTERN.matcher(key).matches()) {
                    if (orgCodeHint != null && orgCodeHint.equals(String.valueOf(val))) {
                        return key.contains(".FNumber") ? key : key + ".FNumber";
                    }
                    if (val instanceof JSONObject) {
                        JSONObject sub = (JSONObject) val;
                        if (orgCodeHint != null && orgCodeHint.equals(sub.getString("FNumber"))) {
                            return key + ".FNumber";
                        }
                    }
                }
                String nested = findOrgFieldInJson(val, orgCodeHint);
                if (nested != null) {
                    return nested;
                }
            }
        } else if (node instanceof JSONArray) {
            JSONArray arr = (JSONArray) node;
            for (int i = 0; i < arr.size(); i++) {
                String nested = findOrgFieldInJson(arr.get(i), orgCodeHint);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private boolean isErrorResponse(String responseBody) {
        if (responseBody == null || responseBody.isEmpty()) {
            return true;
        }
        if (responseBody.contains("\"IsSuccess\":false") || responseBody.contains("ErrorCode")) {
            try {
                if (responseBody.trim().startsWith("[")) {
                    JSONArray outer = JSON.parseArray(responseBody);
                    if (outer != null && !outer.isEmpty()) {
                        Object first = outer.get(0);
                        if (first instanceof JSONArray) {
                            Object inner = ((JSONArray) first).get(0);
                            if (inner instanceof JSONObject) {
                                return isErrorJson((JSONObject) inner);
                            }
                        } else if (first instanceof JSONObject) {
                            return isErrorJson((JSONObject) first);
                        }
                    }
                } else if (responseBody.trim().startsWith("{")) {
                    return isErrorJson(JSON.parseObject(responseBody));
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private boolean isErrorJson(JSONObject obj) {
        JSONObject result = obj.getJSONObject("Result");
        if (result == null) {
            result = obj;
        }
        JSONObject status = result.getJSONObject("ResponseStatus");
        if (status != null) {
            return !status.getBooleanValue("IsSuccess");
        }
        return false;
    }

    private String extractErrorMessage(String responseBody) {
        try {
            if (responseBody.trim().startsWith("[")) {
                JSONArray outer = JSON.parseArray(responseBody);
                if (outer != null && !outer.isEmpty()) {
                    Object first = outer.get(0);
                    JSONObject obj = null;
                    if (first instanceof JSONArray && !((JSONArray) first).isEmpty()) {
                        obj = ((JSONArray) first).getJSONObject(0);
                    } else if (first instanceof JSONObject) {
                        obj = (JSONObject) first;
                    }
                    if (obj != null) {
                        return extractErrorFromObj(obj);
                    }
                }
            } else {
                return extractErrorFromObj(JSON.parseObject(responseBody));
            }
        } catch (Exception ignored) {
        }
        return responseBody;
    }

    private String extractErrorFromObj(JSONObject obj) {
        JSONObject result = obj.getJSONObject("Result");
        if (result == null) {
            result = obj;
        }
        JSONObject status = result.getJSONObject("ResponseStatus");
        if (status != null) {
            String msg = status.getString("Msg");
            if (msg != null && !msg.isEmpty()) {
                return msg;
            }
            JSONArray errors = status.getJSONArray("Errors");
            if (errors != null && !errors.isEmpty()) {
                return errors.getJSONObject(0).getString("Message");
            }
        }
        return obj.toJSONString();
    }

    private String executePostRequest(String url, String sessionId, String jsonBody) throws IOException {
        RequestBody body = RequestBody.create(jsonBody, JSON_TYPE);

        Request.Builder builder = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json; charset=utf-8");

        if (sessionId != null && !sessionId.isEmpty()) {
            builder.addHeader(SESSION_HEADER, sessionId);
        }

        try (Response response = client.newCall(builder.build()).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败，响应码：" + response.code());
            }
            return response.body().string();
        }
    }

    private List<VoucherDetailVO> parseResponse(String responseBody, boolean includeBankDimension) {
        List<VoucherDetailVO> voucherDetailList = new ArrayList<>();

        if (isErrorResponse(responseBody)) {
            throw new RuntimeException("金蝶接口返回错误：" + extractErrorMessage(responseBody));
        }

        try {
            JSONArray outerArray = JSON.parseArray(responseBody);
            if (outerArray == null || outerArray.isEmpty()) {
                return voucherDetailList;
            }

            Object firstElement = outerArray.get(0);
            if (firstElement instanceof JSONArray) {
                for (int i = 0; i < outerArray.size(); i++) {
                    JSONArray row = outerArray.getJSONArray(i);
                    VoucherDetailVO vo = mapRowToVo(row, includeBankDimension);
                    if (vo != null) {
                        voucherDetailList.add(vo);
                    }
                }
            } else if (firstElement instanceof JSONObject) {
                JSONObject responseObj = (JSONObject) firstElement;
                JSONObject result = responseObj.getJSONObject("Result");
                if (result != null) {
                    JSONArray resultDataArray = result.getJSONArray("Result");
                    if (resultDataArray != null) {
                        for (int i = 0; i < resultDataArray.size(); i++) {
                            VoucherDetailVO vo = mapRowToVo(resultDataArray.getJSONArray(i), includeBankDimension);
                            if (vo != null) {
                                voucherDetailList.add(vo);
                            }
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("解析金蝶响应失败：" + e.getMessage(), e);
        }

        return voucherDetailList;
    }

    private VoucherDetailVO mapRowToVo(JSONArray row, boolean includeBankDimension) {
        if (row == null || row.size() < 12) {
            return null;
        }
        VoucherDetailVO vo = new VoucherDetailVO();
        vo.setFdate(row.getString(0));
        vo.setFyear(row.getString(1));
        vo.setFperiod(row.getString(2));
        vo.setFbillTypeID(row.getString(3));
        vo.setFvoucherNo(row.getString(4));
        vo.setFexplanation(row.getString(5));
        vo.setFaccountID(row.getString(6));
        vo.setFaccountName(row.getString(7));
        vo.setFcurrencyID(row.getString(8));
        vo.setFamountFor(toBigDecimal(row.get(9)));
        vo.setFdebit(toBigDecimal(row.get(10)));
        vo.setFcredit(toBigDecimal(row.get(11)));
        if (row.size() >= 13) {
            vo.setFposterID(row.getString(12));
        }
        if (row.size() >= 14) {
            vo.setFauditorID(row.getString(13));
        }
        int next = BASE_FIELD_COUNT;
        if (includeBankDimension && row.size() > next) {
            vo.setFbankDimension(row.getString(next));
            next++;
        }
        if (row.size() > next) {
            vo.setFaccountBookNumber(row.getString(next));
        }
        return vo;
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) {
            return null;
        }
        if (val instanceof BigDecimal) {
            return (BigDecimal) val;
        }
        return new BigDecimal(val.toString());
    }


    public String findVoucherGroupNumberFromAccountBook(String kingdeeUrl, String sessionId,
                                                        String formId, String orgCompanyCode,
                                                        String preferredGroupName) {
        if (!StringUtils.hasText(orgCompanyCode)) {
            return null;
        }
        String fromOrdered = queryVoucherGroupFromAccountBook(
                kingdeeUrl, sessionId, formId, orgCompanyCode, preferredGroupName, true);
        if (StringUtils.hasText(fromOrdered)) {
            return fromOrdered;
        }
        return queryVoucherGroupFromAccountBook(
                kingdeeUrl, sessionId, formId, orgCompanyCode, preferredGroupName, false);
    }

    private String queryVoucherGroupFromAccountBook(String kingdeeUrl, String sessionId,
                                                    String formId, String orgCompanyCode,
                                                    String preferredGroupName, boolean withOrder) {
        try {
            JSONObject data = new JSONObject();
            data.put("FormId", StringUtils.hasText(formId) ? formId : "GL_VOUCHER");
            data.put("FieldKeys", "FVOUCHERGROUPID.FNumber,FVOUCHERGROUPID.FName,FDate");
            data.put("FilterString", ORG_FIELD_KEY + " = '" + escapeFilterValue(orgCompanyCode.trim()) + "'");
            if (withOrder) {
                data.put("OrderString", "FDate DESC");
            }
            data.put("Limit", 100);
            data.put("StartRow", 0);
            data.put("TopRowCount", 0);
            JSONObject requestBody = new JSONObject();
            requestBody.put("data", data);
            String responseBody = executePostRequest(
                    kingdeeUrl + EXECUTE_BILL_QUERY_PATH, sessionId, requestBody.toJSONString());
            if (isErrorResponse(responseBody)) {
                log.warn("按账簿查询凭证字失败 org={} order={} resp={}",
                        orgCompanyCode, withOrder, truncate(responseBody, 300));
                return null;
            }
            String preferred = preferredGroupName != null ? preferredGroupName.trim() : "";
            String fallback = null;
            JSONArray outer = JSON.parseArray(responseBody);
            if (outer == null) {
                return null;
            }
            for (int i = 0; i < outer.size(); i++) {
                Object el = outer.get(i);
                if (!(el instanceof JSONArray)) {
                    continue;
                }
                JSONArray row = (JSONArray) el;
                if (row.isEmpty()) {
                    continue;
                }
                String number = row.getString(0);
                String name = row.size() > 1 ? row.getString(1) : null;
                if (!StringUtils.hasText(number)) {
                    continue;
                }
                if (StringUtils.hasText(preferred) && preferred.equals(name)) {
                    return number.trim();
                }
                if (fallback == null) {
                    fallback = number.trim();
                }
            }
            return fallback;
        } catch (Exception e) {
            log.warn("按账簿反查凭证字异常 org={}：{}", orgCompanyCode, e.getMessage());
            return null;
        }
    }

    public List<String> listVoucherGroupLabels(String kingdeeUrl, String sessionId) {
        List<String> labels = new ArrayList<>();
        try {
            JSONObject data = new JSONObject();
            data.put("FormId", "BD_VOUCHERGROUP");
            data.put("FieldKeys", "FNumber,FName");
            data.put("Limit", 50);
            data.put("StartRow", 0);
            data.put("TopRowCount", 0);
            JSONObject requestBody = new JSONObject();
            requestBody.put("data", data);
            String responseBody = executePostRequest(
                    kingdeeUrl + EXECUTE_BILL_QUERY_PATH, sessionId, requestBody.toJSONString());
            if (isErrorResponse(responseBody)) {
                return labels;
            }
            JSONArray outer = JSON.parseArray(responseBody);
            if (outer == null) {
                return labels;
            }
            for (int i = 0; i < outer.size(); i++) {
                Object el = outer.get(i);
                if (!(el instanceof JSONArray)) {
                    continue;
                }
                JSONArray row = (JSONArray) el;
                if (row.isEmpty()) {
                    continue;
                }
                String number = row.getString(0);
                String name = row.size() > 1 ? row.getString(1) : "";
                if (StringUtils.hasText(number)) {
                    labels.add(number.trim() + "=" + (name != null ? name.trim() : ""));
                }
            }
        } catch (Exception e) {
            log.warn("列出凭证字档案异常：{}", e.getMessage());
        }
        return labels;
    }

    public String findVoucherGroupNumberByName(String kingdeeUrl, String sessionId, String groupName) {
        if (groupName == null || groupName.trim().isEmpty()) {
            return null;
        }
        String trimmed = groupName.trim();
        String byExact = queryVoucherGroupNumber(kingdeeUrl, sessionId,
                "FName = '" + escapeFilterValue(trimmed) + "'");
        if (StringUtils.hasText(byExact)) {
            return byExact;
        }
        String byLike = queryVoucherGroupNumber(kingdeeUrl, sessionId,
                "FName like '%" + escapeFilterValue(trimmed) + "%'");
        if (StringUtils.hasText(byLike)) {
            return byLike;
        }
        return queryVoucherGroupNumber(kingdeeUrl, sessionId,
                "FNumber = '" + escapeFilterValue(trimmed) + "'");
    }

    private String queryVoucherGroupNumber(String kingdeeUrl, String sessionId, String filterString) {
        try {
            JSONObject data = new JSONObject();
            data.put("FormId", "BD_VOUCHERGROUP");
            data.put("FieldKeys", "FNumber,FName");
            data.put("FilterString", filterString);
            data.put("Limit", 10);
            data.put("StartRow", 0);
            data.put("TopRowCount", 0);
            JSONObject requestBody = new JSONObject();
            requestBody.put("data", data);
            String responseBody = executePostRequest(
                    kingdeeUrl + EXECUTE_BILL_QUERY_PATH, sessionId, requestBody.toJSONString());
            if (isErrorResponse(responseBody)) {
                return null;
            }
            JSONArray outerArray = JSON.parseArray(responseBody);
            if (outerArray == null || outerArray.isEmpty()) {
                return null;
            }
            Object firstElement = outerArray.get(0);
            if (firstElement instanceof JSONArray) {
                JSONArray row = (JSONArray) firstElement;
                return row.size() > 0 ? row.getString(0) : null;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 解析计量单位编码：科目默认单位 → FNumber → 名称（个/次/项）。
     * 开票收入数量金额核算必须使用账套中真实存在的单位编码，否则金蝶会清空单位并报「单位不能为空」。
     */
    public String findMeasureUnitNumber(String kingdeeUrl, String sessionId, String preferredNumber) {
        return findMeasureUnitNumber(kingdeeUrl, sessionId, preferredNumber, null);
    }

    public String findMeasureUnitNumber(String kingdeeUrl, String sessionId,
                                        String preferredNumber, String accountNumber) {
        // 0) 历史转字「开票收入」分录上的单位（与账套实际用法一致）
        String fromHistory = findUnitNumberFromIncomeHistory(kingdeeUrl, sessionId, accountNumber);
        if (StringUtils.hasText(fromHistory)) {
            log.info("计量单位取自历史开票收入分录 → {}", fromHistory);
            return fromHistory;
        }
        // 1) 按科目名称「开票收入」查科目档案（配置编码可能与账套不一致）
        String fromNamedAccount = findUnitNumberFromAccountByName(kingdeeUrl, sessionId, "开票收入");
        if (StringUtils.hasText(fromNamedAccount)) {
            log.info("计量单位取自科目名「开票收入」 → {}", fromNamedAccount);
            return fromNamedAccount;
        }
        // 2) 科目档案上的默认计量单位
        if (StringUtils.hasText(accountNumber)) {
            String fromAccount = findUnitNumberFromAccount(kingdeeUrl, sessionId, accountNumber.trim());
            if (StringUtils.hasText(fromAccount)) {
                log.info("计量单位取自科目 {} → {}", accountNumber, fromAccount);
                return fromAccount;
            }
        }
        LinkedHashSet<String> numberCandidates = new LinkedHashSet<>();
        if (StringUtils.hasText(preferredNumber)) {
            numberCandidates.add(preferredNumber.trim());
        }
        numberCandidates.addAll(Arrays.asList("Pcs", "pcs", "ge", "Ge", "jian", "Jian", "ci", "Ci", "xiang", "01", "001", "T"));
        for (String formId : new String[]{"BD_UNIT", "BOS_UNIT", "BD_MeasureUnit"}) {
            for (String num : numberCandidates) {
                String hit = queryDimensionNumber(kingdeeUrl, sessionId, formId,
                        "FNumber,FName",
                        "FNumber = '" + escapeFilterValue(num) + "'", false);
                if (StringUtils.hasText(hit)) {
                    log.info("计量单位按编码命中 form={} number={} → {}", formId, num, hit);
                    return hit;
                }
            }
            for (String name : new String[]{"个", "次", "项", "台", "件", "辆"}) {
                String hit = queryDimensionNumber(kingdeeUrl, sessionId, formId,
                        "FNumber,FName",
                        "FName = '" + escapeFilterValue(name) + "'", false);
                if (StringUtils.hasText(hit)) {
                    log.info("计量单位按名称命中 form={} name={} → {}", formId, name, hit);
                    return hit;
                }
            }
        }
        return null;
    }

    /** 从已有转字凭证的收入贷方分录取计量单位编码。 */
    public String findUnitNumberFromIncomeHistory(String kingdeeUrl, String sessionId, String accountNumber) {
        String formId = "GL_VOUCHER";
        String[] fieldKeyTrials = new String[]{
                "FAccountID.FNumber,FAccountID.FName,FUnitID.FNumber,FCREDIT,FDEBIT",
                "FAccountID.FNumber,FAccountID.FName,FUnitId.FNumber,FCREDIT,FDEBIT"
        };
        StringBuilder filter = new StringBuilder();
        filter.append("(FCREDIT<>0)");
        if (StringUtils.hasText(accountNumber)) {
            filter.append(" AND FAccountID.FNumber like '")
                    .append(escapeFilterValue(accountNumber.trim().split("\\.")[0]))
                    .append("%'");
        } else {
            filter.append(" AND (FAccountID.FName like '%开票收入%' OR FAccountID.FNumber like '6001%')");
        }
        for (String fieldKeys : fieldKeyTrials) {
            try {
                JSONObject data = new JSONObject();
                data.put("FormId", formId);
                data.put("FieldKeys", fieldKeys);
                data.put("FilterString", filter.toString());
                data.put("Limit", 50);
                data.put("StartRow", 0);
                data.put("TopRowCount", 0);
                data.put("OrderString", "FDate desc");
                JSONObject requestBody = new JSONObject();
                requestBody.put("data", data);
                String responseBody = executePostRequest(
                        kingdeeUrl + EXECUTE_BILL_QUERY_PATH, sessionId, requestBody.toJSONString());
                if (isErrorResponse(responseBody)) {
                    continue;
                }
                JSONArray outer = JSON.parseArray(responseBody);
                if (outer == null || outer.isEmpty()) {
                    continue;
                }
                for (int i = 0; i < outer.size(); i++) {
                    Object el = outer.get(i);
                    if (!(el instanceof JSONArray)) {
                        continue;
                    }
                    JSONArray row = (JSONArray) el;
                    if (row.size() < 3) {
                        continue;
                    }
                    String acctName = row.size() > 1 ? row.getString(1) : "";
                    String unitNo = row.getString(2);
                    if (!StringUtils.hasText(unitNo)) {
                        continue;
                    }
                    if (StringUtils.hasText(acctName) && acctName.contains("开票收入")) {
                        return unitNo.trim();
                    }
                    if (StringUtils.hasText(accountNumber)
                            && row.getString(0) != null
                            && row.getString(0).startsWith(accountNumber.split("\\.")[0])) {
                        return unitNo.trim();
                    }
                }
                // 任意一条带单位的 6001 贷方
                for (int i = 0; i < outer.size(); i++) {
                    Object el = outer.get(i);
                    if (!(el instanceof JSONArray)) {
                        continue;
                    }
                    JSONArray row = (JSONArray) el;
                    if (row.size() > 2 && StringUtils.hasText(row.getString(2))) {
                        return row.getString(2).trim();
                    }
                }
            } catch (Exception e) {
                log.debug("从历史凭证取单位失败 keys={}：{}", fieldKeys, e.getMessage());
            }
        }
        return null;
    }

    /** 按科目名称查找默认计量单位，并返回「科目编码|单位编码」时仅取单位。 */
    public String findUnitNumberFromAccountByName(String kingdeeUrl, String sessionId, String accountNameKeyword) {
        if (!StringUtils.hasText(accountNameKeyword)) {
            return null;
        }
        for (String fieldKeys : new String[]{
                "FNumber,FName,FUnitID.FNumber",
                "FNumber,FName,FUnitId.FNumber",
                "FNumber,FName,FMeasureUnitID.FNumber"
        }) {
            try {
                JSONObject data = new JSONObject();
                data.put("FormId", "BD_Account");
                data.put("FieldKeys", fieldKeys);
                data.put("FilterString", "FName like '%" + escapeFilterValue(accountNameKeyword.trim()) + "%'");
                data.put("Limit", 20);
                data.put("StartRow", 0);
                data.put("TopRowCount", 0);
                JSONObject requestBody = new JSONObject();
                requestBody.put("data", data);
                String responseBody = executePostRequest(
                        kingdeeUrl + EXECUTE_BILL_QUERY_PATH, sessionId, requestBody.toJSONString());
                if (isErrorResponse(responseBody)) {
                    continue;
                }
                JSONArray outer = JSON.parseArray(responseBody);
                if (outer == null || outer.isEmpty()) {
                    continue;
                }
                for (int i = 0; i < outer.size(); i++) {
                    Object el = outer.get(i);
                    if (!(el instanceof JSONArray)) {
                        continue;
                    }
                    JSONArray row = (JSONArray) el;
                    if (row.size() > 2 && StringUtils.hasText(row.getString(2))) {
                        log.info("科目名匹配 {} → 编码={} 单位={}", accountNameKeyword,
                                row.getString(0), row.getString(2));
                        return row.getString(2).trim();
                    }
                }
            } catch (Exception e) {
                log.debug("按科目名查单位失败 keyword={}：{}", accountNameKeyword, e.getMessage());
            }
        }
        return null;
    }

    /** 解析「开票收入」对应的真实科目编码（配置可能不准）。 */
    public String findIncomeAccountNumberByName(String kingdeeUrl, String sessionId,
                                                String preferredNumber, String nameKeyword) {
        if (StringUtils.hasText(preferredNumber)) {
            String exists = queryDimensionNumber(kingdeeUrl, sessionId, "BD_Account",
                    "FNumber,FName",
                    "FNumber = '" + escapeFilterValue(preferredNumber.trim()) + "'", false);
            if (StringUtils.hasText(exists)) {
                return preferredNumber.trim();
            }
        }
        String keyword = StringUtils.hasText(nameKeyword) ? nameKeyword.trim() : "开票收入";
        try {
            JSONObject data = new JSONObject();
            data.put("FormId", "BD_Account");
            data.put("FieldKeys", "FNumber,FName");
            data.put("FilterString", "FName like '%" + escapeFilterValue(keyword) + "%'");
            data.put("Limit", 20);
            data.put("StartRow", 0);
            data.put("TopRowCount", 0);
            JSONObject requestBody = new JSONObject();
            requestBody.put("data", data);
            String responseBody = executePostRequest(
                    kingdeeUrl + EXECUTE_BILL_QUERY_PATH, sessionId, requestBody.toJSONString());
            if (isErrorResponse(responseBody)) {
                return preferredNumber;
            }
            JSONArray outer = JSON.parseArray(responseBody);
            if (outer == null || outer.isEmpty()) {
                return preferredNumber;
            }
            for (int i = 0; i < outer.size(); i++) {
                Object el = outer.get(i);
                if (!(el instanceof JSONArray)) {
                    continue;
                }
                JSONArray row = (JSONArray) el;
                if (row.size() > 1 && row.getString(1) != null && row.getString(1).contains(keyword)
                        && StringUtils.hasText(row.getString(0))) {
                    return row.getString(0).trim();
                }
            }
            Object first = outer.get(0);
            if (first instanceof JSONArray) {
                JSONArray row = (JSONArray) first;
                if (StringUtils.hasText(row.getString(0))) {
                    return row.getString(0).trim();
                }
            }
        } catch (Exception e) {
            log.debug("按名称解析收入科目失败：{}", e.getMessage());
        }
        return preferredNumber;
    }

    /** 从科目档案读取默认计量单位编码。 */
    public String findUnitNumberFromAccount(String kingdeeUrl, String sessionId, String accountNumber) {
        if (!StringUtils.hasText(accountNumber)) {
            return null;
        }
        String acct = accountNumber.trim();
        for (String fieldKeys : new String[]{
                "FNumber,FName,FUnitID.FNumber",
                "FNumber,FName,FUnitId.FNumber",
                "FNumber,FName,FMeasureUnitID.FNumber"
        }) {
            try {
                JSONObject data = new JSONObject();
                data.put("FormId", "BD_Account");
                data.put("FieldKeys", fieldKeys);
                data.put("FilterString", "FNumber = '" + escapeFilterValue(acct) + "'");
                data.put("Limit", 5);
                data.put("StartRow", 0);
                data.put("TopRowCount", 0);
                JSONObject requestBody = new JSONObject();
                requestBody.put("data", data);
                String responseBody = executePostRequest(
                        kingdeeUrl + EXECUTE_BILL_QUERY_PATH, sessionId, requestBody.toJSONString());
                if (isErrorResponse(responseBody)) {
                    continue;
                }
                JSONArray outer = JSON.parseArray(responseBody);
                if (outer == null || outer.isEmpty()) {
                    continue;
                }
                Object first = outer.get(0);
                if (!(first instanceof JSONArray)) {
                    continue;
                }
                JSONArray row = (JSONArray) first;
                if (row.size() > 2 && row.getString(2) != null && !row.getString(2).trim().isEmpty()) {
                    return row.getString(2).trim();
                }
            } catch (Exception e) {
                log.debug("查询科目默认单位失败 account={} keys={}：{}", acct, fieldKeys, e.getMessage());
            }
        }
        return null;
    }

    /**
     * 从历史转字「开票收入」贷方分录学习部门核算维度（如 BM000001/广州黄埔）。
     * 本账套：FFLEX5=部门(BD_Department)。返回 [dimensionKey, dimensionNumber]。
     */
    public String[] learnIncomeDepartmentDimension(String kingdeeUrl, String sessionId,
                                                   String orgCompanyCode, String preferredDeptNumber) {
        String preferred = StringUtils.hasText(preferredDeptNumber) ? preferredDeptNumber.trim() : "BM000001";
        // 本账套已核实 FFLEX5=部门；其余作兼容探测
        int[] flexCandidates = {5, 9, 10, 11, 6, 12, 13, 15, 16, 7, 8};
        StringBuilder fieldKeys = new StringBuilder("FAccountID.FNumber,FCredit");
        for (int flex : flexCandidates) {
            fieldKeys.append(",FDETAILID.FFLEX").append(flex).append(".FNumber");
        }
        try {
            JSONObject data = new JSONObject();
            data.put("FormId", "GL_VOUCHER");
            data.put("FieldKeys", fieldKeys.toString());
            StringBuilder filter = new StringBuilder();
            filter.append("FAccountID.FNumber like '6001%' AND FCredit<>0");
            if (StringUtils.hasText(orgCompanyCode)) {
                filter.append(" AND FAccountBookID.FNumber = '")
                        .append(escapeFilterValue(orgCompanyCode.trim())).append("'");
            }
            data.put("FilterString", filter.toString());
            data.put("Limit", 80);
            data.put("StartRow", 0);
            data.put("TopRowCount", 0);
            JSONObject requestBody = new JSONObject();
            requestBody.put("data", data);
            String responseBody = executePostRequest(
                    kingdeeUrl + EXECUTE_BILL_QUERY_PATH, sessionId, requestBody.toJSONString());
            if (isErrorResponse(responseBody)) {
                log.warn("学习开票收入部门维度失败：{}", truncate(responseBody, 200));
                // 接口失败时仍返回本账套已核实的默认：FFLEX5 + 配置部门
                return new String[]{"FDETAILID__FFLEX5", preferred};
            }
            JSONArray outer = JSON.parseArray(responseBody);
            if (outer == null || outer.isEmpty()) {
                return new String[]{"FDETAILID__FFLEX5", preferred};
            }
            String fallbackKey = null;
            String fallbackNo = null;
            for (int i = 0; i < outer.size(); i++) {
                Object el = outer.get(i);
                if (!(el instanceof JSONArray)) {
                    continue;
                }
                JSONArray row = (JSONArray) el;
                for (int c = 0; c < flexCandidates.length; c++) {
                    int col = 2 + c;
                    if (row.size() <= col || !StringUtils.hasText(row.getString(col))) {
                        continue;
                    }
                    String no = row.getString(col).trim();
                    String key = "FDETAILID__FFLEX" + flexCandidates[c];
                    if (preferred.equalsIgnoreCase(no) || no.startsWith("BM")) {
                        if (preferred.equalsIgnoreCase(no)) {
                            log.info("历史开票收入部门维度：{} = {}（优先匹配）", key, no);
                            return new String[]{key, no};
                        }
                        if (fallbackKey == null) {
                            fallbackKey = key;
                            fallbackNo = no;
                        }
                    }
                }
            }
            if (fallbackKey != null) {
                log.info("历史开票收入部门维度：{} = {}（BM 首个）", fallbackKey, fallbackNo);
                return new String[]{fallbackKey, preferred};
            }
        } catch (Exception e) {
            log.warn("学习开票收入部门维度异常：{}", e.getMessage());
        }
        return new String[]{"FDETAILID__FFLEX5", preferred};
    }

    /**
     * 从历史转字应收分录学习：摘要中的客户简称 → 客户编码（本账套客户在 FFLEX6）。
     * 用于写入预检时提示「历史上类似购方用过哪个客户」。
     */
    public Map<String, String> learnCustomerHintsFromTransferHistory(String kingdeeUrl, String sessionId,
                                                                     String orgCompanyCode,
                                                                     Set<String> periods) {
        Map<String, String> hints = new LinkedHashMap<>();
        if (periods == null || periods.isEmpty()) {
            return hints;
        }
        String customerField = "FDETAILID.FFLEX6.FNumber";
        for (String period : periods) {
            try {
                JSONObject data = new JSONObject();
                data.put("FormId", "GL_VOUCHER");
                data.put("FieldKeys",
                        "FExplanation,FAccountID.FNumber," + customerField + ",FDebit,FCredit,FVOUCHERGROUPID.FName");
                String year = period.substring(0, 4);
                int periodNum = Integer.parseInt(period.substring(4).replaceAll("\\D", ""));
                StringBuilder filter = new StringBuilder();
                filter.append("FYear=").append(year).append(" AND FPeriod=").append(periodNum);
                filter.append(" AND FAccountID.FNumber like '1122%' AND FDebit<>0");
                if (StringUtils.hasText(orgCompanyCode)) {
                    filter.append(" AND FAccountBookID.FNumber = '")
                            .append(escapeFilterValue(orgCompanyCode.trim())).append("'");
                }
                data.put("FilterString", filter.toString());
                data.put("Limit", 500);
                data.put("StartRow", 0);
                data.put("TopRowCount", 0);
                JSONObject requestBody = new JSONObject();
                requestBody.put("data", data);
                String responseBody = executePostRequest(
                        kingdeeUrl + EXECUTE_BILL_QUERY_PATH, sessionId, requestBody.toJSONString());
                if (isErrorResponse(responseBody)) {
                    continue;
                }
                JSONArray outer = JSON.parseArray(responseBody);
                if (outer == null) {
                    continue;
                }
                for (int i = 0; i < outer.size(); i++) {
                    Object el = outer.get(i);
                    if (!(el instanceof JSONArray)) {
                        continue;
                    }
                    JSONArray row = (JSONArray) el;
                    if (row.size() < 3) {
                        continue;
                    }
                    String explanation = row.getString(0);
                    String customerNo = row.getString(2);
                    if (!StringUtils.hasText(customerNo) || !StringUtils.hasText(explanation)) {
                        continue;
                    }
                    customerNo = customerNo.trim();
                    // 摘要常见：销XXX开票 / 确认收入XXX
                    String hint = extractCustomerHintFromExplanation(explanation.trim());
                    if (StringUtils.hasText(hint) && !hints.containsKey(hint)) {
                        hints.put(hint, customerNo);
                    }
                }
            } catch (Exception e) {
                log.debug("学习历史客户线索失败 period={}：{}", period, e.getMessage());
            }
        }
        log.info("历史转字学习到客户线索 {} 条", hints.size());
        return hints;
    }

    private String extractCustomerHintFromExplanation(String explanation) {
        if (!StringUtils.hasText(explanation)) {
            return null;
        }
        String s = explanation.trim();
        // 销{客户}开票 / 销{客户}{货物}开票
        if (s.startsWith("销") && s.contains("开票")) {
            int end = s.indexOf("开票");
            String mid = s.substring(1, end);
            // 去掉货物关键词后缀
            for (String cut : new String[]{"机械租赁", "租赁费", "拖车费", "运输费", "等"}) {
                int idx = mid.indexOf(cut);
                if (idx > 2) {
                    mid = mid.substring(0, idx);
                    break;
                }
            }
            mid = mid.trim();
            if (mid.length() >= 4) {
                return mid.length() > 20 ? mid.substring(0, 20) : mid;
            }
        }
        return null;
    }

    public String findCustomerNumberByName(String kingdeeUrl, String sessionId, String customerName) {
        return findCustomerNumberByName(kingdeeUrl, sessionId, customerName, null, null);
    }

    public String findCustomerNumberByName(String kingdeeUrl, String sessionId,
                                           String customerName, String useOrgCode) {
        return findCustomerNumberByName(kingdeeUrl, sessionId, customerName, useOrgCode, null);
    }

    /**
     * 按客户名称查编码。
     * 组织校验同时支持：使用组织编码（FNumber）+ 使用组织名称（如「广东汾源酒业有限公司」）。
     * 注意：账簿编码不一定等于客户「使用组织」的 FNumber，不能只靠编码过滤。
     * 扫码收款等清算客户常为租户共享维度（界面可选但使用组织可能挂在其他公司），精确全称命中即接受。
     */
    public String findCustomerNumberByName(String kingdeeUrl, String sessionId,
                                           String customerName, String useOrgCode,
                                           String useOrgNameHint) {
        if (customerName == null || customerName.trim().isEmpty()) {
            return null;
        }
        String trimmed = customerName.trim();
        // 1) 按组织编码过滤
        CustomerHit hit = findCustomerHitInternal(kingdeeUrl, sessionId, trimmed, useOrgCode, null);
        if (hit != null) {
            return hit.number;
        }
        // 2) 按组织名称过滤（与金蝶客户列表「使用组织」列一致）
        if (StringUtils.hasText(useOrgNameHint)) {
            hit = findCustomerHitInternal(kingdeeUrl, sessionId, trimmed, null, useOrgNameHint);
            if (hit != null) {
                return hit.number;
            }
        }
        // 3) 不限组织查出后，用编码或名称判断是否属于当前账套组织
        CustomerHit global = findCustomerHitInternal(kingdeeUrl, sessionId, trimmed, null, null);
        if (global == null) {
            return null;
        }
        if (customerOrgCompatible(global, useOrgCode, useOrgNameHint)) {
            log.info("客户「{}」→ {}（组织编码={} 组织名={}，按名称/编码兼容校验通过）",
                    trimmed, global.number, global.useOrg, global.useOrgName);
            return global.number;
        }
        // 精确全称命中时：账簿组织名与客户使用组织名一致即可
        if (global.exactName && orgNameCompatible(global.useOrgName, useOrgNameHint)) {
            log.warn("客户「{}」精确命中 {}，使用组织名「{}」与账套组织「{}」匹配（组织编码配置={}，客户组织编码={}）",
                    trimmed, global.number, global.useOrgName, useOrgNameHint, useOrgCode, global.useOrg);
            return global.number;
        }
        // 扫码/微信等清算客户：租户内共享维度，凭证界面可选，不因使用组织挂在其他公司而拒绝
        if (global.exactName && isSharedQrCustomerName(trimmed)) {
            log.warn("客户「{}」精确命中共享扫码客户 {}（使用组织={}），当前账套组织={}，接受",
                    trimmed, global.number, defaultText(global.useOrgName, global.useOrg),
                    defaultText(useOrgNameHint, useOrgCode));
            return global.number;
        }
        // 精确全称 + 同品牌组织（汾源/共成等）兜底
        if (global.exactName && orgBrandCompatible(global.useOrgName, useOrgNameHint)) {
            log.warn("客户「{}」精确命中 {}，使用组织「{}」与账套「{}」同品牌，接受（账簿组织编码配置={}）",
                    trimmed, global.number, global.useOrgName, useOrgNameHint, useOrgCode);
            return global.number;
        }
        log.warn("客户「{}」匹配到 {}（组织编码={} 组织名={}）但与目标组织编码={} 名称={} 不兼容，放弃",
                trimmed, global.number, global.useOrg, global.useOrgName, useOrgCode, useOrgNameHint);
        return null;
    }

    /**
     * 客户未匹配时的可读说明：区分「完全没有」与「有档案但组织不兼容」。
     */
    public String describeMissingCustomer(String kingdeeUrl, String sessionId,
                                          String customerName, String useOrgCode) {
        return describeMissingCustomer(kingdeeUrl, sessionId, customerName, useOrgCode, null);
    }

    public String describeMissingCustomer(String kingdeeUrl, String sessionId,
                                          String customerName, String useOrgCode,
                                          String useOrgNameHint) {
        String name = customerName == null ? "" : customerName.trim();
        if (!StringUtils.hasText(name)) {
            return "购方名称为空，无法匹配金蝶客户";
        }
        CustomerHit global = findCustomerHitInternal(kingdeeUrl, sessionId, name, null, null);
        if (global == null) {
            StringBuilder msg = new StringBuilder();
            msg.append("未在金蝶客户档案找到「").append(name).append("」");
            if (StringUtils.hasText(useOrgNameHint)) {
                msg.append("（目标组织 ").append(useOrgNameHint.trim()).append("）");
            }
            // 购方带分公司/支公司时：若仅有总公司档案，明确提示不可顶替
            if (hasRegionalBranchSuffix(name)) {
                String parent = stripRegionalBranchSuffix(name);
                if (StringUtils.hasText(parent) && !parent.equals(name)) {
                    CustomerHit parentHit = pickBestCustomerHit(kingdeeUrl, sessionId, parent,
                            "FName = '" + escapeFilterValue(parent) + "' AND FForbidStatus = 'A'");
                    if (parentHit != null) {
                        msg.append("。注意：金蝶仅有总公司「")
                                .append(defaultText(parentHit.name, parent))
                                .append("」编码 ").append(parentHit.number)
                                .append("，不能代替购方全称「").append(name).append("」");
                    }
                }
            }
            msg.append("。请在金蝶按购方全称新建客户并分配到当前账套组织后重试");
            return msg.toString();
        }
        if (customerOrgCompatible(global, useOrgCode, useOrgNameHint)
                || (global.exactName && orgNameCompatible(global.useOrgName, useOrgNameHint))
                || (global.exactName && isSharedQrCustomerName(name))) {
            // 理论上可匹配；仍走到这里说明上层逻辑有问题
            return "客户「" + defaultText(global.name, name) + "」编码 " + global.number
                    + " 已找到（使用组织 " + defaultText(global.useOrgName, global.useOrg) + "），"
                    + "但写入校验未通过，请重试或联系管理员";
        }
        return "金蝶已有客户「" + defaultText(global.name, name) + "」编码 " + global.number
                + "（使用组织 " + defaultText(global.useOrgName, defaultText(global.useOrg, "未知")) + "），"
                + "与当前账套组织"
                + (StringUtils.hasText(useOrgNameHint) ? "「" + useOrgNameHint + "」" : "")
                + (StringUtils.hasText(useOrgCode) ? "/编码 " + useOrgCode : "")
                + "不匹配。请确认客户列表筛选组织是否为汾源，或将该客户分配到汾源组织";
    }

    private boolean customerOrgCompatible(CustomerHit hit, String useOrgCode, String useOrgNameHint) {
        if (hit == null) {
            return false;
        }
        if (StringUtils.hasText(useOrgCode) && useOrgCode.trim().equals(defaultText(hit.useOrg, ""))) {
            return true;
        }
        return orgNameCompatible(hit.useOrgName, useOrgNameHint);
    }

    private boolean orgNameCompatible(String customerOrgName, String useOrgNameHint) {
        if (!StringUtils.hasText(customerOrgName) || !StringUtils.hasText(useOrgNameHint)) {
            return false;
        }
        String a = customerOrgName.trim();
        String b = useOrgNameHint.trim();
        if (a.equals(b) || a.contains(b) || b.contains(a)) {
            return true;
        }
        return orgBrandCompatible(a, b);
    }

    /** 同租户品牌组织别名：汾源 / 共成等 */
    private boolean orgBrandCompatible(String customerOrgName, String useOrgNameHint) {
        if (!StringUtils.hasText(customerOrgName)) {
            return false;
        }
        String a = customerOrgName.trim();
        String b = useOrgNameHint == null ? "" : useOrgNameHint.trim();
        if (a.contains("汾源") && (b.isEmpty() || b.contains("汾源"))) {
            return true;
        }
        if (a.contains("共成") && (b.isEmpty() || b.contains("共成"))) {
            return true;
        }
        if (a.contains("共承兴") && (b.isEmpty() || b.contains("共承兴"))) {
            return true;
        }
        return false;
    }

    /** 扫码清算类客户：凭证辅助核算常用，可跨使用组织选用 */
    private boolean isSharedQrCustomerName(String name) {
        if (!StringUtils.hasText(name)) {
            return false;
        }
        String n = name.trim();
        return "扫码收款".equals(n)
                || "银联扫码".equals(n)
                || "二维码收款".equals(n)
                || "微信收款".equals(n)
                || "商户清算".equals(n)
                || n.contains("扫码收款");
    }

    private static String defaultText(String v, String dft) {
        return StringUtils.hasText(v) ? v.trim() : dft;
    }

    private static final class CustomerHit {
        private final String number;
        private final String name;
        private final String useOrg;
        private final String useOrgName;
        private final boolean exactName;

        private CustomerHit(String number, String name, String useOrg, String useOrgName, boolean exactName) {
            this.number = number;
            this.name = name;
            this.useOrg = useOrg;
            this.useOrgName = useOrgName;
            this.exactName = exactName;
        }
    }

    private CustomerHit findCustomerHitInternal(String kingdeeUrl, String sessionId,
                                                String trimmed, String useOrgCode,
                                                String useOrgNameHint) {
        StringBuilder orgFilter = new StringBuilder();
        if (StringUtils.hasText(useOrgCode)) {
            orgFilter.append(" AND FUseOrgId.FNumber = '")
                    .append(escapeFilterValue(useOrgCode.trim())).append("'");
        }
        if (StringUtils.hasText(useOrgNameHint)) {
            // 组织名称用核心词，避免全称/简称差异
            String nameKey = useOrgNameHint.trim();
            if (nameKey.length() > 10) {
                nameKey = nameKey.substring(0, 10);
            }
            orgFilter.append(" AND FUseOrgId.FName like '%")
                    .append(escapeFilterValue(nameKey)).append("%'");
        }
        String statusFilter = " AND FForbidStatus = 'A'";

        // 1) 全称精确
        CustomerHit hit = pickBestCustomerHit(kingdeeUrl, sessionId, trimmed,
                "FName = '" + escapeFilterValue(trimmed) + "'" + orgFilter + statusFilter);
        if (hit != null) {
            return hit;
        }
        // 2) 全称包含（命中名须含完整检索名，不会回落到更短的总公司）
        hit = pickBestCustomerHit(kingdeeUrl, sessionId, trimmed,
                "FName like '%" + escapeFilterValue(trimmed) + "%'" + orgFilter + statusFilter);
        if (hit != null) {
            return hit;
        }
        // 3) 简称候选：购方已带分公司/支公司时禁止剥成总公司再匹配（业务要求按分支全称建档）
        if (hasRegionalBranchSuffix(trimmed)) {
            return null;
        }
        for (String candidate : customerNameCandidates(trimmed)) {
            if (candidate.equals(trimmed)) {
                continue;
            }
            hit = pickBestCustomerHit(kingdeeUrl, sessionId, candidate,
                    "FName like '%" + escapeFilterValue(candidate) + "%'" + orgFilter + statusFilter);
            if (hit != null) {
                return hit;
            }
            hit = pickBestCustomerHit(kingdeeUrl, sessionId, candidate,
                    "FShortName like '%" + escapeFilterValue(candidate) + "%'" + orgFilter + statusFilter);
            if (hit != null) {
                return hit;
            }
        }
        return null;
    }

    /** 在结果集中选名称最贴近的客户，避免 like 命中第一条错误客户。 */
    private CustomerHit pickBestCustomerHit(String kingdeeUrl, String sessionId,
                                            String searchName, String filterString) {
        try {
            JSONObject data = new JSONObject();
            data.put("FormId", "BD_Customer");
            data.put("FieldKeys",
                    "FNumber,FName,FShortName,FForbidStatus,FUseOrgId.FNumber,FUseOrgId.FName,FDocumentStatus");
            data.put("FilterString", filterString);
            data.put("Limit", 50);
            data.put("StartRow", 0);
            data.put("TopRowCount", 0);
            JSONObject requestBody = new JSONObject();
            requestBody.put("data", data);
            String responseBody = executePostRequest(
                    kingdeeUrl + EXECUTE_BILL_QUERY_PATH, sessionId, requestBody.toJSONString());
            if (isErrorResponse(responseBody)) {
                return null;
            }
            JSONArray outer = JSON.parseArray(responseBody);
            if (outer == null || outer.isEmpty()) {
                return null;
            }
            CustomerHit best = null;
            int bestScore = -1;
            String needle = searchName == null ? "" : searchName.trim();
            for (int i = 0; i < outer.size(); i++) {
                Object el = outer.get(i);
                if (!(el instanceof JSONArray)) {
                    continue;
                }
                JSONArray row = (JSONArray) el;
                if (row.isEmpty() || !StringUtils.hasText(row.getString(0))) {
                    continue;
                }
                String number = row.getString(0).trim();
                String name = row.size() > 1 && row.getString(1) != null ? row.getString(1).trim() : "";
                String shortName = row.size() > 2 && row.getString(2) != null ? row.getString(2).trim() : "";
                String forbid = row.size() > 3 && row.getString(3) != null ? row.getString(3).trim() : "A";
                String useOrg = row.size() > 4 && row.getString(4) != null ? row.getString(4).trim() : "";
                String useOrgName = row.size() > 5 && row.getString(5) != null ? row.getString(5).trim() : "";
                if ("B".equalsIgnoreCase(forbid)) {
                    continue;
                }
                int score = scoreCustomerName(needle, name, shortName);
                if (score > bestScore) {
                    bestScore = score;
                    boolean exact = needle.equals(name) || needle.equals(shortName);
                    best = new CustomerHit(number, name, useOrg, useOrgName, exact);
                }
            }
            if (bestScore <= 0 || best == null) {
                return null;
            }
            log.info("客户匹配 search={} → {} / {} orgNo={} orgName={} exact={} (score={})",
                    needle, best.number, best.name, best.useOrg, best.useOrgName, best.exactName, bestScore);
            return best;
        } catch (Exception e) {
            log.debug("客户查询失败 search={}：{}", searchName, e.getMessage());
            return null;
        }
    }

    private int scoreCustomerName(String needle, String name, String shortName) {
        if (!StringUtils.hasText(needle)) {
            return 0;
        }
        if (needle.equals(name)) {
            return 1000;
        }
        if (needle.equals(shortName)) {
            return 900;
        }
        // 检索名带分公司/支公司：禁止用更短的总公司名得分（避免成都支公司→总公司 CUST）
        if (hasRegionalBranchSuffix(needle) && isParentCompanyOfBranch(needle, name)) {
            return 0;
        }
        if (hasRegionalBranchSuffix(needle) && StringUtils.hasText(shortName)
                && isParentCompanyOfBranch(needle, shortName)) {
            return 0;
        }
        if (StringUtils.hasText(name) && name.contains(needle)) {
            return 500 + Math.max(0, 100 - Math.abs(name.length() - needle.length()));
        }
        if (StringUtils.hasText(needle) && StringUtils.hasText(name) && needle.contains(name) && name.length() >= 4) {
            // 普通名称允许「检索含档案名」；分支机构场景已在上方拦截总公司
            return 400 + name.length();
        }
        if (StringUtils.hasText(shortName) && (shortName.contains(needle) || needle.contains(shortName))) {
            return 300;
        }
        // 核心字重合（至少 4 字）；分支机构不走此兜底，避免误命中总公司
        if (hasRegionalBranchSuffix(needle)) {
            return 0;
        }
        String core = needle.length() > 6 ? needle.substring(0, Math.min(8, needle.length())) : needle;
        if (StringUtils.hasText(name) && name.contains(core) && core.length() >= 4) {
            return 200;
        }
        return 0;
    }

    /** 是否含分公司 / 中心支公司 / 支公司 / 营业部等区域分支后缀。 */
    static boolean hasRegionalBranchSuffix(String name) {
        if (!StringUtils.hasText(name)) {
            return false;
        }
        String n = name.trim();
        return n.contains("中心支公司") || n.contains("分公司") || n.contains("支公司")
                || n.contains("营业部") || n.contains("分理处");
    }

    /** 去掉区域分支后缀，得到可能的总公司名。 */
    static String stripRegionalBranchSuffix(String name) {
        if (!StringUtils.hasText(name)) {
            return name;
        }
        String n = name.trim();
        // 先去更长后缀
        for (String suffix : new String[]{"中心支公司", "集团分公司", "分公司", "支公司", "营业部", "分理处"}) {
            int idx = n.lastIndexOf(suffix);
            if (idx > 0) {
                return n.substring(0, idx).trim();
            }
        }
        return n;
    }

    /** hit 是否为 search（含分支）对应的更短总公司名。 */
    static boolean isParentCompanyOfBranch(String branchName, String candidateName) {
        if (!StringUtils.hasText(branchName) || !StringUtils.hasText(candidateName)) {
            return false;
        }
        String branch = branchName.trim();
        String candidate = candidateName.trim();
        if (!hasRegionalBranchSuffix(branch) || hasRegionalBranchSuffix(candidate)) {
            return false;
        }
        if (!branch.contains(candidate) || candidate.length() < 4) {
            return false;
        }
        String parent = stripRegionalBranchSuffix(branch);
        return candidate.equals(parent) || parent.startsWith(candidate) || candidate.equals(
                parent.replace("股份有限公司", "").replace("有限责任公司", "").replace("有限公司", "").trim());
    }

    private boolean customerExistsInOrg(String kingdeeUrl, String sessionId,
                                        String customerNumber, String useOrgCode) {
        if (!StringUtils.hasText(customerNumber)) {
            return false;
        }
        String filter = "FNumber = '" + escapeFilterValue(customerNumber.trim()) + "' AND FForbidStatus = 'A'";
        if (StringUtils.hasText(useOrgCode)) {
            filter += " AND FUseOrgId.FNumber = '" + escapeFilterValue(useOrgCode.trim()) + "'";
        }
        String hit = queryDimensionNumber(kingdeeUrl, sessionId, "BD_Customer",
                "FNumber,FName", filter, false);
        return StringUtils.hasText(hit);
    }

    /**
     * 生成客户名匹配候选：仅去「股份有限公司/有限公司」等主体后缀。
     * 不剥「分公司/支公司」——带分支的名称由 {@link #findCustomerHitInternal} 要求全称命中。
     */
    private List<String> customerNameCandidates(String fullName) {
        List<String> out = new ArrayList<>();
        if (fullName == null || fullName.trim().isEmpty()) {
            return out;
        }
        String name = fullName.trim();
        out.add(name);
        if (hasRegionalBranchSuffix(name)) {
            return out;
        }
        String stripped = name
                .replace("股份有限公司", "")
                .replace("有限责任公司", "")
                .replace("有限公司", "")
                .trim();
        if (!stripped.isEmpty() && !stripped.equals(name)) {
            out.add(stripped);
        }
        // 再截到常见企业主体（保留前 8～12 个汉字量级的核心名）
        if (stripped.length() > 6) {
            String core = stripped.length() > 12 ? stripped.substring(0, 12) : stripped;
            if (!out.contains(core)) {
                out.add(core);
            }
            if (stripped.contains("平安")) {
                out.add("平安财产保险");
                out.add("中国平安");
            }
        }
        return out;
    }

    private String queryDimensionNumber(String kingdeeUrl, String sessionId, String formId,
                                        String fieldKeys, String filterString, boolean preferStaffNumber) {
        try {
            JSONObject data = new JSONObject();
            data.put("FormId", formId);
            data.put("FieldKeys", fieldKeys);
            data.put("FilterString", filterString);
            data.put("Limit", 20);
            data.put("StartRow", 0);
            data.put("TopRowCount", 0);
            JSONObject requestBody = new JSONObject();
            requestBody.put("data", data);
            String responseBody = executePostRequest(
                    kingdeeUrl + EXECUTE_BILL_QUERY_PATH, sessionId, requestBody.toJSONString());
            if (isErrorResponse(responseBody)) {
                return null;
            }
            JSONArray outerArray = JSON.parseArray(responseBody);
            if (outerArray == null || outerArray.isEmpty()) {
                return null;
            }
            Object first = outerArray.get(0);
            if (!(first instanceof JSONArray)) {
                return null;
            }
            JSONArray row = (JSONArray) first;
            if (preferStaffNumber && row.size() > 2 && row.getString(2) != null && !row.getString(2).isEmpty()) {
                return row.getString(2).trim();
            }
            return row.size() > 0 && row.getString(0) != null ? row.getString(0).trim() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isDataConflictError(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }
        String normalized = message.trim();
        return normalized.contains("冲突") || normalized.contains("已存在");
    }

    public com.fenyuan.liquor.modules.kingdee.vo.KingdeeVoucherHeaderVO resolveVoucherHeaderFromLocalOrKingdee(
            com.fenyuan.liquor.modules.kingdee.vo.KingdeeVoucherHeaderVO localHeader,
            String kingdeeUrl,
            String sessionId,
            String formId,
            String voucherDate,
            String voucherGroupName,
            String orgCompanyCode) {
        if (localHeader != null) {
            String voucherId = localHeader.getVoucherId();
            if (voucherId != null && !voucherId.trim().isEmpty()) {
                return localHeader;
            }
        }
        return findExistingVoucherHeader(kingdeeUrl, sessionId, formId, voucherDate, voucherGroupName, orgCompanyCode);
    }

    public String extractVoucherGroupNoFromConflict(String message) {
        if (message == null || message.trim().isEmpty()) {
            return null;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("银字(\\d+)号").matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public com.fenyuan.liquor.modules.kingdee.vo.KingdeeVoucherHeaderVO findVoucherHeaderForRecovery(String kingdeeUrl,
                                                                                           String sessionId,
                                                                                           String formId,
                                                                                           String voucherDate,
                                                                                           String voucherGroupName,
                                                                                           String orgCompanyCode,
                                                                                           String conflictMessage) {
        String voucherGroupNo = extractVoucherGroupNoFromConflict(conflictMessage);
        if (voucherGroupNo != null && !voucherGroupNo.isEmpty()) {
            com.fenyuan.liquor.modules.kingdee.vo.KingdeeVoucherHeaderVO exact = findExistingVoucherHeader(
                    kingdeeUrl, sessionId, formId, voucherDate, voucherGroupName, orgCompanyCode, voucherGroupNo);
            if (exact != null) {
                return exact;
            }
        }
        return findExistingVoucherHeader(kingdeeUrl, sessionId, formId, voucherDate, voucherGroupName, orgCompanyCode);
    }

    public com.fenyuan.liquor.modules.kingdee.vo.KingdeeVoucherHeaderVO findExistingVoucherHeader(String kingdeeUrl,
                                                                                        String sessionId,
                                                                                        String formId,
                                                                                        String voucherDate,
                                                                                        String voucherGroupName,
                                                                                        String orgCompanyCode) {
        return findExistingVoucherHeader(kingdeeUrl, sessionId, formId, voucherDate, voucherGroupName, orgCompanyCode, null);
    }

    public com.fenyuan.liquor.modules.kingdee.vo.KingdeeVoucherHeaderVO findExistingVoucherHeader(String kingdeeUrl,
                                                                                        String sessionId,
                                                                                        String formId,
                                                                                        String voucherDate,
                                                                                        String voucherGroupName,
                                                                                        String orgCompanyCode,
                                                                                        String voucherGroupNo) {
        if (voucherDate == null || voucherDate.isEmpty() || voucherGroupName == null || voucherGroupName.isEmpty()) {
            return null;
        }
        try {
            String orgCode = orgCompanyCode != null && !orgCompanyCode.isEmpty() ? orgCompanyCode : "106";
            String fieldKeys = "FVOUCHERID,FDate,FVOUCHERGROUPID.FName,FVOUCHERGROUPID.FNumber,FVOUCHERGROUPNO";
            StringBuilder filter = new StringBuilder();
            filter.append("FDate >= '").append(voucherDate).append(" 00:00:00'");
            filter.append(" AND FDate < '").append(voucherDate).append(" 23:59:59'");
            filter.append(" AND FVOUCHERGROUPID.FName = '").append(escapeFilterValue(voucherGroupName)).append("'");
            filter.append(" AND ").append(ORG_FIELD_KEY).append(" = '").append(escapeFilterValue(orgCode)).append("'");
            if (voucherGroupNo != null && !voucherGroupNo.isEmpty()) {
                filter.append(" AND FVOUCHERGROUPNO = '").append(escapeFilterValue(voucherGroupNo)).append("'");
            }

            JSONObject data = new JSONObject();
            data.put("FormId", formId);
            data.put("FieldKeys", fieldKeys + "," + ORG_FIELD_KEY);
            data.put("FilterString", filter.toString());
            data.put("Limit", 200);
            data.put("StartRow", 0);
            data.put("TopRowCount", 0);
            JSONObject requestBody = new JSONObject();
            requestBody.put("data", data);
            String responseBody = executePostRequest(
                    kingdeeUrl + EXECUTE_BILL_QUERY_PATH, sessionId, requestBody.toJSONString());
            if (isErrorResponse(responseBody)) {
                return null;
            }
            JSONArray outerArray = JSON.parseArray(responseBody);
            if (outerArray == null || outerArray.isEmpty()) {
                return null;
            }
            com.fenyuan.liquor.modules.kingdee.vo.KingdeeVoucherHeaderVO header = null;
            int minVoucherNo = Integer.MAX_VALUE;
            int targetVoucherNo = voucherGroupNo != null && !voucherGroupNo.isEmpty()
                    ? parseVoucherGroupNumber(voucherGroupNo) : -1;
            for (int i = 0; i < outerArray.size(); i++) {
                JSONArray row = outerArray.getJSONArray(i);
                if (row == null || row.size() < 5) {
                    continue;
                }
                String voucherId = row.getString(0);
                if (voucherId == null || voucherId.isEmpty()) {
                    continue;
                }
                int voucherNo = parseVoucherGroupNumber(row.getString(4));
                if (targetVoucherNo > 0) {
                    if (voucherNo != targetVoucherNo) {
                        continue;
                    }
                    header = new com.fenyuan.liquor.modules.kingdee.vo.KingdeeVoucherHeaderVO();
                    header.setVoucherId(voucherId);
                    header.setVoucherGroupName(row.getString(2));
                    header.setVoucherGroupNumber(row.getString(3));
                    header.setVoucherNumber(row.getString(4));
                    return header;
                }
                if (header == null || voucherNo < minVoucherNo) {
                    minVoucherNo = voucherNo;
                    header = new com.fenyuan.liquor.modules.kingdee.vo.KingdeeVoucherHeaderVO();
                    header.setVoucherId(voucherId);
                    header.setVoucherGroupName(row.getString(2));
                    header.setVoucherGroupNumber(row.getString(3));
                    header.setVoucherNumber(row.getString(4));
                }
            }
            return header;
        } catch (Exception e) {
            return null;
        }
    }


    public String findEmployeeNumberByName(String kingdeeUrl, String sessionId, String employeeName) {
        JSONObject employee = findEmployeeByName(kingdeeUrl, sessionId, employeeName);
        if (employee == null) {
            return null;
        }
        return resolveEmployeeDimensionNumber(employee);
    }

    public String resolveEmployeeDimensionNumber(JSONObject employee) {
        if (employee == null) {
            return null;
        }
        String staffNumber = employee.getString("FStaffNumber");
        if (staffNumber != null && !staffNumber.trim().isEmpty()) {
            return staffNumber.trim();
        }
        String number = employee.getString("FNumber");
        return number != null ? number.trim() : null;
    }

    public JSONObject findEmployeeByName(String kingdeeUrl, String sessionId, String employeeName) {
        if (employeeName == null || employeeName.trim().isEmpty()) {
            return null;
        }
        String trimmed = employeeName.trim();
        String[] formIds = {"BD_Empinfo", "BD_EMPINFO", "BD_NEWSTAFF"};
        for (String formId : formIds) {
            JSONObject employee = queryEmployeeRow(kingdeeUrl, sessionId, formId, trimmed,
                    "FName = '" + escapeFilterValue(trimmed) + "'");
            if (employee != null) {
                return employee;
            }
            employee = queryEmployeeRow(kingdeeUrl, sessionId, formId, trimmed,
                    "FStaffNumber = '" + escapeFilterValue(trimmed) + "'");
            if (employee != null) {
                return employee;
            }
        }
        return queryEmployeeRow(kingdeeUrl, sessionId, "BD_Empinfo", trimmed,
                "FName like '%" + escapeFilterValue(trimmed) + "%'");
    }

    private JSONObject queryEmployeeRow(String kingdeeUrl, String sessionId, String formId,
                                        String searchName, String filterString) {
        try {
            JSONObject data = new JSONObject();
            data.put("FormId", formId);
            data.put("FieldKeys", "FNumber,FName,FStaffNumber");
            data.put("FilterString", filterString);
            data.put("Limit", 20);
            data.put("StartRow", 0);
            data.put("TopRowCount", 0);
            JSONObject requestBody = new JSONObject();
            requestBody.put("data", data);
            String responseBody = executePostRequest(
                    kingdeeUrl + EXECUTE_BILL_QUERY_PATH, sessionId, requestBody.toJSONString());
            if (isErrorResponse(responseBody)) {
                return null;
            }
            JSONArray outerArray = JSON.parseArray(responseBody);
            if (outerArray == null || outerArray.isEmpty()) {
                return null;
            }
            List<JSONObject> employees = new ArrayList<>();
            for (int i = 0; i < outerArray.size(); i++) {
                Object element = outerArray.get(i);
                if (!(element instanceof JSONArray)) {
                    continue;
                }
                JSONArray row = (JSONArray) element;
                if (row.size() < 2) {
                    continue;
                }
                JSONObject employee = new JSONObject();
                employee.put("FNumber", row.getString(0));
                employee.put("FName", row.size() > 1 ? row.getString(1) : null);
                employee.put("FStaffNumber", row.size() > 2 ? row.getString(2) : null);
                employees.add(employee);
            }
            return pickBestEmployee(employees, searchName);
        } catch (Exception e) {
            return null;
        }
    }

    private JSONObject pickBestEmployee(List<JSONObject> employees, String searchName) {
        if (employees == null || employees.isEmpty()) {
            return null;
        }
        if (employees.size() == 1) {
            return employees.get(0);
        }
        String trimmed = searchName != null ? searchName.trim() : "";
        List<JSONObject> exactNameMatches = new ArrayList<>();
        for (JSONObject employee : employees) {
            String name = employee.getString("FName");
            if (trimmed.equals(safeTrim(name))) {
                exactNameMatches.add(employee);
            }
        }
        if (!exactNameMatches.isEmpty()) {
            return pickBestEmployeeCode(exactNameMatches);
        }
        for (JSONObject employee : employees) {
            String name = employee.getString("FName");
            if (name != null && name.contains(trimmed)) {
                exactNameMatches.add(employee);
            }
        }
        if (!exactNameMatches.isEmpty()) {
            return pickBestEmployeeCode(exactNameMatches);
        }
        return pickBestEmployeeCode(employees);
    }

    private JSONObject pickBestEmployeeCode(List<JSONObject> employees) {
        for (JSONObject employee : employees) {
            String number = employee.getString("FNumber");
            String staffNumber = employee.getString("FStaffNumber");
            if (hasLetterCode(staffNumber) && staffNumber.equals(number)) {
                return employee;
            }
        }
        for (JSONObject employee : employees) {
            String staffNumber = employee.getString("FStaffNumber");
            if (hasLetterCode(staffNumber)) {
                return employee;
            }
        }
        for (JSONObject employee : employees) {
            String number = employee.getString("FNumber");
            if (hasLetterCode(number)) {
                return employee;
            }
        }
        return employees.get(0);
    }

    private boolean hasLetterCode(String code) {
        return code != null && code.matches(".*[A-Za-z].*");
    }

    private String safeTrim(String value) {
        return value != null ? value.trim() : "";
    }

    public String findBankNumberByName(String kingdeeUrl, String sessionId, String bankName) {
        if (bankName == null || bankName.trim().isEmpty()) {
            return null;
        }
        for (String candidate : buildBankNameCandidates(bankName)) {
            String number = findBaseDataNumberByName(kingdeeUrl, sessionId, "BD_BANK", candidate);
            if (number != null && !number.trim().isEmpty()) {
                return number.trim();
            }
        }
        return null;
    }

    private List<String> buildBankNameCandidates(String bankName) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        String trimmed = bankName.trim();
        candidates.add(trimmed);
        int bankIdx = trimmed.indexOf("银行");
        if (bankIdx > 0) {
            candidates.add(trimmed.substring(0, bankIdx + 2));
        }
        String[] suffixes = {"支行", "分行", "营业部", "分理处", "储蓄所"};
        for (String suffix : suffixes) {
            int idx = trimmed.indexOf(suffix);
            if (idx > 2) {
                candidates.add(trimmed.substring(0, idx + suffix.length()));
                candidates.add(trimmed.substring(0, idx));
            }
        }
        String withoutCompany = trimmed
                .replace("股份有限公司", "")
                .replace("有限责任公司", "")
                .trim();
        if (!withoutCompany.isEmpty()) {
            candidates.add(withoutCompany);
        }
        return new ArrayList<>(candidates);
    }

    private String findBaseDataNumberByName(String kingdeeUrl, String sessionId, String formId, String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        String trimmed = name.trim();
        String exact = queryBaseDataNumberBestMatch(kingdeeUrl, sessionId, formId,
                "FName = '" + escapeFilterValue(trimmed) + "'", trimmed);
        if (exact != null) {
            return exact;
        }
        return queryBaseDataNumberBestMatch(kingdeeUrl, sessionId, formId,
                "FName like '%" + escapeFilterValue(trimmed) + "%'", trimmed);
    }

    private String queryBaseDataNumberBestMatch(String kingdeeUrl, String sessionId, String formId,
                                                String filterString, String searchName) {
        try {
            JSONObject data = new JSONObject();
            data.put("FormId", formId);
            data.put("FieldKeys", "FNumber,FName");
            data.put("FilterString", filterString);
            data.put("Limit", 20);
            data.put("StartRow", 0);
            data.put("TopRowCount", 0);
            JSONObject requestBody = new JSONObject();
            requestBody.put("data", data);
            String responseBody = executePostRequest(
                    kingdeeUrl + EXECUTE_BILL_QUERY_PATH, sessionId, requestBody.toJSONString());
            if (isErrorResponse(responseBody)) {
                return null;
            }
            JSONArray outerArray = JSON.parseArray(responseBody);
            if (outerArray == null || outerArray.isEmpty()) {
                return null;
            }
            String trimmed = searchName != null ? searchName.trim() : "";
            String exactMatch = null;
            String containsMatch = null;
            for (int i = 0; i < outerArray.size(); i++) {
                Object element = outerArray.get(i);
                if (!(element instanceof JSONArray)) {
                    continue;
                }
                JSONArray row = (JSONArray) element;
                if (row.isEmpty()) {
                    continue;
                }
                String number = row.getString(0);
                String rowName = row.size() > 1 ? row.getString(1) : null;
                if (number == null) {
                    continue;
                }
                if (trimmed.equals(safeTrim(rowName))) {
                    return number.trim();
                }
                if (exactMatch == null && rowName != null && rowName.contains(trimmed)) {
                    exactMatch = number.trim();
                } else if (containsMatch == null && rowName != null && trimmed.contains(safeTrim(rowName))) {
                    containsMatch = number.trim();
                }
            }
            if (exactMatch != null) {
                return exactMatch;
            }
            if (containsMatch != null) {
                return containsMatch;
            }
            Object firstElement = outerArray.get(0);
            if (firstElement instanceof JSONArray) {
                JSONArray row = (JSONArray) firstElement;
                return row.size() > 0 ? row.getString(0) : null;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    private int parseVoucherGroupNumber(String voucherNumber) {
        if (voucherNumber == null || voucherNumber.trim().isEmpty()) {
            return Integer.MAX_VALUE;
        }
        try {
            return Integer.parseInt(voucherNumber.trim());
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    private String escapeFilterValue(String value) {
        return value == null ? "" : value.replace("'", "''");
    }
    private String truncate(String s, int maxLen) {
        if (s == null) {
            return null;
        }
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}
