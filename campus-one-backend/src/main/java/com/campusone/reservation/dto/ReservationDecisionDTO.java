package com.campusone.reservation.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReservationDecisionDTO {
    @Size(max = 500, message = "审核意见不能超过500字")
    private String comment;
}
