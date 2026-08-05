package com.xuhang.mealops.planning.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class RecipeCandidateRankingApiIT {
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
    void ranksEligibleRecipesAndRefreshesDefaultServingsAndInventory() throws Exception {
        long a = ingredient("Rank A");
        long b = ingredient("Rank B");
        long excluded = ingredient("Rank excluded");
        long recipeA = recipe("A", 2, 20, a, "200", "g");
        long recipeB = recipe("B", 2, 10, b, "200", "g");
        recipe("Slow", 2, 40, a, "200", "g");
        recipe("Blocked", 2, 10, excluded, "200", "g");
        long batchA = inventory(a, "100", "g", "2020-01-01");
        preferences(1, "30", "[" + excluded + "]");

        int transactionCount = count("inventory_transaction");
        int allocationCount = count("inventory_transaction_allocation");
        int mealPlanCount = count("meal_plan");
        mvc.perform(get("/api/v1/recipe-candidate-rankings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].rank").value(1))
                .andExpect(jsonPath("$.items[0].recipeId").value((int) recipeA))
                .andExpect(jsonPath("$.items[0].targetServings").value(1))
                .andExpect(jsonPath("$.items[0].inventoryCoverageScore").value(1))
                .andExpect(jsonPath("$.items[0].shortageIngredientCount").value(0))
                .andExpect(jsonPath("$.items[1].rank").value(2))
                .andExpect(jsonPath("$.items[1].recipeId").value((int) recipeB));

        preferences(2, "30", "[" + excluded + "]");
        mvc.perform(get("/api/v1/recipe-candidate-rankings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].recipeId").value((int) recipeA))
                .andExpect(jsonPath("$.items[0].targetServings").value(2))
                .andExpect(jsonPath("$.items[0].inventoryCoverageScore").value(0.5));

        inventory(b, "200", "g", null);
        mvc.perform(get("/api/v1/recipe-candidate-rankings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].recipeId").value((int) recipeB))
                .andExpect(jsonPath("$.items[0].inventoryCoverageScore").value(1))
                .andExpect(jsonPath("$.items[1].recipeId").value((int) recipeA));

        assertThat(jdbc.queryForObject("SELECT remaining_amount FROM inventory_batch WHERE id=?", BigDecimal.class, batchA))
                .isEqualByComparingTo("100");
        assertThat(jdbc.queryForObject("SELECT version FROM inventory_batch WHERE id=?", Long.class, batchA)).isZero();
        assertThat(count("inventory_transaction")).isEqualTo(transactionCount);
        assertThat(count("inventory_transaction_allocation")).isEqualTo(allocationCount);
        assertThat(count("meal_plan")).isEqualTo(mealPlanCount);
    }

    @Test
    void returnsEmptyAndDocumentsOnlyTransparentScorecard() throws Exception {
        preferences(1, "null", "[]");
        mvc.perform(get("/api/v1/recipe-candidate-rankings"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(0));

        var result = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/recipe-candidate-rankings'].get.responses['200']").exists())
                .andReturn();
        String json = result.getResponse().getContentAsString();
        String responseRef = JsonPath.read(json,
                "$.paths['/api/v1/recipe-candidate-rankings'].get.responses['200'].content['application/json'].schema['$ref']");
        String responseName = responseRef.substring(responseRef.lastIndexOf('/') + 1);
        String itemRef = JsonPath.read(json,
                "$.components.schemas['" + responseName + "'].properties.items.items['$ref']");
        String itemName = itemRef.substring(itemRef.lastIndexOf('/') + 1);
        Map<String, Object> properties = JsonPath.read(json,
                "$.components.schemas['" + itemName + "'].properties");
        assertThat(properties.keySet()).containsExactlyInAnyOrder("rank", "recipeId", "name", "baseServings",
                "targetServings", "estimatedMinutes", "inventoryCoverageScore", "shortageIngredientCount");
        assertThat(properties.keySet()).doesNotContain("totalScore", "weights", "mealPlan", "planner");
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }

    private long ingredient(String prefix) throws Exception {
        var response = mvc.perform(post("/api/v1/ingredients").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + prefix + " " + UUID.randomUUID() + "\"}"))
                .andExpect(status().isCreated()).andReturn();
        return ((Number) JsonPath.read(response.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private long recipe(String prefix, int servings, int minutes, long ingredientId, String amount, String unit)
            throws Exception {
        String body = "{\"name\":\"" + prefix + " " + UUID.randomUUID() + "\",\"baseServings\":" + servings
                + ",\"estimatedMinutes\":" + minutes + ",\"ingredients\":[{\"ingredientId\":" + ingredientId
                + ",\"amount\":" + amount + ",\"unit\":\"" + unit + "\"}],\"steps\":[\"Cook\"]}";
        var response = mvc.perform(post("/api/v1/recipes").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        return ((Number) JsonPath.read(response.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private long inventory(long ingredientId, String amount, String unit, String expiresOn) throws Exception {
        String expiry = expiresOn == null ? "null" : "\"" + expiresOn + "\"";
        String body = "{\"ingredientId\":" + ingredientId + ",\"amount\":" + amount + ",\"unit\":\"" + unit
                + "\",\"expiresOn\":" + expiry + "}";
        var response = mvc.perform(post("/api/v1/inventory/batches").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        return ((Number) JsonPath.read(response.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private void preferences(int servings, String max, String exclusions) throws Exception {
        mvc.perform(put("/api/v1/planning-preferences").contentType(MediaType.APPLICATION_JSON)
                .content("{\"defaultServings\":" + servings + ",\"maxCookingMinutes\":" + max
                        + ",\"excludedIngredientIds\":" + exclusions + "}"))
                .andExpect(status().isOk());
    }
}
