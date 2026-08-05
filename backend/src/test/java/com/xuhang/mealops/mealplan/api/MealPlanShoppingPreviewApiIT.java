package com.xuhang.mealops.mealplan.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
class MealPlanShoppingPreviewApiIT {
    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void generatedDraftUsesWholePlanRequirementsAndLiveInventoryWithoutSideEffects() throws Exception {
        long ingredientId = ingredient("Plan preview");
        long recipeId = recipe(ingredientId);
        long firstBatch = inventory(ingredientId, "100");
        preferences(ingredientId);
        int transactionCount = count("inventory_transaction");
        int allocationCount = count("inventory_transaction_allocation");

        var generated = mvc.perform(post("/api/v1/meal-plans/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startDate\":\"2026-08-06\",\"endDate\":\"2026-08-06\"," 
                                + "\"mealTypes\":[\"DINNER\",\"LUNCH\"]}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.slots[0].recipeSelection.recipeId").value((int) recipeId))
                .andExpect(jsonPath("$.slots[1].recipeSelection.recipeId").value((int) recipeId))
                .andReturn();
        long planId = ((Number) JsonPath.read(generated.getResponse().getContentAsString(), "$.id")).longValue();
        String planBefore = mvc.perform(get("/api/v1/meal-plans/{id}", planId)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mvc.perform(get("/api/v1/meal-plans/{id}/shopping-preview", planId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].requiredAmount").value(200))
                .andExpect(jsonPath("$.items[0].availableAmount").value(100))
                .andExpect(jsonPath("$.items[0].shortageAmount").value(100))
                .andExpect(jsonPath("$.items[0].unit").value("g"));
        assertThat(mvc.perform(get("/api/v1/meal-plans/{id}", planId)).andReturn()
                .getResponse().getContentAsString()).isEqualTo(planBefore);
        assertBatch(firstBatch, "100", 0);

        long secondBatch = inventory(ingredientId, "100");
        mvc.perform(get("/api/v1/meal-plans/{id}/shopping-preview", planId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty());
        assertBatch(firstBatch, "100", 0);
        assertBatch(secondBatch, "100", 0);
        assertThat(count("inventory_transaction")).isEqualTo(transactionCount);
        assertThat(count("inventory_transaction_allocation")).isEqualTo(allocationCount);

        mvc.perform(post("/api/v1/meal-plans/{id}/confirm", planId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMED"));
        mvc.perform(get("/api/v1/meal-plans/{id}/shopping-preview", planId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void mapsMissingIncompleteAndCancelledPlansToExistingProblemDetails() throws Exception {
        mvc.perform(get("/api/v1/meal-plans/{id}/shopping-preview", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("MEAL_PLAN_NOT_FOUND"));

        long ingredientId = ingredient("State API");
        long recipeId = recipe(ingredientId);
        long incomplete = manualPlan(recipeId, false);
        mvc.perform(get("/api/v1/meal-plans/{id}/shopping-preview", incomplete))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MEAL_PLAN_INCOMPLETE"));

        long cancelled = manualPlan(recipeId, true);
        mvc.perform(post("/api/v1/meal-plans/{id}/cancel", cancelled)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/meal-plans/{id}/shopping-preview", cancelled))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MEAL_PLAN_STATE_CONFLICT"));
    }

    @Test
    void openApiDocumentsReadOnlyPlanDerivedPreviewWithSharedShoppingSchema() throws Exception {
        String json = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/meal-plans/{id}/shopping-preview'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/meal-plans/{id}/shopping-preview'].get.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/meal-plans/{id}/shopping-preview'].get.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/meal-plans/{id}/shopping-preview'].get.requestBody").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        Map<String, Object> operation = JsonPath.read(json,
                "$.paths['/api/v1/meal-plans/{id}/shopping-preview'].get");
        assertThat(operation.keySet()).doesNotContain("requestBody");
        assertThat(json).contains("requiredAmount", "availableAmount", "shortageAmount", "unit");
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }

    private void assertBatch(long id, String amount, long version) {
        assertThat(jdbc.queryForObject("SELECT remaining_amount FROM inventory_batch WHERE id=?",
                BigDecimal.class, id)).isEqualByComparingTo(amount);
        assertThat(jdbc.queryForObject("SELECT version FROM inventory_batch WHERE id=?", Long.class, id))
                .isEqualTo(version);
    }

    private long ingredient(String prefix) throws Exception {
        var response = mvc.perform(post("/api/v1/ingredients").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + prefix + " " + UUID.randomUUID() + "\"}"))
                .andExpect(status().isCreated()).andReturn();
        return ((Number) JsonPath.read(response.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private long recipe(long ingredientId) throws Exception {
        String body = "{\"name\":\"Plan shopping " + UUID.randomUUID()
                + "\",\"baseServings\":1,\"estimatedMinutes\":10,\"ingredients\":[{\"ingredientId\":"
                + ingredientId + ",\"amount\":100,\"unit\":\"g\"}],\"steps\":[\"Cook\"]}";
        var response = mvc.perform(post("/api/v1/recipes").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        return ((Number) JsonPath.read(response.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private long inventory(long ingredientId, String amount) throws Exception {
        String body = "{\"ingredientId\":" + ingredientId + ",\"amount\":" + amount
                + ",\"unit\":\"g\",\"expiresOn\":null}";
        var response = mvc.perform(post("/api/v1/inventory/batches")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        return ((Number) JsonPath.read(response.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private void preferences(long allowedIngredientId) throws Exception {
        var excluded = jdbc.queryForList("SELECT id FROM ingredient WHERE id <> ? ORDER BY id",
                Long.class, allowedIngredientId);
        String excludedJson = excluded.stream().map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
        mvc.perform(put("/api/v1/planning-preferences").contentType(MediaType.APPLICATION_JSON)
                .content("{\"defaultServings\":1,\"maxCookingMinutes\":30,\"excludedIngredientIds\":"
                        + excludedJson + "}"))
                .andExpect(status().isOk());
    }

    private long manualPlan(long recipeId, boolean complete) throws Exception {
        String selection = complete
                ? "{\"recipeId\":" + recipeId + ",\"targetServings\":1}" : "null";
        String body = "{\"startDate\":\"2026-08-06\",\"endDate\":\"2026-08-06\",\"slots\":["
                + "{\"date\":\"2026-08-06\",\"mealType\":\"LUNCH\",\"recipeSelection\":"
                + selection + "}]}";
        var response = mvc.perform(post("/api/v1/meal-plans")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        return ((Number) JsonPath.read(response.getResponse().getContentAsString(), "$.id")).longValue();
    }
}
