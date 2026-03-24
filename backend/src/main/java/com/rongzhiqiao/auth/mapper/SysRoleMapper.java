package com.rongzhiqiao.auth.mapper;

import com.rongzhiqiao.auth.entity.SysRole;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysRoleMapper {

    @Select({
            "<script>",
            "SELECT id,",
            "       role_code,",
            "       role_name,",
            "       created_at,",
            "       updated_at,",
            "       is_deleted",
            "FROM sys_role",
            "WHERE is_deleted = 0",
            "  AND role_code IN",
            "<foreach collection='roleCodes' item='roleCode' open='(' separator=',' close=')'>",
            "  #{roleCode}",
            "</foreach>",
            "</script>"
    })
    @Results(id = "sysRoleResultMap", value = {
            @Result(property = "roleCode", column = "role_code"),
            @Result(property = "roleName", column = "role_name"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "isDeleted", column = "is_deleted")
    })
    List<SysRole> selectByRoleCodes(@Param("roleCodes") List<String> roleCodes);

    @Select("""
            SELECT r.role_code
            FROM sys_user_role ur
            INNER JOIN sys_role r ON ur.role_id = r.id
            WHERE ur.user_id = #{userId}
              AND r.is_deleted = 0
            """)
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);
}
