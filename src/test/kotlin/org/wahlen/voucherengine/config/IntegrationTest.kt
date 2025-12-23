package org.wahlen.voucherengine.config

import io.awspring.cloud.autoconfigure.sqs.SqsAutoConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.annotation.AliasFor
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.wahlen.voucherengine.service.async.AsyncJobListener
import org.wahlen.voucherengine.service.async.AsyncJobPublisher

/**
 * Meta-annotation for integration tests.
 *
 * This annotation combines common test annotations:
 * - @SpringBootTest: Loads the full Spring application context
 * - @ActiveProfiles("test"): Activates the "test" profile
 * - Disables SQS autoconfiguration for faster test startup
 * - Mocks AsyncJobPublisher and AsyncJobListener (async functionality disabled)
 *
 * For tests that require real async/SQS functionality, use @SqsIntegrationTest instead.
 *
 * You can pass custom properties to override application settings:
 * ```
 * @IntegrationTest(properties = ["my.property=value"])
 * class MyIntegrationTest { ... }
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SpringBootTest
@ImportAutoConfiguration(exclude = [SqsAutoConfiguration::class])
@ActiveProfiles("test")
@MockitoBean(types = [AsyncJobPublisher::class])
@MockitoBean(types = [AsyncJobListener::class])
annotation class IntegrationTest(
    /**
     * Properties in form key=value that should be added to the Spring Environment.
     * Forwarded to @SpringBootTest.properties
     */
    @get:AliasFor(annotation = SpringBootTest::class, attribute = "properties")
    val properties: Array<String> = []
)
