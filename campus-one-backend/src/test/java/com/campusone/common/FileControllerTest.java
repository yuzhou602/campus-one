package com.campusone.common;

import com.campusone.common.controller.FileController;
import com.campusone.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileControllerTest {
    @TempDir
    Path uploadDirectory;

    @Test
    void acceptsAFileWithMatchingExtensionMimeAndSignature() throws Exception {
        FileController controller = configuredController();
        byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};
        MockMultipartFile file = new MockMultipartFile("file", "campus.png", "image/png", png);

        var response = controller.upload(file);

        assertNotNull(response.getData());
        String storedName = response.getData().get("filename");
        assertTrue(storedName.endsWith(".png"));
        assertTrue(Files.isRegularFile(uploadDirectory.resolve(storedName)));
    }

    @Test
    void rejectsAFileWhoseContentDoesNotMatchItsDeclaredType() {
        FileController controller = configuredController();
        MockMultipartFile file = new MockMultipartFile(
                "file", "fake.png", "image/png", "not an image".getBytes());

        BusinessException error = assertThrows(BusinessException.class, () -> controller.upload(file));

        assertEquals(400, error.getCode());
    }

    private FileController configuredController() {
        FileController controller = new FileController();
        ReflectionTestUtils.setField(controller, "uploadDir", uploadDirectory.toString());
        ReflectionTestUtils.setField(controller, "baseUrl", "http://localhost:8080/api/v1/files");
        ReflectionTestUtils.setField(controller, "maxSizeBytes", 10_485_760L);
        return controller;
    }
}
