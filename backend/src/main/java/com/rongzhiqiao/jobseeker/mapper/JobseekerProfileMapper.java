package com.rongzhiqiao.jobseeker.mapper;

import com.rongzhiqiao.jobseeker.entity.JobseekerProfile;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface JobseekerProfileMapper {

    @Select("""
            SELECT id,
                   user_id,
                   real_name,
                   gender,
                   birth_year,
                   school_name,
                   major,
                   degree,
                   graduation_year,
                   current_city,
                   target_city,
                   expected_job,
                   expected_salary_min,
                   expected_salary_max,
                   work_mode_preference,
                   intro,
                   profile_completion_rate,
                   created_at,
                   updated_at,
                   is_deleted
            FROM jobseeker_profile
            WHERE user_id = #{userId}
              AND is_deleted = 0
            LIMIT 1
            """)
    @Results(id = "jobseekerProfileResultMap", value = {
            @Result(property = "userId", column = "user_id"),
            @Result(property = "realName", column = "real_name"),
            @Result(property = "birthYear", column = "birth_year"),
            @Result(property = "schoolName", column = "school_name"),
            @Result(property = "graduationYear", column = "graduation_year"),
            @Result(property = "currentCity", column = "current_city"),
            @Result(property = "targetCity", column = "target_city"),
            @Result(property = "expectedJob", column = "expected_job"),
            @Result(property = "expectedSalaryMin", column = "expected_salary_min"),
            @Result(property = "expectedSalaryMax", column = "expected_salary_max"),
            @Result(property = "workModePreference", column = "work_mode_preference"),
            @Result(property = "profileCompletionRate", column = "profile_completion_rate"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "isDeleted", column = "is_deleted")
    })
    JobseekerProfile selectByUserId(@Param("userId") Long userId);

    @Insert("""
            INSERT INTO jobseeker_profile (
                user_id,
                real_name,
                gender,
                birth_year,
                school_name,
                major,
                degree,
                graduation_year,
                current_city,
                target_city,
                expected_job,
                expected_salary_min,
                expected_salary_max,
                work_mode_preference,
                intro,
                profile_completion_rate,
                is_deleted
            ) VALUES (
                #{userId},
                #{realName},
                #{gender},
                #{birthYear},
                #{schoolName},
                #{major},
                #{degree},
                #{graduationYear},
                #{currentCity},
                #{targetCity},
                #{expectedJob},
                #{expectedSalaryMin},
                #{expectedSalaryMax},
                #{workModePreference},
                #{intro},
                #{profileCompletionRate},
                #{isDeleted}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(JobseekerProfile profile);

    @Update("""
            UPDATE jobseeker_profile
            SET real_name = #{realName},
                gender = #{gender},
                birth_year = #{birthYear},
                school_name = #{schoolName},
                major = #{major},
                degree = #{degree},
                graduation_year = #{graduationYear},
                current_city = #{currentCity},
                target_city = #{targetCity},
                expected_job = #{expectedJob},
                expected_salary_min = #{expectedSalaryMin},
                expected_salary_max = #{expectedSalaryMax},
                work_mode_preference = #{workModePreference},
                intro = #{intro},
                profile_completion_rate = #{profileCompletionRate}
            WHERE user_id = #{userId}
              AND is_deleted = 0
            """)
    int updateByUserId(JobseekerProfile profile);
}
