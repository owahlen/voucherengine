package org.wahlen.voucherengine.async

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

/**
 * Configuration to provide the test-specific SqsQueueListener as a bean
 */
@TestConfiguration
class SqsQueueTestConfiguration {
    @Bean
    fun sqsQueueListener(): SqsQueueListener {
        return SqsQueueListener()
    }
}

