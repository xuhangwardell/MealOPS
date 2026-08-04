package com.xuhang.mealops;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

import com.xuhang.mealops.support.PostgresTestConfiguration;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
class MealOpsApplicationIT {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Flyway flyway;

    @Test
    void startsWithPostgreSql18AndFlyway() throws SQLException {
        assertThat(applicationContext).isNotNull();
        assertThat(dataSource).isNotNull();
        assertThat(flyway).isNotNull();

        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SHOW server_version_num")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getInt(1) / 10_000).isEqualTo(18);
        }
    }

}
