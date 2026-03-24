package com.rongzhiqiao.auth.mapper;

import com.rongzhiqiao.auth.entity.SysUserRole;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysUserRoleMapper {

    @Insert({
            "<script>",
            "INSERT INTO sys_user_role (",
            "    user_id,",
            "    role_id",
            ") VALUES",
            "<foreach collection='items' item='item' separator=','>",
            "    (#{item.userId}, #{item.roleId})",
            "</foreach>",
            "</script>"
    })
    int batchInsert(@Param("items") List<SysUserRole> items);
}
