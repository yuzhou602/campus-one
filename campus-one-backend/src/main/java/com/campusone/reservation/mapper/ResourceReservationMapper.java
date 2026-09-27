package com.campusone.reservation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusone.reservation.entity.ResourceReservation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ResourceReservationMapper extends BaseMapper<ResourceReservation> {
    @Select("""
            <script>
            SELECT rr.*
            FROM resource_reservation rr
            JOIN sys_user applicant
              ON applicant.id = rr.user_id AND applicant.deleted = 0
            WHERE rr.status = 'PENDING'
            <choose>
              <when test="scope == 'ALL'"></when>
              <when test="scope == 'COLLEGE'">
                AND applicant.college_id = #{collegeId}
              </when>
              <when test="scope == 'CLASS'">
                AND applicant.class_id = #{classId}
              </when>
              <otherwise>
                AND applicant.id = #{reviewerId}
              </otherwise>
            </choose>
            ORDER BY rr.created_at ASC
            </script>
            """)
    IPage<ResourceReservation> selectPendingForScope(
            Page<ResourceReservation> page,
            @Param("reviewerId") Long reviewerId,
            @Param("scope") String scope,
            @Param("collegeId") Long collegeId,
            @Param("classId") Long classId);
}
