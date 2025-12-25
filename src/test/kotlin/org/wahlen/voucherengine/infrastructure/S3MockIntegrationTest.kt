package org.wahlen.voucherengine.infrastructure

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.wahlen.voucherengine.config.S3IntegrationTest
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@S3IntegrationTest
class S3MockIntegrationTest {

    @Autowired
    private lateinit var s3Client: S3Client

    @Test
    fun `should store and retrieve object from S3Mock`() {
        // Given
        val bucketName = "voucherengine-imports"
        val key = "test/sample.csv"
        val content = "id,name\n1,test"

        // When - Store object
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build(),
            RequestBody.fromString(content)
        )

        // Then - Retrieve and verify
        val response = s3Client.getObject(
            GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build()
        )

        val retrieved = response.readAllBytes().decodeToString()
        assertEquals(content, retrieved)
    }

    @Test
    fun `should list buckets created on initialization`() {
        // When
        val buckets = s3Client.listBuckets().buckets()

        // Then
        val bucketNames = buckets.map { it.name() }
        assertTrue(bucketNames.contains("voucherengine-imports"))
        assertTrue(bucketNames.contains("voucherengine-exports"))
    }
}