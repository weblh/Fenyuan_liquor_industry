package com.fenyuan.liquor.common.hub;

import com.fenyuan.liquor.common.config.HubDashboardProperties;
import com.fenyuan.liquor.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class FengchiIdentityClient {

    private final HubDashboardProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public void assertHubAllowed(String fengchiToken) {
        if (!properties.isEnabled()) {
            throw new BusinessException(403, "总大屏令牌换发未启用");
        }
        if (!StringUtils.hasText(fengchiToken)) {
            throw new BusinessException(401, "丰驰令牌不能为空");
        }

        String url = properties.getFengchiValidateUrl();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + fengchiToken.trim());
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new BusinessException(401, "丰驰令牌校验失败");
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            int code = root.path("code").asInt(0);
            if (code != 200) {
                throw new BusinessException(401, root.path("message").asText("丰驰令牌无效"));
            }

            JsonNode data = root.path("data");
            String email = text(data, "email");
            String phone = text(data, "phone");
            String username = text(data, "username");
            if (!isAllowed(email, phone, username)) {
                log.warn("总大屏换发拒绝非白名单账号: email={}, phone={}, username={}", email, phone, username);
                throw new BusinessException(403, "该账号无权换发汾源大屏令牌");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("校验丰驰令牌失败: url={}, err={}", url, e.getMessage());
            throw new BusinessException(502, "无法校验丰驰登录状态，请确认丰驰服务可用");
        }
    }

    private boolean isAllowed(String email, String phone, String username) {
        String emailNorm = email == null ? "" : email.trim().toLowerCase();
        String phoneNorm = phone == null ? "" : phone.trim();
        String userNorm = username == null ? "" : username.trim().toLowerCase();

        for (String allowed : properties.getAllowedEmails()) {
            if (StringUtils.hasText(allowed) && allowed.trim().equalsIgnoreCase(emailNorm)) {
                return true;
            }
            if (StringUtils.hasText(allowed) && allowed.trim().equalsIgnoreCase(userNorm)) {
                return true;
            }
        }
        for (String allowed : properties.getAllowedPhones()) {
            if (StringUtils.hasText(allowed) && allowed.trim().equals(phoneNorm)) {
                return true;
            }
            if (StringUtils.hasText(allowed) && allowed.trim().equals(userNorm)) {
                return true;
            }
        }
        return false;
    }

    private static String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText("");
    }
}
