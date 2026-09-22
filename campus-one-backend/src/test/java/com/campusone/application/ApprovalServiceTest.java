package com.campusone.application;

import com.campusone.application.dto.ApprovalAction;
import com.campusone.application.dto.ApplicationDTO;
import com.campusone.application.entity.ApprovalRecord;
import com.campusone.application.entity.ServiceApplication;
import com.campusone.application.mapper.ApprovalRecordMapper;
import com.campusone.application.mapper.ServiceApplicationMapper;
import com.campusone.application.service.impl.ApprovalServiceImpl;
import com.campusone.common.exception.BusinessException;
import com.campusone.system.user.entity.User;
import com.campusone.system.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApprovalServiceTest {

    @Mock
    private ServiceApplicationMapper applicationMapper;
    @Mock
    private ApprovalRecordMapper approvalRecordMapper;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private ApprovalServiceImpl approvalService;

    private ServiceApplication testApp;
    private ApprovalRecord testRecord;

    @BeforeEach
    void setUp() {
        testApp = new ServiceApplication();
        testApp.setId(1L);
        testApp.setApplicantId(100L);
        testApp.setStatus("PENDING");

        testRecord = new ApprovalRecord();
        testRecord.setId(1L);
        testRecord.setApplicationId(1L);
        testRecord.setNodeName("辅导员审批");
        testRecord.setAssigneeId(1L);
        testRecord.setAction("PENDING");
    }

    private ApprovalAction action(String type) {
        ApprovalAction action = new ApprovalAction();
        action.setAction(type);
        action.setComment("同意");
        return action;
    }

    @Test
    @DisplayName("提交申请 - 创建申请并初始化审批步骤")
    void testSubmitApplication_InitializesSteps() {
        when(applicationMapper.insert(any(ServiceApplication.class))).thenReturn(1);
        when(approvalRecordMapper.insert(any(ApprovalRecord.class))).thenReturn(1);
        when(userMapper.selectOne(any())).thenReturn(user(5L, "COUNSELOR", "王职工"));

        ApplicationDTO dto = new ApplicationDTO();
        dto.setServiceId(10L);
        dto.setFormData("{}");
        ServiceApplication result = approvalService.submitApplication(dto, 100L);

        assertNotNull(result);
        assertEquals("PENDING", result.getStatus());
        assertTrue(result.getApplicationNo().startsWith("APP"));
        verify(approvalRecordMapper, times(2)).insert(any(ApprovalRecord.class));
    }

    private User user(Long id, String role, String name) {
        User u = new User();
        u.setId(id);
        u.setRole(role);
        u.setRealName(name);
        u.setUsername(name);
        return u;
    }

    @Test
    @DisplayName("审批通过 - 无后续等待节点时申请转为APPROVED")
    void testApprove_NoRemaining() {
        when(applicationMapper.selectById(1L)).thenReturn(testApp);
        // 第一次 selectOne 取当前待审批记录，第二次查等待节点返回 null → 办结
        when(approvalRecordMapper.selectOne(any())).thenReturn(testRecord, (ApprovalRecord) null);
        when(approvalRecordMapper.updateById(any(ApprovalRecord.class))).thenReturn(1);
        when(applicationMapper.updateById(any(ServiceApplication.class))).thenReturn(1);

        approvalService.processApproval(1L, 1L, action("APPROVE"));

        assertEquals("APPROVED", testRecord.getAction());
        assertEquals("APPROVED", testApp.getStatus());
        assertNotNull(testApp.getCompletedAt());
    }

    @Test
    @DisplayName("审批通过 - 有后续等待节点时激活为待审批并保持PENDING")
    void testApprove_ActivateNextWait() {
        ApprovalRecord nextRec = new ApprovalRecord();
        nextRec.setId(2L);
        nextRec.setApplicationId(1L);
        nextRec.setNodeName("管理员审批");
        nextRec.setAssigneeId(1L);
        nextRec.setAction("WAIT");

        when(applicationMapper.selectById(1L)).thenReturn(testApp);
        // 第一次取当前待审批记录，第二次命中下一等待节点 → 激活
        when(approvalRecordMapper.selectOne(any())).thenReturn(testRecord, nextRec);
        when(approvalRecordMapper.updateById(any(ApprovalRecord.class))).thenReturn(1);
        when(applicationMapper.updateById(any(ServiceApplication.class))).thenReturn(1);

        approvalService.processApproval(1L, 1L, action("APPROVE"));

        assertEquals("APPROVED", testRecord.getAction());
        assertEquals("PENDING", nextRec.getAction());
        assertEquals("管理员审批", testApp.getCurrentNode());
        assertEquals("PENDING", testApp.getStatus());
        assertNull(testApp.getCompletedAt());
    }

    @Test
    @DisplayName("审批驳回 - 申请转为REJECTED并跳过剩余步骤")
    void testReject_RejectsApplication() {
        when(applicationMapper.selectById(1L)).thenReturn(testApp);
        when(approvalRecordMapper.selectOne(any())).thenReturn(testRecord);
        when(approvalRecordMapper.updateById(any(ApprovalRecord.class))).thenReturn(1);
        when(applicationMapper.updateById(any(ServiceApplication.class))).thenReturn(1);
        when(approvalRecordMapper.update(any(), any())).thenReturn(1);

        approvalService.processApproval(1L, 1L, action("REJECT"));

        assertEquals("REJECTED", testRecord.getAction());
        assertEquals("REJECTED", testApp.getStatus());
        verify(approvalRecordMapper).update(any(), any());
    }

    @Test
    @DisplayName("非当前步骤审批人不能审批")
    void testProcessApproval_NotAssignee() {
        testRecord.setAssigneeId(2L);
        when(applicationMapper.selectById(1L)).thenReturn(testApp);
        when(approvalRecordMapper.selectOne(any())).thenReturn(testRecord);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> approvalService.processApproval(1L, 1L, action("APPROVE")));
        assertEquals("您不是该步骤的审批人", ex.getMessage());
    }

    @Test
    @DisplayName("已被处理的申请不能再次审批")
    void testProcessApproval_InvalidStatus() {
        testApp.setStatus("APPROVED");
        when(applicationMapper.selectById(1L)).thenReturn(testApp);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> approvalService.processApproval(1L, 1L, action("APPROVE")));
        assertEquals("该申请当前状态不允许审批", ex.getMessage());
    }

    @Test
    @DisplayName("无待审批记录时抛异常")
    void testProcessApproval_NoPendingRecord() {
        when(applicationMapper.selectById(1L)).thenReturn(testApp);
        when(approvalRecordMapper.selectOne(any())).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> approvalService.processApproval(1L, 1L, action("APPROVE")));
        assertEquals("无待审批记录", ex.getMessage());
    }

    @Test
    @DisplayName("申请不存在时抛异常")
    void testProcessApproval_ApplicationNotFound() {
        when(applicationMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> approvalService.processApproval(999L, 1L, action("APPROVE")));
        assertEquals("申请不存在", ex.getMessage());
    }

    @Test
    @DisplayName("退回上一步 - 当前步骤置为已退回，上一步重新激活")
    void testRollback_ActivatesPrevious() {
        ApprovalRecord prev = new ApprovalRecord();
        prev.setId(1L);
        prev.setApplicationId(1L);
        prev.setNodeName("职工审批");
        prev.setAssigneeId(1L);
        prev.setAction("APPROVED");

        testRecord.setId(2L);
        testRecord.setNodeName("管理员审批");

        when(applicationMapper.selectById(1L)).thenReturn(testApp);
        when(approvalRecordMapper.selectOne(any())).thenReturn(testRecord, prev);
        when(approvalRecordMapper.updateById(any(ApprovalRecord.class))).thenReturn(1);
        when(applicationMapper.updateById(any(ServiceApplication.class))).thenReturn(1);

        approvalService.rollbackApproval(1L, 1L, action("APPROVE"));

        assertEquals("ROLLED_BACK", testRecord.getAction());
        assertEquals("PENDING", prev.getAction());
        assertEquals("职工审批", testApp.getCurrentNode());
        assertEquals("PENDING", testApp.getStatus());
    }

    @Test
    @DisplayName("退回上一步 - 首审节点无上级可退")
    void testRollback_NoPrevious() {
        when(applicationMapper.selectById(1L)).thenReturn(testApp);
        when(approvalRecordMapper.selectOne(any())).thenReturn(testRecord, (ApprovalRecord) null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> approvalService.rollbackApproval(1L, 1L, action("APPROVE")));
        assertEquals("已是首审节点，无法退回上一步", ex.getMessage());
    }

    @Test
    @DisplayName("申请人催办 - 累加催办次数")
    void testUrge_Increments() {
        when(applicationMapper.selectById(1L)).thenReturn(testApp);
        when(applicationMapper.updateById(any(ServiceApplication.class))).thenReturn(1);

        approvalService.urgeApplication(1L, 100L);

        assertEquals(Integer.valueOf(1), testApp.getUrgeCount());
        assertNotNull(testApp.getLastUrgedAt());
    }

    @Test
    @DisplayName("申请人催办 - 非申请人本人不可催办")
    void testUrge_NotApplicant() {
        when(applicationMapper.selectById(1L)).thenReturn(testApp);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> approvalService.urgeApplication(1L, 99L));
        assertEquals("仅申请人本人可催办", ex.getMessage());
    }

    @Test
    @DisplayName("申请人催办 - 已完成申请不可催办")
    void testUrge_AlreadyClosed() {
        testApp.setStatus("APPROVED");
        when(applicationMapper.selectById(1L)).thenReturn(testApp);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> approvalService.urgeApplication(1L, 100L));
        assertEquals("仅处理中的申请可催办", ex.getMessage());
    }
}
