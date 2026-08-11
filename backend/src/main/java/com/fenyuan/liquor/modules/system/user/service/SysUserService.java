package com.fenyuan.liquor.modules.system.user.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fenyuan.liquor.common.exception.BusinessException;
import com.fenyuan.liquor.common.result.PageResult;
import com.fenyuan.liquor.modules.system.user.dto.UserDTO;
import com.fenyuan.liquor.modules.system.user.dto.UserVO;
import com.fenyuan.liquor.modules.system.user.entity.SysUser;
import com.fenyuan.liquor.modules.system.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    public PageResult<UserVO> page(long current, long size, String username, Integer status, Long deptId) {
        Page<SysUser> page = sysUserMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<SysUser>()
                        .like(StrUtil.isNotBlank(username), SysUser::getUsername, username)
                        .eq(status != null, SysUser::getStatus, status)
                        .eq(deptId != null, SysUser::getDeptId, deptId)
                        .orderByDesc(SysUser::getCreateTime));
        List<UserVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        PageResult<UserVO> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(page.getTotal());
        result.setSize(page.getSize());
        result.setCurrent(page.getCurrent());
        result.setPages(page.getPages());
        return result;
    }

    public UserVO getById(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return toVO(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void create(UserDTO dto) {
        Long count = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.getUsername()));
        if (count != null && count > 0) {
            throw new BusinessException("用户名已存在");
        }
        SysUser user = BeanUtil.copyProperties(dto, SysUser.class);
        String raw = StrUtil.blankToDefault(dto.getPassword(), "admin123");
        user.setPassword(passwordEncoder.encode(raw));
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        if (user.getIsAdmin() == null) {
            user.setIsAdmin(0);
        }
        sysUserMapper.insert(user);
        saveRoles(user.getId(), dto.getRoleIds());
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, UserDTO dto) {
        SysUser exist = sysUserMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("用户不存在");
        }
        SysUser user = BeanUtil.copyProperties(dto, SysUser.class);
        user.setId(id);
        user.setUsername(null);
        user.setPassword(null);
        sysUserMapper.updateById(user);
        if (dto.getRoleIds() != null) {
            saveRoles(id, dto.getRoleIds());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(String ids) {
        List<Long> idList = Arrays.stream(ids.split(","))
                .filter(StrUtil::isNotBlank)
                .map(Long::valueOf)
                .collect(Collectors.toList());
        for (Long id : idList) {
            if (id == 1L) {
                throw new BusinessException("不能删除超级管理员");
            }
            sysUserMapper.deleteById(id);
            sysUserMapper.deleteUserRoles(id);
        }
    }

    public void updateStatus(Long id, Integer status) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(status);
        sysUserMapper.updateById(user);
    }

    public void resetPassword(Long id, String password) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setPassword(passwordEncoder.encode(StrUtil.blankToDefault(password, "admin123")));
        sysUserMapper.updateById(user);
    }

    private void saveRoles(Long userId, List<Long> roleIds) {
        sysUserMapper.deleteUserRoles(userId);
        if (roleIds != null && !roleIds.isEmpty()) {
            sysUserMapper.insertUserRoles(userId, roleIds);
        }
    }

    private UserVO toVO(SysUser user) {
        UserVO vo = BeanUtil.copyProperties(user, UserVO.class);
        vo.setRoleIds(sysUserMapper.selectRoleIdsByUserId(user.getId()));
        return vo;
    }
}
