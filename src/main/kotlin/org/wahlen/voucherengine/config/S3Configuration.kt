package org.wahlen.voucherengine.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.net.URI
import java.time.Duration

/**
 * Configuration for AWS S3 client.
 * Uses LocalStack in development, real AWS S3 in production.
 * Not active in test profile (S3MockTestConfiguration is used instead).
 */
@Configuration
@org.springframework.context.annotation.Profile("!test")
class S3Configuration {

    @Value("\${aws.region.static:eu-central-1}")
    private lateinit var region: String

    @Value("\${aws.credentials.access-key:test}")
    private lateinit var accessKey: String

    @Value("\${aws.credentials.secret-key:test}")
    private lateinit var secretKey: String

    @Value("\${aws.s3.endpoint:#{null}}")
    private val s3Endpoint: String? = null

    @Value("\${aws.s3.force-path-style:false}")
    private val forcePathStyle: Boolean = false

    @Bean
    fun s3Client(): S3Client {
        val builder = S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)
                )
            )

        // Use endpoint override for LocalStack
        s3Endpoint?.let {
            builder.endpointOverride(URI.create(it))
            if (forcePathStyle) {
                builder.forcePathStyle(true)
            }
        }

        return builder.build()
    }

    @Bean
    fun s3Presigner(): S3Presigner {
        val builder = S3Presigner.builder()
            .region(Region.of(region))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)
                )
            )

        // Use endpoint override for LocalStack
        s3Endpoint?.let {
            builder.endpointOverride(URI.create(it))
        }

        return builder.build()
    }
}
