package org.wahlen.voucherengine.testinfra

import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.support.TestPropertySourceUtils

/**
 * Spring ApplicationContextInitializer that configures the dynamic S3Mock endpoint.
 * Used by @S3IntegrationTest annotation.
 * 
 * Static properties (region, credentials, path-style-access) are configured in test application.yml.
 * This initializer only sets the dynamic endpoint property that depends on the running S3Mock instance.
 */
class S3MockPropertyInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        // Ensure S3Mock is started and get its dynamic endpoint
        val endpoint = S3MockExtension.endpoint()
        
        // Override only the endpoint property (everything else in application.yml)
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
            applicationContext,
            "spring.cloud.aws.s3.endpoint=$endpoint"
        )
    }
}
