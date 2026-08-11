package com.fenyuan.liquor.common.utils;

import com.fenyuan.liquor.security.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static LoginUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser)) {
            return null;
        }
        return (LoginUser) authentication.getPrincipal();
    }

    public static Long getUserId() {
        LoginUser loginUser = getLoginUser();
        return loginUser == null ? null : loginUser.getUserId();
    }

    public static String getUsername() {
        LoginUser loginUser = getLoginUser();
        return loginUser == null ? null : loginUser.getUsername();
    }
}
