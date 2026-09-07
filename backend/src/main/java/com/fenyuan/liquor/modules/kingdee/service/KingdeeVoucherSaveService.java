package com.fenyuan.liquor.modules.kingdee.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fenyuan.liquor.modules.kingdee.vo.KingdeeVoucherSaveResultVO;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
public class KingdeeVoucherSaveService {

    private static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final String SESSION_HEADER = "kdservice-sessionid";
    private static final String SAVE_PATH =
            "Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.Save.common.kdsvc";
    private static final String VIEW_PATH =
            "Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.View.common.kdsvc";

    private final OkHttpClient client;

    public KingdeeVoucherSaveService() {
        this.client = KingdeeHttpSupport.newClient(30, 60, 60);
    }

    public JSONObject view(String kingdeeUrl, String sessionId, String formId, String voucherId) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("formid", formId);
            JSONObject data = new JSONObject();
            data.put("CreateOrgId", 0);
            data.put("Number", "");
            data.put("Id", voucherId);
            requestBody.put("data", data);

            String responseBody = executePostRequest(kingdeeUrl + VIEW_PATH, sessionId, requestBody.toJSONString());
            JSONObject result = extractResultObject(responseBody);
            if (result == null) {
                throw new RuntimeException("\u67e5\u770b\u91d1\u8776\u51ed\u8bc1\u5931\u8d25\uff1a" + responseBody);
            }
            JSONObject status = result.getJSONObject("ResponseStatus");
            if (status != null && !status.getBooleanValue("IsSuccess")) {
                throw new RuntimeException("\u67e5\u770b\u91d1\u8776\u51ed\u8bc1\u5931\u8d25\uff1a" + extractErrorMessage(status));
            }
            JSONObject model = result.getJSONObject("Result");
            if (model == null) {
                model = result;
            }
            return model;
        } catch (IOException e) {
            throw new RuntimeException("\u67e5\u770b\u91d1\u8776\u51ed\u8bc1\u5931\u8d25\uff1a" + e.getMessage(), e);
        }
    }

    public KingdeeVoucherSaveResultVO save(String kingdeeUrl, String sessionId, String formId,
                                           JSONObject model, String explanation, String voucherGroup) {
        return save(kingdeeUrl, sessionId, formId, model, explanation, voucherGroup, true);
    }

    public KingdeeVoucherSaveResultVO save(String kingdeeUrl, String sessionId, String formId,
                                           JSONObject model, String explanation, String voucherGroup,
                                           boolean deleteMissingEntries) {
        return save(kingdeeUrl, sessionId, formId, model, explanation, voucherGroup,
                deleteMissingEntries, true, false);
    }

    /**
     * @param entryBatchFill 为 true 时金蝶可能用首行分录批量填充后续行，导致收入行单位被清空
     * @param verifyBaseData 为 true 时校验基础资料编码是否存在（单位编码错误会明确报错）
     */
    public KingdeeVoucherSaveResultVO save(String kingdeeUrl, String sessionId, String formId,
                                           JSONObject model, String explanation, String voucherGroup,
                                           boolean deleteMissingEntries,
                                           boolean entryBatchFill,
                                           boolean verifyBaseData) {
        try {
            JSONObject data = new JSONObject();
            data.put("ValidateFlag", true);
            data.put("NumberSearch", true);
            data.put("IsVerifyBaseDataField", verifyBaseData);
            data.put("IsEntryBatchFill", entryBatchFill);
            data.put("IsDeleteEntry", deleteMissingEntries);
            data.put("NeedReturnFields", new String[]{"FVOUCHERID", "FVOUCHERGROUPNO", "FVOUCHERGROUPID.FName"});
            data.put("Model", model);

            JSONObject requestBody = new JSONObject();
            requestBody.put("formid", formId);
            requestBody.put("data", data);

            String responseBody = executePostRequest(
                    kingdeeUrl + SAVE_PATH, sessionId, requestBody.toJSONString());
            KingdeeVoucherSaveResultVO result = parseSaveResponse(responseBody, explanation, voucherGroup);
            return finalizeVoucherNumber(result, kingdeeUrl, sessionId, formId);
        } catch (IOException e) {
            throw new RuntimeException("\u7f51\u7edc\u8bf7\u6c42\u5931\u8d25\uff1a" + e.getMessage(), e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("\u4fdd\u5b58\u51ed\u8bc1\u5931\u8d25\uff1a" + e.getMessage(), e);
        }
    }

    private KingdeeVoucherSaveResultVO parseSaveResponse(String responseBody, String explanation,
                                                         String voucherGroup) {
        JSONObject result = extractResultObject(responseBody);
        if (result == null) {
            throw new RuntimeException("\u91d1\u8776\u63a5\u53e3\u8fd4\u56de\u683c\u5f0f\u5f02\u5e38\uff1a" + responseBody);
        }

        JSONObject status = result.getJSONObject("ResponseStatus");
        if (status != null && !status.getBooleanValue("IsSuccess")) {
            throw new RuntimeException("\u91d1\u8776\u63a5\u53e3\u8fd4\u56de\u9519\u8bef\uff1a" + extractErrorMessage(status));
        }

        KingdeeVoucherSaveResultVO vo = new KingdeeVoucherSaveResultVO();
        vo.setExplanation(explanation);
        vo.setVoucherGroup(voucherGroup);

        String voucherId = result.getString("Id");
        if (voucherId == null || voucherId.isEmpty()) {
            voucherId = result.getString("FVOUCHERID");
        }
        if ((voucherId == null || voucherId.isEmpty()) && status != null) {
            JSONArray successEntities = status.getJSONArray("SuccessEntitys");
            if (successEntities != null && !successEntities.isEmpty()) {
                voucherId = successEntities.getJSONObject(0).getString("Id");
            }
        }
        vo.setVoucherId(voucherId);

        String voucherNumber = null;
        if (result.containsKey("NeedReturnData")) {
            JSONArray needReturnData = result.getJSONArray("NeedReturnData");
            if (needReturnData != null && !needReturnData.isEmpty()) {
                JSONObject first = needReturnData.getJSONObject(0);
                if (first != null) {
                    if (voucherId == null || voucherId.isEmpty()) {
                        vo.setVoucherId(first.getString("FVOUCHERID"));
                        voucherId = vo.getVoucherId();
                    }
                    voucherNumber = first.getString("FVOUCHERGROUPNO");
                }
            }
        }
        if (!isLikelyVoucherGroupNo(voucherNumber)) {
            voucherNumber = result.getString("FVOUCHERGROUPNO");
        }
        if (!isLikelyVoucherGroupNo(voucherNumber) && status != null) {
            JSONArray successEntities = status.getJSONArray("SuccessEntitys");
            if (successEntities != null && !successEntities.isEmpty()) {
                String successNumber = successEntities.getJSONObject(0).getString("Number");
                if (isLikelyVoucherGroupNo(successNumber)) {
                    voucherNumber = successNumber;
                }
            }
        }
        if (!isLikelyVoucherGroupNo(voucherNumber)) {
            String number = result.getString("Number");
            if (isLikelyVoucherGroupNo(number)) {
                voucherNumber = number;
            }
        }
        vo.setVoucherNumber(voucherNumber);
        return vo;
    }

    private KingdeeVoucherSaveResultVO finalizeVoucherNumber(KingdeeVoucherSaveResultVO result,
                                                             String kingdeeUrl,
                                                             String sessionId,
                                                             String formId) {
        if (result == null || result.getVoucherId() == null || result.getVoucherId().isEmpty()) {
            return result;
        }
        String resolved = resolveVoucherGroupNoFromView(kingdeeUrl, sessionId, formId, result.getVoucherId());
        if (resolved != null && !resolved.isEmpty()) {
            result.setVoucherNumber(resolved);
        }
        return result;
    }

    private String resolveVoucherGroupNoFromView(String kingdeeUrl, String sessionId, String formId, String voucherId) {
        try {
            JSONObject model = view(kingdeeUrl, sessionId, formId, voucherId);
            String groupNo = model.getString("FVOUCHERGROUPNO");
            if (isLikelyVoucherGroupNo(groupNo)) {
                return groupNo.trim();
            }
        } catch (Exception ignored) {
            // fall through
        }
        return null;
    }

    private boolean isLikelyVoucherGroupNo(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        String trimmed = value.trim();
        return !trimmed.contains("/") && trimmed.matches("\\d+");
    }

    private JSONObject extractResultObject(String responseBody) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            return null;
        }
        String trimmed = responseBody.trim();
        if (trimmed.startsWith("[")) {
            JSONArray outer = JSON.parseArray(trimmed);
            if (outer == null || outer.isEmpty()) {
                return null;
            }
            Object first = outer.get(0);
            if (first instanceof JSONArray && !((JSONArray) first).isEmpty()) {
                return ((JSONArray) first).getJSONObject(0);
            }
            if (first instanceof JSONObject) {
                JSONObject obj = (JSONObject) first;
                return obj.getJSONObject("Result") != null ? obj.getJSONObject("Result") : obj;
            }
            return null;
        }
        JSONObject obj = JSON.parseObject(trimmed);
        return obj.getJSONObject("Result") != null ? obj.getJSONObject("Result") : obj;
    }

    private String extractErrorMessage(JSONObject status) {
        String msg = status.getString("Msg");
        if (msg != null && !msg.isEmpty()) {
            return msg;
        }
        JSONArray errors = status.getJSONArray("Errors");
        if (errors != null && !errors.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < errors.size(); i++) {
                JSONObject err = errors.getJSONObject(i);
                if (err == null) {
                    continue;
                }
                String message = err.getString("Message");
                if (message == null || message.isEmpty()) {
                    continue;
                }
                if (sb.length() > 0) {
                    sb.append("；");
                }
                sb.append(message.trim());
            }
            if (sb.length() > 0) {
                return sb.toString();
            }
        }
        return status.toJSONString();
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
                throw new IOException("\u8bf7\u6c42\u5931\u8d25\uff0c\u54cd\u5e94\u7801\uff1a" + response.code());
            }
            return response.body().string();
        }
    }
}