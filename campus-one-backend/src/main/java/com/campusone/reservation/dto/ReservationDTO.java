package com.campusone.reservation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ReservationDTO {
    @NotNull(message = "资源ID不能为空")
    private Long resourceId;
    @NotNull(message = "预约日期不能为空")
    private LocalDate reservationDate;
    @NotNull(message = "开始时间不能为空")
    private String startTime;
    @NotNull(message = "结束时间不能为空")
    private String endTime;
    private String purpose;
    private Integer attendeeCount;
}
