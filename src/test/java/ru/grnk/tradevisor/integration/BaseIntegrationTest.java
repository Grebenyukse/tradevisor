package ru.grnk.tradevisor.integration;

import com.github.dockerjava.api.model.PortBinding;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.common.repository.MarketDataRepository;
import ru.grnk.tradevisor.common.repository.SignalsRepository;

import javax.sql.DataSource;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;

@Slf4j
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "spring.config.location=classpath:config/application-test.yaml",
                "server.port=8080"
        })
@Testcontainers
public abstract class BaseIntegrationTest {

    private static final int POSTGRES_PORT = 5432;
    protected static final String TEST_TICKER = "TEST_TICKER";
    protected static final String TEST_EXCHANGE = "TEST_EXCHANGE";
    protected static final String TEST_TICKER_CODE = TEST_TICKER + "@" + TEST_EXCHANGE;

    @Autowired
    protected TradevisorProperties properties;

    @Autowired
    protected MarketDataRepository marketDataRepository;

    @Autowired
    protected SignalsRepository signalsRepository;

    protected static DecimalFormat df = new DecimalFormat("#.#####");

    static {
        df.setRoundingMode(RoundingMode.CEILING);
    }

    @Container
    protected static final PostgreSQLContainer<?> postgresContainer =
            new PostgreSQLContainer<>("postgres:15")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test")
                    .withExposedPorts(POSTGRES_PORT)
                    .withCreateContainerCmdModifier(cmd -> cmd.withPortBindings(
                            PortBinding.parse(String.format("%d:5432", POSTGRES_PORT))
                    ));

    protected static DataSource dataSource;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
        registry.add("spring.liquibase.enabled", () -> "false");
    }

    @BeforeAll
    static void setUp() throws Exception {
        postgresContainer.start();
        runLiquibaseMigrations();
        dataSource = createDataSource();
        executeSqlScript("src/test/resources/sql/setup-test-data.sql");
    }

    @BeforeEach
    public void logProperties() {
        log.info("properties: {}", properties.toString());
    }

    @AfterAll
    static void tearDown() throws Exception {
        executeSqlScript("src/test/resources/sql/cleanup-test-data.sql");
        postgresContainer.stop();
    }

    private static void runLiquibaseMigrations() throws Exception {
        System.out.println("🚀 Запуск Liquibase миграций...");

        Connection connection = DriverManager.getConnection(
                postgresContainer.getJdbcUrl(),
                postgresContainer.getUsername(),
                postgresContainer.getPassword()
        );

        Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(connection));

        Liquibase liquibase = new Liquibase(
                "changelog/changelog-master.yaml",
                new ClassLoaderResourceAccessor(),
                database
        );

        liquibase.update("");
        connection.close();

        System.out.println("✅ Liquibase миграции завершены!");
    }

    private static DataSource createDataSource() {
        org.postgresql.ds.PGSimpleDataSource ds = new org.postgresql.ds.PGSimpleDataSource();
        ds.setUrl(postgresContainer.getJdbcUrl());
        ds.setUser(postgresContainer.getUsername());
        ds.setPassword(postgresContainer.getPassword());
        return ds;
    }

    @SneakyThrows
    protected static void executeSqlScript(String scriptPath) {
        String sql = Files.readString(Path.of(scriptPath));
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    // Вспомогательный метод для выполнения произвольных SQL запросов в тестах
    protected static void executeSql(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
