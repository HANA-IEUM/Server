package com.hanaieum.server.infrastructure.aws;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;

@Service
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.bucket}")
    private String bucketName;

    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public String uploadFile(MultipartFile multipartFile) throws IOException {
        String fileName = multipartFile.getOriginalFilename();

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(fileName)

                        .contentType(multipartFile.getContentType())
                        .contentDisposition("inline")

                        .build(),
                RequestBody.fromInputStream(multipartFile.getInputStream(), multipartFile.getSize())
        );

        return "https://" + bucketName + ".s3.amazonaws.com/" + fileName;
    }

}
