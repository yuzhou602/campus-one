package com.campusone.common.controller;

import com.campusone.common.response.ApiResponse;
import com.campusone.common.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Locale;
import java.util.Set;

@Tag(name = "文件上传")
@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${file.base-url:http://localhost:8080/api/v1/files}")
    private String baseUrl;

    @Value("${file.max-size-bytes:10485760}")
    private long maxSizeBytes;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp", ".pdf");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "application/pdf");

    @Operation(summary = "上传文件")
    @PostMapping("/upload")
    public ApiResponse<Map<String, String>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new BusinessException(400, "文件不能为空");
        }
        if (file.getSize() > maxSizeBytes) {
            throw new BusinessException(400, "文件不能超过 10MB");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase(Locale.ROOT);
        }
        if (!ALLOWED_EXTENSIONS.contains(extension)
                || !ALLOWED_CONTENT_TYPES.contains(file.getContentType())
                || !hasValidSignature(file)) {
            throw new BusinessException(400, "仅支持 JPG、PNG、WebP 和 PDF 文件");
        }
        String filename = UUID.randomUUID().toString() + extension;

        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        Path target = uploadPath.resolve(filename).normalize();
        if (!target.startsWith(uploadPath)) {
            throw new BusinessException(400, "非法文件名");
        }
        file.transferTo(target);

        Map<String, String> result = new HashMap<>();
        result.put("url", baseUrl + "/" + filename);
        result.put("filename", filename);
        result.put("originalFilename", originalFilename);
        return ApiResponse.success(result);
    }

    @Operation(summary = "获取文件")
    @GetMapping("/{filename}")
    public void getFile(@PathVariable String filename, jakarta.servlet.http.HttpServletResponse response) throws IOException {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path filePath = uploadPath.resolve(filename).normalize();
        if (filePath.startsWith(uploadPath) && Files.isRegularFile(filePath)) {
            String contentType = Files.probeContentType(filePath);
            response.setContentType(contentType == null ? "application/octet-stream" : contentType);
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("Content-Disposition", "inline; filename=\"" + filePath.getFileName() + "\"");
            Files.copy(filePath, response.getOutputStream());
        } else {
            response.setStatus(404);
        }
    }

    private boolean hasValidSignature(MultipartFile file) throws IOException {
        byte[] header;
        try (var input = file.getInputStream()) {
            header = input.readNBytes(12);
        }
        String contentType = file.getContentType();
        if ("image/jpeg".equals(contentType)) {
            return header.length >= 3
                    && (header[0] & 0xFF) == 0xFF
                    && (header[1] & 0xFF) == 0xD8
                    && (header[2] & 0xFF) == 0xFF;
        }
        if ("image/png".equals(contentType)) {
            int[] signature = {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
            if (header.length < signature.length) return false;
            for (int i = 0; i < signature.length; i++) {
                if ((header[i] & 0xFF) != signature[i]) return false;
            }
            return true;
        }
        if ("image/webp".equals(contentType)) {
            return header.length >= 12
                    && new String(header, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")
                    && new String(header, 8, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP");
        }
        if ("application/pdf".equals(contentType)) {
            return header.length >= 5
                    && new String(header, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-");
        }
        return false;
    }
}
