plugins {
    java
    `kotlin-dsl`
    kotlin("jvm") version "1.9.20"
    id("org.liquibase.gradle") version "2.2.0"
}

java.sourceCompatibility = JavaVersion.VERSION_21
java.targetCompatibility = JavaVersion.VERSION_21

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "21"
    }
}
group = "ru.grnk.tradevisor"
version = ""

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.springframework.boot:spring-boot-gradle-plugin:3.2.4")
    implementation("io.spring.dependency-management:io.spring.dependency-management.gradle.plugin:1.1.4")
    implementation("org.liquibase.gradle:org.liquibase.gradle.gradle.plugin:2.2.0")
    implementation("org.liquibase:liquibase-core:4.26.0")
    implementation("org.jooq:jooq-meta:3.14.4")
    implementation("org.jooq:jooq-codegen:3.14.4")
    implementation("org.gradle.kotlin.kotlin-dsl:org.gradle.kotlin.kotlin-dsl.gradle.plugin:3.0.0")
    implementation(platform("org.testcontainers:testcontainers-bom:1.19.7"))
    implementation("org.testcontainers:testcontainers")
    implementation("org.testcontainers:postgresql")
    implementation("org.postgresql:postgresql:42.7.1")
    implementation("org.yaml:snakeyaml:1.30")
    implementation("javax.validation:validation-api:2.0.1.Final")
}
