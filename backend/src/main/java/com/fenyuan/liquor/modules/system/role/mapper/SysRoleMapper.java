package com.fenyuan.liquor.modules.system.role.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenyuan.liquor.modules.system.role.entity.SysRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    @Select("SELECT r.* FROM sys_role r INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.del_flag = 0")
    List<SysRole> selectRolesByUserId(@Param("userId") Long userId);

    @Select("SELECT menu_id FROM sys_role_menu WHERE role_id = #{roleId}")
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);

    @Delete("DELETE FROM sys_role_menu WHERE role_id = #{roleId}")
    int deleteRoleMenus(@Param("roleId") Long roleId);

    @Insert("<script>INSERT INTO sys_role_menu(role_id, menu_id) VALUES " +
            "<foreach collection='menuIds' item='menuId' separator=','>(#{roleId}, #{menuId})</foreach></script>")
    int insertRoleMenus(@Param("roleId") Long roleId, @Param("menuIds") List<Long> menuIds);
}
