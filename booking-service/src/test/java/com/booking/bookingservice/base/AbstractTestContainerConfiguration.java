package com.booking.bookingservice.base;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Slf4j
@Testcontainers
public abstract class AbstractTestContainerConfiguration {

    private static final String POSTGRES_IMAGE = "postgres:16-alpine";

    protected static final PostgreSQLContainer PROFESSIONALS_POSTGRES =
            new PostgreSQLContainer(POSTGRES_IMAGE)
                    .withDatabaseName("professionalsdb")
                    .withUsername("postgres")
                    .withPassword("111111");

    @BeforeAll
    static void startContainer() {
        log.info("Starting PostgreSQL Testcontainer...");
        PROFESSIONALS_POSTGRES.start();
        log.info("PostgreSQL Testcontainer started on port: {}", PROFESSIONALS_POSTGRES.getMappedPort(5432));
    }

    @AfterAll
    static void stopContainer() {
        log.info("Stopping PostgreSQL Testcontainer...");
        PROFESSIONALS_POSTGRES.stop();
    }

    @DynamicPropertySource
    public static void overrideProps(DynamicPropertyRegistry registry) {
        // Dynamically override Spring Boot properties to use Testcontainers PostgreSQL
        registry.add("spring.datasource.url", PROFESSIONALS_POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", PROFESSIONALS_POSTGRES::getUsername);
        registry.add("spring.datasource.password", PROFESSIONALS_POSTGRES::getPassword);
    }

}