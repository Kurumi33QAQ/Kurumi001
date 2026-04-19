package com.zsj.modules.pms.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.zsj.common.api.ResultCode;
import com.zsj.common.config.OssProperties;
import com.zsj.common.exception.ApiException;
import com.zsj.modules.pms.dto.UploadResultDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final OssProperties ossProperties;

    private static final long MAX_SIZE = 5 * 1024 * 1024L;
    private static final Set<String> ALLOW_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    public UploadResultDTO uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ResultCode.VALIDATE_FAILED);
        }
        if (file.getSize() > MAX_SIZE) {
            throw new ApiException(ResultCode.VALIDATE_FAILED);
        }
        if (!ALLOW_TYPES.contains(file.getContentType())) {
            throw new ApiException(ResultCode.VALIDATE_FAILED);
        }

        String ext = getExt(file.getOriginalFilename());
        String datePath = LocalDate.now().toString().replace("-", "/"); // 2026/04/19
        String key = ossProperties.getDirPrefix() + "/" + datePath + "/" + UUID.randomUUID() + ext;

        OSS ossClient = new OSSClientBuilder().build(
                ossProperties.getEndpoint(),
                ossProperties.getAccessKeyId(),
                ossProperties.getAccessKeySecret()
        );

        try (InputStream in = file.getInputStream()) {
            ossClient.putObject(ossProperties.getBucketName(), key, in);
        } catch (Exception e) {
            throw new ApiException(ResultCode.FAILED);
        } finally {
            ossClient.shutdown();
        }

        String base = ossProperties.getPublicBaseUrl();
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        String url = base + "/" + key;
        return new UploadResultDTO(url, key);
    }

    private String getExt(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }
}
