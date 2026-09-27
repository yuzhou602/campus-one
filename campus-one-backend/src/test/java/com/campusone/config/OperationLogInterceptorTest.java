package com.campusone.config;

import com.campusone.security.UserContext;
import com.campusone.system.log.entity.OperationLog;
import com.campusone.system.log.mapper.OperationLogMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OperationLogInterceptorTest {

    @Test
    void recordsAuthenticatedActorAndDoesNotTrustForwardedIpDirectly() throws Exception {
        OperationLogMapper mapper = mock(OperationLogMapper.class);
        when(mapper.insert(any(OperationLog.class))).thenReturn(1);
        OperationLogInterceptor interceptor = new OperationLogInterceptor(mapper);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/applications");
        request.setRemoteAddr("10.0.0.8");
        request.addHeader("X-Forwarded-For", "203.0.113.10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        try (MockedStatic<UserContext> context = mockStatic(UserContext.class)) {
            context.when(UserContext::getCurrentUserId).thenReturn(42L);
            interceptor.preHandle(request, response, new Object());
            interceptor.afterCompletion(request, response, new Object(), null);
        }

        ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
        verify(mapper).insert(captor.capture());
        assertEquals(42L, captor.getValue().getUserId());
        assertEquals("10.0.0.8", captor.getValue().getIp());
        assertEquals("applications", captor.getValue().getModule());
    }
}
