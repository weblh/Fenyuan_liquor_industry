package com.fenyuan.liquor.modules.system.menu.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenyuan.liquor.common.exception.BusinessException;
import com.fenyuan.liquor.common.utils.SecurityUtils;
import com.fenyuan.liquor.modules.system.menu.dto.MenuDTO;
import com.fenyuan.liquor.modules.system.menu.entity.SysMenu;
import com.fenyuan.liquor.modules.system.menu.mapper.SysMenuMapper;
import com.fenyuan.liquor.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysMenuService {

    private final SysMenuMapper sysMenuMapper;

    public List<SysMenu> tree() {
        List<SysMenu> menus = sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getSort));
        return buildTree(menus, 0L);
    }

    public SysMenu getById(Long id) {
        SysMenu menu = sysMenuMapper.selectById(id);
        if (menu == null) {
            throw new BusinessException("菜单不存在");
        }
        return menu;
    }

    public void create(MenuDTO dto) {
        SysMenu menu = BeanUtil.copyProperties(dto, SysMenu.class);
        if (menu.getParentId() == null) {
            menu.setParentId(0L);
        }
        if (menu.getStatus() == null) {
            menu.setStatus(1);
        }
        if (menu.getVisible() == null) {
            menu.setVisible(1);
        }
        sysMenuMapper.insert(menu);
    }

    public void update(Long id, MenuDTO dto) {
        SysMenu menu = BeanUtil.copyProperties(dto, SysMenu.class);
        menu.setId(id);
        sysMenuMapper.updateById(menu);
    }

    public void deleteByIds(String ids) {
        List<Long> idList = Arrays.stream(ids.split(",")).filter(StrUtil::isNotBlank).map(Long::valueOf).collect(Collectors.toList());
        for (Long id : idList) {
            Long childCount = sysMenuMapper.selectCount(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id));
            if (childCount != null && childCount > 0) {
                throw new BusinessException("存在子菜单，无法删除");
            }
            sysMenuMapper.deleteById(id);
        }
    }

    public List<SysMenu> routes() {
        return userMenus();
    }

    public List<SysMenu> userMenus() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            throw new BusinessException(401, "未登录");
        }
        List<SysMenu> menus;
        if (loginUser.isAdmin()) {
            menus = sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                    .eq(SysMenu::getStatus, 1)
                    .in(SysMenu::getType, 0, 1)
                    .orderByAsc(SysMenu::getSort));
        } else {
            menus = sysMenuMapper.selectMenusByUserId(loginUser.getUserId());
        }
        return buildTree(menus, 0L);
    }

    private List<SysMenu> buildTree(List<SysMenu> menus, Long parentId) {
        List<SysMenu> tree = new ArrayList<>();
        for (SysMenu menu : menus) {
            if (Objects.equals(menu.getParentId(), parentId)) {
                menu.setChildren(buildTree(menus, menu.getId()));
                tree.add(menu);
            }
        }
        return tree;
    }
}
