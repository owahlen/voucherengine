plugins {
	kotlin("jvm") version "2.3.0"
	kotlin("plugin.spring") version "2.3.0"
	id("org.springframework.boot") version "4.0.1"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("plugin.jpa") version "2.3.0"
}

group = "org.wahlen"
version = "0.0.1-SNAPSHOT"
description = "Voucher Engine"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-liquibase")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("tools.jackson.module:jackson-module-kotlin")
	implementation("com.google.zxing:core:3.5.3")
	implementation("com.google.zxing:javase:3.5.3")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.0")
	implementation("io.awspring.cloud:spring-cloud-aws-starter-sqs:4.0.0-M1")
	implementation("io.awspring.cloud:spring-cloud-aws-starter-s3:4.0.0-M1")

	runtimeOnly("org.postgresql:postgresql")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.boot:spring-boot-webmvc-test")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")

	// Testcontainers for LocalStack testing
	testImplementation("org.testcontainers:localstack:1.21.4")
	testImplementation("org.testcontainers:junit-jupiter:1.21.4")
	testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
	
	// ElasticMQ for in-memory SQS testing (like H2 for database)
	testImplementation("org.elasticmq:elasticmq-rest-sqs_2.13:1.6.15") {
		exclude(group = "ch.qos.logback", module = "logback-classic")
	}
	
	// S3Mock for in-memory S3 testing
	testImplementation("com.adobe.testing:s3mock-testcontainers:4.11.0")

	testRuntimeOnly("com.h2database:h2")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
