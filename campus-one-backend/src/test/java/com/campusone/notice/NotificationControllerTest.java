package com.campusone.notice;

import com.campusone.common.exception.BusinessException;
import com.campusone.notice.controller.NotificationController;
import com.campusone.notice.service.impl.NoticeServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@SuppressWarnings("deprecation")
class NotificationControllerTest {

    @Test
    void deleteDelegatesToCanonicalNoticeService() {
        NoticeServiceImpl noticeService = mock(NoticeServiceImpl.class);
        NotificationController controller = new NotificationController(noticeService);

        controller.delete(9L);

        verify(noticeService).deleteNotice(9L);
    }

    @Test
    void deleteRejectsMissingNotification() {
        NoticeServiceImpl noticeService = mock(NoticeServiceImpl.class);
        doThrow(new BusinessException("通知不存在")).when(noticeService).deleteNotice(9L);
        NotificationController controller = new NotificationController(noticeService);

        assertThrows(BusinessException.class, () -> controller.delete(9L));

        verify(noticeService).deleteNotice(9L);
    }
}
