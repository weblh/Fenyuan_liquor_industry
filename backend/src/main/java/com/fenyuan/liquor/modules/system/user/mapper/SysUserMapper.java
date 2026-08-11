package com.fenyuan.liquor.modules.system.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenyuan.liquor.modules.system.user.entity.SysUser;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId}")
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteUserRoles(@Param("userId") Long userId);

    @Insert("<script>INSERT INTO sys_user_role(user_id, role_id) VALUES " +
            "<foreach collection='roleIds' item='roleId' separator=','>(#{userId}, #{roleId})</foreach></script>")
    int insertUserRoles(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);
}
