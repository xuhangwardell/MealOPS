package com.xuhang.mealops.planning.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import com.xuhang.mealops.support.PostgresTestConfiguration;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class PlanningPreferencesFreshSeedIT {
    @Autowired JdbcTemplate jdbc;

    @Test
    void v5MigrationSeedsOnlyDefaultSingletonBeforeAnyReplace() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM planning_preferences", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT id FROM planning_preferences", Long.class)).isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT default_servings FROM planning_preferences WHERE id=1", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT max_cooking_minutes FROM planning_preferences WHERE id=1", Integer.class)).isNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM planning_preference_excluded_ingredient", Integer.class)).isZero();
    }
}
