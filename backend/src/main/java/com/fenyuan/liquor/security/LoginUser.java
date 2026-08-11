package com.fenyuan.liquor.security;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class LoginUser implements UserDetails {

    private Long userId;
    private String username;
    private String password;
    private Integer status;
    private Integer isAdmin;
    private Set<String> permissions;
    private Set<String> roles;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (permissions == null) {
            return java.util.Collections.emptyList();
        }
        return permissions.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toSet());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status == null || status == 1;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == null || status == 1;
    }

    public boolean isAdmin() {
        return isAdmin != null && isAdmin == 1;
    }
}
