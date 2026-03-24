package com.rongzhiqiao.admin.repository;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AdminUserManagementMapper {

    @Select("""
            SELECT u.id AS user_id,
                   u.account,
                   u.nickname,
                   u.email,
                   u.phone,
                   u.status,
                   COALESCE(GROUP_CONCAT(DISTINCT r.role_code ORDER BY r.role_code SEPARATOR ','), '') AS role_codes,
                   u.last_login_at,
                   u.created_at
            FROM sys_user u
            LEFT JOIN sys_user_role ur ON ur.user_id = u.id
            LEFT JOIN sys_role r ON ur.role_id = r.id
                                 AND r.is_deleted = 0
            WHERE u.is_deleted = 0
            GROUP BY u.id,
                     u.account,
                     u.nickname,
                     u.email,
                     u.phone,
                     u.status,
                     u.last_login_at,
                     u.created_at
            ORDER BY u.created_at DESC, u.id DESC
            """)
    @Results(id = "adminUserSummaryRecordMap", value = {
            @Result(property = "userId", column = "user_id"),
            @Result(property = "roleCodes", column = "role_codes"),
            @Result(property = "lastLoginAt", column = "last_login_at"),
            @Result(property = "createdAt", column = "created_at")
    })
    List<AdminUserSummaryRecord> selectAllUsers();
}
