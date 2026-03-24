package com.rongzhiqiao.auth.mapper;

import com.rongzhiqiao.auth.entity.SysUser;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SysUserMapper {

    @Select("""
            SELECT id,
                   account,
                   password_hash,
                   phone,
                   email,
                   nickname,
                   avatar_url,
                   status,
                   last_login_at,
                   created_at,
                   updated_at,
                   is_deleted
            FROM sys_user
            WHERE account = #{account}
              AND is_deleted = 0
            LIMIT 1
            """)
    @Results(id = "sysUserResultMap", value = {
            @Result(property = "passwordHash", column = "password_hash"),
            @Result(property = "avatarUrl", column = "avatar_url"),
            @Result(property = "lastLoginAt", column = "last_login_at"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "isDeleted", column = "is_deleted")
    })
    SysUser selectByAccount(@Param("account") String account);

    @Select("""
            SELECT id,
                   account,
                   password_hash,
                   phone,
                   email,
                   nickname,
                   avatar_url,
                   status,
                   last_login_at,
                   created_at,
                   updated_at,
                   is_deleted
            FROM sys_user
            WHERE email = #{email}
              AND is_deleted = 0
            LIMIT 1
            """)
    @Results(id = "sysUserByEmailResultMap", value = {
            @Result(property = "passwordHash", column = "password_hash"),
            @Result(property = "avatarUrl", column = "avatar_url"),
            @Result(property = "lastLoginAt", column = "last_login_at"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "isDeleted", column = "is_deleted")
    })
    SysUser selectByEmail(@Param("email") String email);

    @Select("""
            SELECT id,
                   account,
                   password_hash,
                   phone,
                   email,
                   nickname,
                   avatar_url,
                   status,
                   last_login_at,
                   created_at,
                   updated_at,
                   is_deleted
            FROM sys_user
            WHERE id = #{id}
              AND is_deleted = 0
            LIMIT 1
            """)
    @Results(id = "sysUserByIdResultMap", value = {
            @Result(property = "passwordHash", column = "password_hash"),
            @Result(property = "avatarUrl", column = "avatar_url"),
            @Result(property = "lastLoginAt", column = "last_login_at"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "isDeleted", column = "is_deleted")
    })
    SysUser selectById(@Param("id") Long id);

    @Insert("""
            INSERT INTO sys_user (
                account,
                password_hash,
                phone,
                email,
                nickname,
                avatar_url,
                status,
                is_deleted
            ) VALUES (
                #{account},
                #{passwordHash},
                #{phone},
                #{email},
                #{nickname},
                #{avatarUrl},
                #{status},
                #{isDeleted}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysUser user);

    @Update("""
            UPDATE sys_user
            SET last_login_at = #{lastLoginAt}
            WHERE id = #{id}
              AND is_deleted = 0
            """)
    int updateLastLoginAt(@Param("id") Long id, @Param("lastLoginAt") LocalDateTime lastLoginAt);

    @Update("""
            UPDATE sys_user
            SET password_hash = #{passwordHash}
            WHERE id = #{id}
              AND is_deleted = 0
            """)
    int updatePasswordHash(@Param("id") Long id, @Param("passwordHash") String passwordHash);
}
