import org.jetbrains.kotlin.konan.properties.Properties

plugins {
    java
    kotlin("jvm")
    id("ru.sberdevices.pay.liquibase-jooq-generator")
    id("org.liquibase.gradle")
}

group = "ru.grnk.tradevisor"
version = "01.000.00"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    liquibaseRuntime("org.liquibase:liquibase-core:4.3.1")
    liquibaseRuntime("org.liquibase:liquibase-groovy-dsl:3.0.2")
    liquibaseRuntime("ch.qos.logback:logback-core:1.2.9")
    liquibaseRuntime("ch.qos.logback:logback-classic:1.2.9")
    liquibaseRuntime("org.postgresql:postgresql:42.3.3")
    liquibaseRuntime("info.picocli:picocli:4.6.1")
    liquibaseRuntime("org.yaml:snakeyaml:1.30")
    implementation("info.picocli:picocli:4.6.1")
    implementation("org.yaml:snakeyaml:1.30")
}

liquibase {
    val properties = Properties()
    val propertyFile = file("liquibase.properties")

    if (propertyFile.exists()) {
        properties.load(propertyFile.inputStream())
    }

    activities.register("main") {
        this.arguments = mapOf(
            "url" to properties["url"],
            "changeLogFile" to "src/main/resources/changelog/changelog-master.yaml",
            "username" to properties["username"],
            "password" to properties["password"],
            "defaultSchemaName" to properties["schema"],
            "liquibaseSchemaName" to properties["liquibaseSchemaName"],
            "logLevel" to "debug"
        )
    }
}

tasks.register<Zip>("liquibaseChangelogArchive") {
    from("$buildDir/resources/main/changelog/")
    archiveFileName.set("sql.zip")
    destinationDirectory.set(file("$buildDir/dist"))
}
tasks.getByName("build").finalizedBy("liquibaseChangelogArchive")

liquibaseJooq {
    val osName = System.getProperty("os.name").toLowerCase()
    val projectDir = project(":tradevisor-db").projectDir

    // Формируем исходный путь
    val rawPath = projectDir.resolve("src/main/resources/changelog/changelog-master.yaml")

    // Обрабатываем путь для Windows
    val changelogPath = if (osName.contains("win")) {
        rawPath.toString()
                .replaceFirst("^[A-Za-z]:".toRegex(), "") // Удаляем префикс диска
                .replace("\\", "/") // Нормализуем разделители
    } else {
        rawPath.toString()
    }

    logger.lifecycle("Processed changelog path: $changelogPath")
    logger.lifecycle("File exists: ${File(changelogPath).exists()}")

    changelogDirectory = changelogPath
    schemas = listOf("tradevisor")
    outputPackage = "ru.grnk.tradevisor.dbmodel"
}

tasks.register("prepareKotlinBuildScriptModel"){}