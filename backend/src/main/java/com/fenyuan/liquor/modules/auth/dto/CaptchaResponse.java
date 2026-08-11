package com.fenyuan.liquor.modules.auth.dto;

import lombok.Data;

@Data
public class CaptchaResponse {
    private String captchaKey;
    private String captchaImage;
}
