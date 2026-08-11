package com.fenyuan.liquor.modules.system.dept.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenyuan.liquor.common.exception.BusinessException;
import com.fenyuan.liquor.modules.system.dept.dto.DeptDTO;
import com.fenyuan.liquor.modules.system.dept.entity.SysDept;
import com.fenyuan.liquor.modules.system.dept.mapper.SysDeptMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysDeptService {

    private final SysDeptMapper sysDeptMapper;

    public List<SysDept> tree() {
        List<SysDept> depts = sysDeptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                .orderByAsc(SysDept::getSort));
        return buildTree(depts, 0L);
    }

    public SysDept getById(Long id) {
        SysDept dept = sysDeptMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException("部门不存在");
        }
        return dept;
    }

    public void create(DeptDTO dto) {
        SysDept dept = BeanUtil.copyProperties(dto, SysDept.class);
        if (dept.getParentId() == null) {
            dept.setParentId(0L);
        }
        if (dept.getStatus() == null) {
            dept.setStatus(1);
        }
        sysDeptMapper.insert(dept);
    }

    public void update(Long id, DeptDTO dto) {
        SysDept dept = BeanUtil.copyProperties(dto, SysDept.class);
        dept.setId(id);
        sysDeptMapper.updateById(dept);
    }

    public void deleteByIds(String ids) {
        List<Long> idList = Arrays.stream(ids.split(",")).filter(StrUtil::isNotBlank).map(Long::valueOf).collect(Collectors.toList());
        for (Long id : idList) {
            Long childCount = sysDeptMapper.selectCount(new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, id));
            if (childCount != null && childCount > 0) {
                throw new BusinessException("存在子部门，无法删除");
            }
            sysDeptMapper.deleteById(id);
        }
    }

    private List<SysDept> buildTree(List<SysDept> depts, Long parentId) {
        List<SysDept> tree = new ArrayList<>();
        for (SysDept dept : depts) {
            if (Objects.equals(dept.getParentId(), parentId)) {
                dept.setChildren(buildTree(depts, dept.getId()));
                tree.add(dept);
            }
        }
        return tree;
    }
}
