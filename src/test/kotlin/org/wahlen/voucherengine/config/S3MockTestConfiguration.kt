package org.wahlen.voucherengine.config

import com.adobe.testing.s3mock.testcontainers.S3MockContainer
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import java.net.URI

/**
 * Test configuration for S3Mock (in-memory S3)
 * S3Mock provides a lightweight S3-compatible API for testing without LocalStack
 */
@TestConfiguration
class S3MockTestConfiguration {

    @Bean(initMethod = "start", destroyMethod = "stop")
    fun s3MockContainer(): S3MockContainer {
        return S3MockContainer("4.11.0")
            .withInitialBuckets("voucherengine-imports,voucherengine-exports")
    }

    @Bean
    fun s3Client(s3MockContainer: S3MockContainer): S3Client {
        val client = S3Client.builder()
            .region(Region.US_EAST_1)
            .endpointOverride(URI.create(s3MockContainer.httpEndpoint))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create("test", "test")
                )
            )
            .forcePathStyle(true)
            .build()

        // Ensure buckets exist (withInitialBuckets may not work in all versions)
        ensureBucketExists(client, "voucherengine-imports")
        ensureBucketExists(client, "voucherengine-exports")

        return client
    }

    @Bean
    fun s3Presigner(s3MockContainer: S3MockContainer): software.amazon.awssdk.services.s3.presigner.S3Presigner {
        return software.amazon.awssdk.services.s3.presigner.S3Presigner.builder()
            .region(Region.US_EAST_1)
            .endpointOverride(URI.create(s3MockContainer.httpEndpoint))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create("test", "test")
                )
            )
            .build()
    }

    @Bean
    fun objectMapper(): ObjectMapper {
        return ObjectMapper().findAndRegisterModules()
    }

    private fun ensureBucketExists(s3Client: S3Client, bucketName: String) {
        try {
            s3Client.headBucket { it.bucket(bucketName) }
        } catch (e: Exception) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build())
        }
    }
}
