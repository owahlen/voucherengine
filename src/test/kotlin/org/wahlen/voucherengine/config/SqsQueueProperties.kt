package org.wahlen.voucherengine.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for SQS queues.
 *
 * Binds all properties under "aws.sqs.queues" prefix directly into a flat map.
 * Spring Boot will automatically populate this map from the YAML configuration.
 *
 * Example in application.yml:
 * ```yaml
 * aws:
 *   sqs:
 *     queues:
 *       async-jobs: async-jobs
 *       notifications: notifications
 * ```
 *
 * This creates a map: {"async-jobs": "async-jobs", "notifications": "notifications"}
 */
@ConfigurationProperties(prefix = "aws.sqs")
class SqsQueueProperties {
    var queues: Map<String, String> = emptyMap()

    /**
     * Returns all queue names as a collection
     */
    fun getAllQueueNames(): Collection<String> = queues.values

    /**
     * Returns a specific queue name by key
     */
    fun getQueueName(key: String): String? = queues[key]
}

