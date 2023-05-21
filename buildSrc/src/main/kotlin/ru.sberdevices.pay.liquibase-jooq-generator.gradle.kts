import dbgenerator.LiquibaseJooqCodeGenerator.CodeGeneratorParams
import dbgenerator.LiquibaseJooqCodeGenerator.runGeneration

plugins {
    java
    idea
    `java-library`
    id("org.liquibase.gradle")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation("javax.validation:validation-api:2.0.1.Final")
    implementation("org.jooq:jooq:3.14.4")
    implementation("org.postgresql:postgresql:42.3.3")
    implementation("org.springframework:spring-context:5.3.3")

}

open class LiquibaseJooqGeneratorExtension {
    var url = "jdbc://localhost:5432/postgres"
    var username = "postgres"
    var password = "password"
    var schemas: List<String> = listOf("public")
    var outputPackage: String = "generated.model"
    var generatedSourcesDirectory = "db-generated-src"
    var changelogDirectory: String = "changelog/changelog-master.yaml"
}

class LiquibaseJooqGeneratorPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val extension = target.extensions.create<LiquibaseJooqGeneratorExtension>("liquibaseJooq")
        val outputDirectory = "${target.buildDir}/extension.generatedSourcesDirectory"

        target.project.sourceSets["main"].java.srcDir(outputDirectory)
        target.task("generateJooqFromLiquibase") {
            doLast {
                runGeneration(
                    CodeGeneratorParams(
                        extension.schemas,
                        outputDirectory,
                        extension.outputPackage,
                        extension.changelogDirectory
                    )
                )
            }
        }

        target.tasks.getByName("compileJava").dependsOn("generateJooqFromLiquibase")
    }
}

apply<LiquibaseJooqGeneratorPlugin>()