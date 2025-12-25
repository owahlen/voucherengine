package org.wahlen.voucherengine.config

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

/**
 * Base annotation for integration tests that require S3
 * Uses S3Mock (lightweight S3 emulation) instead of LocalStack
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = [ElasticMQInitializer::class])
@Import(ElasticMQTestConfiguration::class, S3MockTestConfiguration::class)
annotation class S3IntegrationTest
