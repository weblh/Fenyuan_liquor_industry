package com.fenyuan.liquor.modules.system.role.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fenyuan.liquor.common.exception.BusinessException;
import com.fenyuan.liquor.common.result.PageResult;
import com.fenyuan.liquor.modules.system.role.dto.RoleDTO;
import com.fenyuan.liquor.modules.system.role.entity.SysRole;
import com.fenyuan.liquor.modules.system.role.mapper.SysRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysRoleService {

    private final SysRoleMapper sysRoleMapper;

    public PageResult<SysRole> page(long current, long size, String name, Integer status) {
        Page<SysRole> page = sysRoleMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<SysRole>()
                        .like(StrUtil.isNotBlank(name), SysRole::getName, name)
                        .eq(status != null, SysRole::getStatus, status)
                        .orderByAsc(SysRole::getSort));
        return PageResult.of(page);
    }

    public List<SysRole> listAll() {
        return sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getStatus, 1)
                .orderByAsc(SysRole::getSort));
    }

    public SysRole getById(Long id) {
        SysRole role = sysRoleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        return role;
    }

    @Transactional(rollbackFor = Exception.class)
    public void create(RoleDTO dto) {
        Long count = sysRoleMapper.selectCount(new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, dto.getCode()));
        if (count != null && count > 0) {
            throw new BusinessException("角色编码已存在");
        }
        SysRole role = BeanUtil.copyProperties(dto, SysRole.class);
        if (role.getStatus() == null) {
            role.setStatus(1);
        }
        sysRoleMapper.insert(role);
        if (dto.getMenuIds() != null) {
            saveMenus(role.getId(), dto.getMenuIds());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, RoleDTO dto) {
        SysRole role = BeanUtil.copyProperties(dto, SysRole.class);
        role.setId(id);
        sysRoleMapper.updateById(role);
        if (dto.getMenuIds() != null) {
            saveMenus(id, dto.getMenuIds());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(String ids) {
        List<Long> idList = Arrays.stream(ids.split(",")).filter(StrUtil::isNotBlank).map(Long::valueOf).collect(Collectors.toList());
        for (Long id : idList) {
            if (id == 1L) {
                throw new BusinessException("不能删除超级管理员角色");
            }
            sysRoleMapper.deleteById(id);
            sysRoleMapper.deleteRoleMenus(id);
        }
    }

    public void updateStatus(Long id, Integer status) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setStatus(status);
        sysRoleMapper.updateById(role);
    }

    public Map<String, Object> getPermissions(Long id) {
        Map<String, Object> map = new HashMap<>();
        map.put("roleId", id);
        map.put("menuIds", sysRoleMapper.selectMenuIdsByRoleId(id));
        return map;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updatePermissions(Long id, List<Long> menuIds) {
        saveMenus(id, menuIds);
    }

    private void saveMenus(Long roleId, List<Long> menuIds) {
        sysRoleMapper.deleteRoleMenus(roleId);
        if (menuIds != null && !menuIds.isEmpty()) {
            sysRoleMapper.insertRoleMenus(roleId, menuIds);
        }
    }
}
