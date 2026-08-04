package com.xuhang.mealops.planning.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.xuhang.mealops.support.PostgresTestConfiguration;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class PlanningPreferencesFreshApiIT {
    @Autowired WebApplicationContext context;
    MockMvc mvc;

    @BeforeEach void setup() { mvc = MockMvcBuilders.webAppContextSetup(context).build(); }

    @Test
    void getsMigrationDefaultsWithoutPreferenceWrite() throws Exception {
        mvc.perform(get("/api/v1/planning-preferences")).andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultServings").value(1))
                .andExpect(jsonPath("$.maxCookingMinutes").doesNotExist())
                .andExpect(jsonPath("$.excludedIngredientIds.length()").value(0));
    }
}
