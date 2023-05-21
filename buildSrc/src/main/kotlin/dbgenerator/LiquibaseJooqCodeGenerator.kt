package dbgenerator

import liquibase.Contexts
import liquibase.LabelExpression
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.FileSystemResourceAccessor
import org.jooq.codegen.GenerationTool
import org.jooq.meta.jaxb.*
import org.jooq.meta.jaxb.Target
import org.slf4j.LoggerFactory
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.io.File
import java.sql.DriverManager
import java.sql.SQLException
import javax.validation.constraints.NotNull


object LiquibaseJooqCodeGenerator {
    private val log = LoggerFactory.getLogger(LiquibaseJooqCodeGenerator::class.java)

    private const val POSTGRES_DEFAULT_IMAGE_NAME = "postgres:12"

    data class CodeGeneratorParams(
        val schemas: List<String>,
        val outputDirectory: String,
        val outputPackage: String,
        val changelogDirectory: String
    )

    data class DbConnectionParams(
        val url: String,
        val username: String,
        val password: String
    )

    fun runGeneration(params: CodeGeneratorParams) {
        PostgreSQLContainer<Nothing>(
            DockerImageName.parse(POSTGRES_DEFAULT_IMAGE_NAME)
                .asCompatibleSubstituteFor("postgres")
        ).apply {
            withReuse(true)
            withUsername("postgres")
            withPassword("postgres")
        }.use { dbContainer ->
            dbContainer.start()
            getConnection(dbContainer).use { connection ->
                val database: liquibase.database.Database =
                    DatabaseFactory.getInstance().findCorrectDatabaseImplementation(JdbcConnection(connection))
                Liquibase(params.changelogDirectory, FileSystemResourceAccessor(File("/")), database)
                    .use { liquibase ->
                        liquibase.update(Contexts("local"), LabelExpression())
                        GenerationTool.generate(
                            generateJooqConfig(
                                DbConnectionParams(
                                    dbContainer.jdbcUrl,
                                    dbContainer.username,
                                    dbContainer.password
                                ),
                                params
                            )
                        )
                    }
            }
        }
        log.info("JOOQ DB Code Generation finished")
    }

    private fun getConnection(dbContainer: PostgreSQLContainer<Nothing>) = try { // Костыль!
        DriverManager.getConnection(dbContainer.jdbcUrl, dbContainer.username, dbContainer.password)
    } catch (e: SQLException) {
        DriverManager.getConnection(dbContainer.jdbcUrl, dbContainer.username, dbContainer.password)
    }

    private fun generateJooqConfig(dbParams: DbConnectionParams, commonParams: CodeGeneratorParams): Configuration {
        return Configuration()
            .withJdbc(
                Jdbc()
                    .withDriver("org.postgresql.Driver")
                    .withUrl(dbParams.url)
                    .withUser(dbParams.username)
                    .withPassword(dbParams.password)
            )
            .withGenerator(Generator()
                .withDatabase(Database()
                    .withName("org.jooq.meta.postgres.PostgresDatabase")
                    .withForceIntegerTypesOnZeroScaleDecimals(true)
                    .withIncludes(".*")
                    .withExcludes("")
                    .withSchemata(
                        commonParams.schemas.map { SchemaMappingType().withInputSchema(it) }
                    )
                    .withRecordVersionFields("rec_version")
                )
                .withGenerate(
                    Generate()
                        .withJavaTimeTypes(true)
                        .withPojos(true)
                        .withImmutablePojos(false)
                        .withFluentSetters(true)
                        .withDaos(false)
                        .withPojosEqualsAndHashCode(true)
                        .withPojosToString(true)
                        .withDeprecated(false)
                        .withRelations(true)
                        .withGlobalObjectReferences(true)
                        .withGeneratedAnnotation(true)
                        .withGeneratedAnnotationType(GeneratedAnnotationType.DETECT_FROM_JDK)
                        .withNullableAnnotation(false)
                        .withNonnullAnnotation(true)
                        .withNonnullAnnotationType(NotNull::class.java.canonicalName)
                        .withJpaAnnotations(false)
                        .withValidationAnnotations(true)
                        .withSpringAnnotations(true)
                        .withSources(true)
                )
                .withStrategy(
                    Strategy()
                        .withName("org.jooq.codegen.DefaultGeneratorStrategy")
                )
                .withTarget(
                    Target()
                        .withPackageName(commonParams.outputPackage)
                        .withDirectory(commonParams.outputDirectory)
                        .withClean(true)
                )
            )
    }
}