package com.campusone.repair;

import com.campusone.common.exception.BusinessException;
import com.campusone.repair.controller.RepairController;
import com.campusone.repair.entity.RepairOrder;
import com.campusone.repair.service.RepairService;
import com.campusone.security.UserContext;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class RepairControllerTest {

    @Test
    void serviceUserCannotViewRepairAssignedToAnotherTechnician() {
        RepairService repairService = mock(RepairService.class);
        RepairController controller = new RepairController(repairService);
        RepairOrder order = new RepairOrder();
        order.setId(9L);
        order.setUserId(100L);
        order.setAssignedUserId(200L);
        when(repairService.getRepairById(9L)).thenReturn(order);

        try (MockedStatic<UserContext> context = mockStatic(UserContext.class)) {
            context.when(UserContext::getCurrentUserId).thenReturn(201L);
            context.when(UserContext::getCurrentUserRole).thenReturn("SERVICE");

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> controller.getById(9L));

            assertEquals(403, exception.getCode());
            assertEquals("无权访问", exception.getMessage());
        }
    }
}
