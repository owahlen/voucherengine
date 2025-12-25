package org.wahlen.voucherengine.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.wahlen.voucherengine.testinfra.ElasticMqExtension
import software.amazon.awssdk.services.sqs.SqsAsyncClient


/**
 * Test configuration for ElasticMQ (in-memory SQS)
 * ElasticMQ is started by ElasticMqExtension and properties are registered via @DynamicPropertySource
 *
 * Note: Message conversion is handled manually in AsyncJobListener.
 */
@TestConfiguration
@EnableConfigurationProperties(SqsQueueProperties::class)
class ElasticMQTestConfiguration {

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun registerElasticMqProperties(registry: DynamicPropertyRegistry) {
            ElasticMqExtension.registerSpringProperties(registry)
        }
    }

    @Bean
    fun elasticMQQueueInitializer(
        sqsAsyncClient: SqsAsyncClient,
        sqsQueueProperties: SqsQueueProperties
    ): ElasticMQQueueInitializer {
        return ElasticMQQueueInitializer(sqsAsyncClient, sqsQueueProperties)
    }
}

