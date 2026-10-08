package com.campusone.reservation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ReservationDTO {
    @NotNull(message = "资源ID不能为空")
    private Long resourceId;
    @NotNull(message = "预约日期不能为空")
    private LocalDate reservationDate;
    @NotBlank(message = "开始时间不能为空")
    private String startTime;
    @NotBlank(message = "结束时间不能为空")
    private String endTime;
    @NotBlank(message = "预约用途不能为空")
    @Size(max = 500, message = "预约用途不能超过500个字符")
    private String purpose;
    @NotNull(message = "参与人数不能为空")
    @Min(value = 1, message = "参与人数至少为1人")
    private Integer attendeeCount;
}
