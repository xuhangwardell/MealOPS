package com.xuhang.mealops.mealplan.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;
import com.xuhang.mealops.support.PostgresTestConfiguration;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
@Transactional
class MealPlanGenerationApiIT {
    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        jdbc.update("DELETE FROM meal_plan");
        jdbc.update("DELETE FROM recipe_ingredient");
        jdbc.update("DELETE FROM recipe_step");
        jdbc.update("DELETE FROM recipe");
    }

    @Test
    void generatesOrderedDraftWithoutInventoryMutationAndSupportsLifecycle() throws Exception {
        long x = ingredient("API generate X");
        long y = ingredient("API generate Y");
        long excluded = ingredient("API generate excluded");
        long a = recipe("A", 20, x);
        long b = recipe("B", 30, y);
        recipe("Filtered", 10, excluded);
        long batchX = inventory(x, "100", "2020-01-01");
        long batchY = inventory(y, "100", null);
        preferences(1, "40", "[" + excluded + "]");
        int transactionCount = count("inventory_transaction");
        int allocationCount = count("inventory_transaction_allocation");

        String body = "{\"startDate\":\"2026-08-06\",\"endDate\":\"2026-08-07\","
                + "\"mealTypes\":[\"DINNER\",\"LUNCH\"]}";
        var result = mvc.perform(post("/api/v1/meal-plans/generate")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/v1/meal-plans/\\d+")))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.slots.length()").value(4))
                .andExpect(jsonPath("$.slots[0].date").value("2026-08-06"))
                .andExpect(jsonPath("$.slots[0].mealType").value("LUNCH"))
                .andExpect(jsonPath("$.slots[0].recipeSelection.recipeId").value((int) a))
                .andExpect(jsonPath("$.slots[1].mealType").value("DINNER"))
                .andExpect(jsonPath("$.slots[1].recipeSelection.recipeId").value((int) b))
                .andExpect(jsonPath("$.slots[0].recipeSelection.targetServings").value(1))
                .andReturn();
        long planId = ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();

        mvc.perform(get("/api/v1/meal-plans/{id}", planId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.slots.length()").value(4));
        assertBatch(batchX, "100", 0);
        assertBatch(batchY, "100", 0);
        assertThat(count("inventory_transaction")).isEqualTo(transactionCount);
        assertThat(count("inventory_transaction_allocation")).isEqualTo(allocationCount);
        mvc.perform(post("/api/v1/meal-plans/{id}/confirm", planId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void validatesGenerationRequestAndReturnsStableNoCandidateConflict() throws Exception {
        List<String> invalidBodies = List.of(
                "{\"endDate\":\"2026-08-06\",\"mealTypes\":[\"LUNCH\"]}",
                "{\"startDate\":\"2026-08-06\",\"mealTypes\":[\"LUNCH\"]}",
                "{\"startDate\":\"2026-08-07\",\"endDate\":\"2026-08-06\",\"mealTypes\":[\"LUNCH\"]}",
                "{\"startDate\":\"2026-08-06\",\"endDate\":\"2026-08-09\",\"mealTypes\":[\"LUNCH\"]}",
                "{\"startDate\":\"2026-08-06\",\"endDate\":\"2026-08-06\"}",
                "{\"startDate\":\"2026-08-06\",\"endDate\":\"2026-08-06\",\"mealTypes\":null}",
                "{\"startDate\":\"2026-08-06\",\"endDate\":\"2026-08-06\",\"mealTypes\":[]}",
                "{\"startDate\":\"2026-08-06\",\"endDate\":\"2026-08-06\",\"mealTypes\":[null]}",
                "{\"startDate\":\"2026-08-06\",\"endDate\":\"2026-08-06\",\"mealTypes\":[\"LUNCH\",\"LUNCH\"]}",
                "{\"startDate\":\"2026-08-06\",\"endDate\":\"2026-08-06\",\"mealTypes\":[\"BRUNCH\"]}");
        for (String body : invalidBodies) {
            mvc.perform(post("/api/v1/meal-plans/generate").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }

        long ingredient = ingredient("API no candidate");
        recipe("Too slow", 60, ingredient);
        preferences(1, "10", "[]");
        int plans = count("meal_plan");
        int slots = count("meal_plan_slot");
        mvc.perform(post("/api/v1/meal-plans/generate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startDate\":\"2026-08-06\",\"endDate\":\"2026-08-06\",\"mealTypes\":[\"LUNCH\"]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MEAL_PLAN_NO_ELIGIBLE_RECIPE"));
        assertThat(count("meal_plan")).isEqualTo(plans);
        assertThat(count("meal_plan_slot")).isEqualTo(slots);
    }

    @Test
    void openApiDocumentsGenerateContractWithoutOverrides() throws Exception {
        var result = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/meal-plans/generate'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/meal-plans/generate'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/meal-plans/generate'].post.responses['409']").exists())
                .andReturn();
        String json = result.getResponse().getContentAsString();
        String requestRef = JsonPath.read(json,
                "$.paths['/api/v1/meal-plans/generate'].post.requestBody.content['application/json'].schema['$ref']");
        String requestName = requestRef.substring(requestRef.lastIndexOf('/') + 1);
        Map<String, Object> properties = JsonPath.read(json,
                "$.components.schemas['" + requestName + "'].properties");
        assertThat(properties.keySet()).containsExactlyInAnyOrder("startDate", "endDate", "mealTypes");
        assertThat(properties.keySet()).doesNotContain("recipeId", "targetServings", "weights",
                "defaultServings", "inventory", "autoConfirm");
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }

    private void assertBatch(long id, String amount, long version) {
        assertThat(jdbc.queryForObject("SELECT remaining_amount FROM inventory_batch WHERE id=?", BigDecimal.class, id))
                .isEqualByComparingTo(amount);
        assertThat(jdbc.queryForObject("SELECT version FROM inventory_batch WHERE id=?", Long.class, id))
                .isEqualTo(version);
    }

    private long ingredient(String prefix) throws Exception {
        var response = mvc.perform(post("/api/v1/ingredients").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + prefix + " " + UUID.randomUUID() + "\"}"))
                .andExpect(status().isCreated()).andReturn();
        return ((Number) JsonPath.read(response.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private long recipe(String prefix, int minutes, long ingredientId) throws Exception {
        String body = "{\"name\":\"" + prefix + " " + UUID.randomUUID()
                + "\",\"baseServings\":1,\"estimatedMinutes\":" + minutes
                + ",\"ingredients\":[{\"ingredientId\":" + ingredientId
                + ",\"amount\":100,\"unit\":\"g\"}],\"steps\":[\"Cook\"]}";
        var response = mvc.perform(post("/api/v1/recipes").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        return ((Number) JsonPath.read(response.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private long inventory(long ingredientId, String amount, String expiresOn) throws Exception {
        String expiry = expiresOn == null ? "null" : "\"" + expiresOn + "\"";
        String body = "{\"ingredientId\":" + ingredientId + ",\"amount\":" + amount
                + ",\"unit\":\"g\",\"expiresOn\":" + expiry + "}";
        var response = mvc.perform(post("/api/v1/inventory/batches").contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isCreated()).andReturn();
        return ((Number) JsonPath.read(response.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private void preferences(int servings, String max, String excluded) throws Exception {
        mvc.perform(put("/api/v1/planning-preferences").contentType(MediaType.APPLICATION_JSON)
                .content("{\"defaultServings\":" + servings + ",\"maxCookingMinutes\":" + max
                        + ",\"excludedIngredientIds\":" + excluded + "}"))
                .andExpect(status().isOk());
    }
}
