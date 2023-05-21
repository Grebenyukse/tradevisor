plugins {
    java
    `kotlin-dsl`
    kotlin("jvm") version "1.5.10"
    id("org.liquibase.gradle") version "2.1.1"
}

java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17
group = "ru.grnk.tradevisor"
version = ""

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.springframework.boot:spring-boot-gradle-plugin:2.6.4")
    implementation("io.spring.dependency-management:io.spring.dependency-management.gradle.plugin:1.0.15.RELEASE")
    implementation("org.liquibase.gradle:org.liquibase.gradle.gradle.plugin:2.2.0")
    implementation("org.liquibase:liquibase-core:4.3.1")
    implementation("org.jooq:jooq-meta:3.14.4")
    implementation("org.jooq:jooq-codegen:3.14.4")
    implementation("org.gradle.kotlin.kotlin-dsl:org.gradle.kotlin.kotlin-dsl.gradle.plugin:3.0.0")
    implementation(platform("org.testcontainers:testcontainers-bom:1.15.2"))
    implementation("org.testcontainers:testcontainers")
    implementation("org.testcontainers:postgresql")
    implementation("org.postgresql:postgresql:42.3.3")
    implementation("org.yaml:snakeyaml:1.30")
    implementation("javax.validation:validation-api:2.0.1.Final")
}
