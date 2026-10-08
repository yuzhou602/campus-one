package com.campusone.application;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;

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
        testApp.setServiceId(1L);
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
        when(applicationMapper.insert(any(ServiceApplication.class))).thenAnswer(invocation -> {
            ServiceApplication application = invocation.getArgument(0);
            application.setId(1L);
            return 1;
        });
        when(applicationMapper.selectById(1L)).thenAnswer(invocation -> testApp);
        when(approvalRecordMapper.insert(any(ApprovalRecord.class))).thenReturn(1);
        User applicant = user(100L, "STUDENT", "张同学");
        applicant.setStatus(1);
        applicant.setCollegeId(1L);
        when(userMapper.selectById(100L)).thenReturn(applicant);
        User counselor = user(5L, "COUNSELOR", "王职工");
        counselor.setDataScope("COLLEGE");
        counselor.setCollegeId(1L);
        User admin = user(1L, "ADMIN", "管理员");
        admin.setDataScope("ALL");
        when(userMapper.selectList(any())).thenReturn(List.of(counselor), List.of(admin));

        ApplicationDTO dto = new ApplicationDTO();
        dto.setServiceId(2L);
        dto.setFormData("{}");
        ServiceApplication result = approvalService.submitApplication(dto, 100L);

        assertNotNull(result);
        assertEquals("PENDING", result.getStatus());
        assertTrue(result.getApplicationNo().startsWith("APP"));
        verify(approvalRecordMapper, times(2)).insert(any(ApprovalRecord.class));
    }

    @Test
    @DisplayName("提交申请 - 未知服务事项必须拒绝")
    void testSubmitApplication_RejectsUnknownService() {
        ApplicationDTO dto = new ApplicationDTO();
        dto.setServiceId(999L);
        dto.setFormData("{}");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> approvalService.submitApplication(dto, 100L));

        assertEquals("服务事项不存在或已停用", ex.getMessage());
        verify(applicationMapper, never()).insert(any(ServiceApplication.class));
    }

    @Test
    @DisplayName("我的申请 - 返回分页契约并补充服务与申请人名称")
    void testGetMyApprovals_ReturnsEnrichedPage() {
        Page<ServiceApplication> page = new Page<>(1, 20);
        page.setRecords(List.of(testApp));
        User applicant = user(100L, "STUDENT", "张同学");
        applicant.setSchoolId(20260001L);
        when(applicationMapper.selectPage(any(Page.class), any())).thenReturn(page);
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(applicant));

        IPage<ServiceApplication> result = approvalService.getMyApprovals(100L, 1, 20, null);

        assertEquals(1, result.getRecords().size());
        assertEquals("请假申请", result.getRecords().get(0).getServiceName());
        assertEquals("张同学", result.getRecords().get(0).getApplicantName());
        assertEquals(20260001L, result.getRecords().get(0).getStudentNo());
    }

    @Test
    @DisplayName("初始化审批 - 不允许回退到固定管理员账号")
    void testInitApprovalSteps_FailsWithoutScopedAssignee() {
        when(applicationMapper.selectById(1L)).thenReturn(testApp);
        User applicant = user(100L, "STUDENT", "张同学");
        applicant.setStatus(1);
        applicant.setCollegeId(2L);
        when(userMapper.selectById(100L)).thenReturn(applicant);
        when(userMapper.selectList(any())).thenReturn(List.of());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> approvalService.initApprovalSteps(1L));

        assertTrue(ex.getMessage().contains("未找到可处理该申请"));
        verify(approvalRecordMapper, never()).insert(any(ApprovalRecord.class));
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
        when(approvalRecordMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);
        when(applicationMapper.updateById(any(ServiceApplication.class))).thenReturn(1);

        approvalService.processApproval(1L, 1L, action("APPROVE"));

        assertEquals("APPROVED", testRecord.getAction());
        assertEquals("APPROVED", testApp.getStatus());
        assertEquals("已归档", testApp.getCurrentNode());
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
        when(approvalRecordMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);
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
        when(approvalRecordMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);
        when(applicationMapper.updateById(any(ServiceApplication.class))).thenReturn(1);
        when(approvalRecordMapper.update(any(ApprovalRecord.class), any())).thenReturn(1);

        approvalService.processApproval(1L, 1L, action("REJECT"));

        assertEquals("REJECTED", testRecord.getAction());
        assertEquals("REJECTED", testApp.getStatus());
        assertEquals("已驳回", testApp.getCurrentNode());
        assertNotNull(testApp.getCompletedAt());
        verify(approvalRecordMapper).update(any(ApprovalRecord.class), any());
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
    @DisplayName("未知审批动作不会被当作驳回")
    void testProcessApproval_InvalidAction() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> approvalService.processApproval(1L, 1L, action("UNKNOWN")));

        assertEquals("审批动作不合法", ex.getMessage());
        verifyNoInteractions(applicationMapper, approvalRecordMapper);
    }

    @Test
    @DisplayName("并发审批时只有首个请求可以认领待审批步骤")
    void testProcessApproval_ConcurrentConflict() {
        when(applicationMapper.selectById(1L)).thenReturn(testApp);
        when(approvalRecordMapper.selectOne(any())).thenReturn(testRecord);
        when(approvalRecordMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> approvalService.processApproval(1L, 1L, action("APPROVE")));

        assertEquals(409, ex.getCode());
        assertEquals("审批状态已变化，请刷新后重试", ex.getMessage());
        verify(applicationMapper, never()).updateById(any(ServiceApplication.class));
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
        when(approvalRecordMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);
        when(approvalRecordMapper.insert(any(ApprovalRecord.class))).thenReturn(1);
        when(applicationMapper.updateById(any(ServiceApplication.class))).thenReturn(1);

        approvalService.rollbackApproval(1L, 1L, action("APPROVE"));

        assertEquals("ROLLED_BACK", testRecord.getAction());
        assertEquals("PENDING", prev.getAction());
        assertEquals("职工审批", testApp.getCurrentNode());
        assertEquals("PENDING", testApp.getStatus());
        verify(approvalRecordMapper).insert(argThat((ApprovalRecord retry) ->
                "管理员审批".equals(retry.getNodeName()) && "WAIT".equals(retry.getAction())));
    }

    @Test
    @DisplayName("终审退回后重新通过仍需再次终审")
    void testRollback_RecreatesFinalApprovalStep() {
        ApprovalRecord previous = new ApprovalRecord();
        previous.setId(1L);
        previous.setApplicationId(1L);
        previous.setNodeName("职工审批");
        previous.setAssigneeId(1L);
        previous.setAction("APPROVED");
        testRecord.setId(2L);
        testRecord.setNodeName("管理员审批");
        testRecord.setAssigneeName("管理员");

        when(applicationMapper.selectById(1L)).thenReturn(testApp);
        when(approvalRecordMapper.selectOne(any())).thenReturn(testRecord, previous);
        when(approvalRecordMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);
        when(approvalRecordMapper.insert((ApprovalRecord) any())).thenReturn(1);
        when(applicationMapper.updateById(any(ServiceApplication.class))).thenReturn(1);

        approvalService.rollbackApproval(1L, 1L, action("APPROVE"));

        ArgumentCaptor<ApprovalRecord> retryCaptor = ArgumentCaptor.forClass(ApprovalRecord.class);
        verify(approvalRecordMapper).insert(retryCaptor.capture());
        ApprovalRecord retry = retryCaptor.getValue();
        when(approvalRecordMapper.selectOne(any())).thenReturn(previous, retry);

        approvalService.processApproval(1L, 1L, action("APPROVE"));

        assertEquals("PENDING", testApp.getStatus());
        assertEquals("管理员审批", testApp.getCurrentNode());
        assertEquals("PENDING", retry.getAction());
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
