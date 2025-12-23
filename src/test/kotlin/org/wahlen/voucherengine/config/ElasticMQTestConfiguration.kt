package org.wahlen.voucherengine.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import software.amazon.awssdk.services.sqs.SqsAsyncClient


/**
 * Test configuration for ElasticMQ (in-memory SQS)
 * ElasticMQ runs on port 9324, configured in application.yml
 *
 * Note: Message conversion is handled manually in AsyncJobListener.
 */
@TestConfiguration
@EnableConfigurationProperties(SqsQueueProperties::class)
class ElasticMQTestConfiguration {

    @Bean
    fun elasticMQQueueInitializer(
        sqsAsyncClient: SqsAsyncClient,
        sqsQueueProperties: SqsQueueProperties
    ): ElasticMQQueueInitializer {
        return ElasticMQQueueInitializer(sqsAsyncClient, sqsQueueProperties)
    }
}

