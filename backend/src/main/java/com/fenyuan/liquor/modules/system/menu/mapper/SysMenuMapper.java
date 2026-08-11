package com.fenyuan.liquor.modules.system.menu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenyuan.liquor.modules.system.menu.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    @Select("SELECT DISTINCT m.permission FROM sys_menu m " +
            "INNER JOIN sys_role_menu rm ON m.id = rm.menu_id " +
            "INNER JOIN sys_user_role ur ON rm.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND m.del_flag = 0 AND m.status = 1 AND m.permission IS NOT NULL")
    List<String> selectPermsByUserId(@Param("userId") Long userId);

    @Select("SELECT DISTINCT m.* FROM sys_menu m " +
            "INNER JOIN sys_role_menu rm ON m.id = rm.menu_id " +
            "INNER JOIN sys_user_role ur ON rm.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND m.del_flag = 0 AND m.status = 1 AND m.type IN (0, 1) " +
            "ORDER BY m.sort ASC")
    List<SysMenu> selectMenusByUserId(@Param("userId") Long userId);
}
