package com.fenyuan.liquor.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenyuan.liquor.modules.system.menu.entity.SysMenu;
import com.fenyuan.liquor.modules.system.menu.mapper.SysMenuMapper;
import com.fenyuan.liquor.modules.system.role.entity.SysRole;
import com.fenyuan.liquor.modules.system.role.mapper.SysRoleMapper;
import com.fenyuan.liquor.modules.system.user.entity.SysUser;
import com.fenyuan.liquor.modules.system.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysMenuMapper sysMenuMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setPassword(user.getPassword());
        loginUser.setStatus(user.getStatus());
        loginUser.setIsAdmin(user.getIsAdmin());

        List<SysRole> roles = sysRoleMapper.selectRolesByUserId(user.getId());
        Set<String> roleCodes = roles.stream().map(SysRole::getCode).collect(Collectors.toSet());
        loginUser.setRoles(roleCodes);

        Set<String> permissions = new HashSet<>();
        if (user.getIsAdmin() != null && user.getIsAdmin() == 1) {
            List<SysMenu> menus = sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                    .eq(SysMenu::getStatus, 1)
                    .isNotNull(SysMenu::getPermission));
            permissions = menus.stream()
                    .map(SysMenu::getPermission)
                    .filter(p -> p != null && !p.isEmpty())
                    .collect(Collectors.toSet());
            permissions.add("*:*:*");
        } else {
            List<String> perms = sysMenuMapper.selectPermsByUserId(user.getId());
            if (perms != null) {
                permissions = perms.stream().filter(p -> p != null && !p.isEmpty()).collect(Collectors.toSet());
            }
        }
        loginUser.setPermissions(permissions);
        return loginUser;
    }
}
