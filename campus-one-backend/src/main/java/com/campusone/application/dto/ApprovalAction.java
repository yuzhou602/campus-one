package com.campusone.application.dto;

import lombok.Data;

@Data
public class ApprovalAction {
    private String action; // APPROVE, REJECT, RETURN
    private String comment;
}
