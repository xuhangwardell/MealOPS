package com.xuhang.mealops.mealplan.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
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
import org.springframework.web.context.WebApplicationContext;
import com.jayway.jsonpath.JsonPath;
import com.xuhang.mealops.support.PostgresTestConfiguration;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
class MealSlotCompletionApiIT {
    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    MockMvc mvc;
    @BeforeEach void setUp() { mvc = MockMvcBuilders.webAppContextSetup(context).build(); }

    @Test
    void completesConfirmedSlotWithoutBodyAndMakesRetryNoOp() throws Exception {
        long ingredient = ingredient(); long recipe = recipe(ingredient); inventory(ingredient, "100");
        long plan = plan(recipe, "2026-08-06", "DINNER");
        mvc.perform(post("/api/v1/meal-plans/{id}/confirm", plan)).andExpect(status().isOk())
                .andExpect(jsonPath("$.slots[0].executionStatus").value("PENDING"));
        int transactions = count("inventory_transaction");

        mvc.perform(post("/api/v1/meal-plans/{id}/slots/{date}/{type}/complete", plan, "2026-08-06", "DINNER"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.slots[0].executionStatus").value("COMPLETED"));
        assertThat(count("inventory_transaction")).isEqualTo(transactions + 1);
        assertThat(jdbc.queryForObject("SELECT remaining_amount FROM inventory_batch WHERE ingredient_id=?",
                java.math.BigDecimal.class, ingredient)).isEqualByComparingTo("0");

        mvc.perform(post("/api/v1/meal-plans/{id}/slots/{date}/{type}/complete", plan, "2026-08-06", "DINNER"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
        assertThat(count("inventory_transaction")).isEqualTo(transactions + 1);
        mvc.perform(get("/api/v1/meal-plans/{id}/shopping-preview", plan)).andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void mapsStateSlotInventoryAndPathErrorsAndDocumentsEndpoint() throws Exception {
        long ingredient = ingredient(); long recipe = recipe(ingredient);
        long draft = plan(recipe, "2026-08-07", "LUNCH");
        mvc.perform(post("/api/v1/meal-plans/{id}/slots/{date}/{type}/complete", draft, "2026-08-07", "LUNCH"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("MEAL_PLAN_STATE_CONFLICT"));
        mvc.perform(post("/api/v1/meal-plans/{id}/confirm", draft)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/meal-plans/{id}/slots/{date}/{type}/complete", draft, "2026-08-07", "DINNER"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("MEAL_PLAN_SLOT_NOT_FOUND"));
        mvc.perform(post("/api/v1/meal-plans/{id}/slots/{date}/{type}/complete", draft, "2026-08-07", "LUNCH"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INSUFFICIENT_INVENTORY"));
        mvc.perform(post("/api/v1/meal-plans/{id}/slots/{date}/{type}/complete", draft, "bad-date", "LUNCH"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(post("/api/v1/meal-plans/{id}/slots/{date}/{type}/complete", draft, "2026-08-07", "BRUNCH"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void mapsUnknownAndCancelledPlansWithoutInventoryMutation() throws Exception {
        mvc.perform(post("/api/v1/meal-plans/{id}/slots/{date}/{type}/complete",
                        Long.MAX_VALUE, "2026-08-08", "LUNCH"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("MEAL_PLAN_NOT_FOUND"));
        long ingredient = ingredient(); long recipe = recipe(ingredient); inventory(ingredient, "100");
        long plan = plan(recipe, "2026-08-08", "LUNCH");
        mvc.perform(post("/api/v1/meal-plans/{id}/cancel", plan)).andExpect(status().isOk());
        var amount = jdbc.queryForObject("SELECT remaining_amount FROM inventory_batch WHERE ingredient_id=?",
                java.math.BigDecimal.class, ingredient);
        var version = jdbc.queryForObject("SELECT version FROM inventory_batch WHERE ingredient_id=?",
                Long.class, ingredient);
        int transactions = count("inventory_transaction");
        int allocations = count("inventory_transaction_allocation");
        mvc.perform(post("/api/v1/meal-plans/{id}/slots/{date}/{type}/complete",
                        plan, "2026-08-08", "LUNCH"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("MEAL_PLAN_STATE_CONFLICT"));
        assertThat(jdbc.queryForObject("SELECT remaining_amount FROM inventory_batch WHERE ingredient_id=?",
                java.math.BigDecimal.class, ingredient)).isEqualByComparingTo(amount);
        assertThat(jdbc.queryForObject("SELECT version FROM inventory_batch WHERE ingredient_id=?",
                Long.class, ingredient)).isEqualTo(version);
        assertThat(count("inventory_transaction")).isEqualTo(transactions);
        assertThat(count("inventory_transaction_allocation")).isEqualTo(allocations);
        assertThat(jdbc.queryForObject("SELECT execution_status FROM meal_plan_slot WHERE meal_plan_id=?",
                String.class, plan)).isEqualTo("PENDING");
    }

    @Test
    void openApiDocumentsSuccessErrorsAndNoRequestBody() throws Exception {
        String operation = "$.paths['/api/v1/meal-plans/{planId}/slots/{mealDate}/{mealType}/complete'].post";
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath(operation).exists())
                .andExpect(jsonPath(operation + ".requestBody").doesNotExist())
                .andExpect(jsonPath(operation + ".responses['200'].content['application/json'].schema['$ref']")
                        .value("#/components/schemas/MealPlanResponse"))
                .andExpect(jsonPath(operation + ".responses['400'].content['application/problem+json'].schema['$ref']")
                        .value("#/components/schemas/ProblemDetail"))
                .andExpect(jsonPath(operation + ".responses['404'].content['application/problem+json'].schema['$ref']")
                        .value("#/components/schemas/ProblemDetail"))
                .andExpect(jsonPath(operation + ".responses['409'].content['application/problem+json'].schema['$ref']")
                        .value("#/components/schemas/ProblemDetail"));
    }

    private long ingredient() throws Exception { var result=mvc.perform(post("/api/v1/ingredients").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Complete "+UUID.randomUUID()+"\"}")).andExpect(status().isCreated()).andReturn();return ((Number)JsonPath.read(result.getResponse().getContentAsString(),"$.id")).longValue(); }
    private long recipe(long ingredient) throws Exception { var result=mvc.perform(post("/api/v1/recipes").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Complete Recipe "+UUID.randomUUID()+"\",\"baseServings\":1,\"estimatedMinutes\":10,\"ingredients\":[{\"ingredientId\":"+ingredient+",\"amount\":100,\"unit\":\"g\"}],\"steps\":[\"Cook\"]}")).andExpect(status().isCreated()).andReturn();return ((Number)JsonPath.read(result.getResponse().getContentAsString(),"$.id")).longValue(); }
    private void inventory(long ingredient,String amount) throws Exception { mvc.perform(post("/api/v1/inventory/batches").contentType(MediaType.APPLICATION_JSON).content("{\"ingredientId\":"+ingredient+",\"amount\":"+amount+",\"unit\":\"g\"}")).andExpect(status().isCreated()); }
    private long plan(long recipe,String date,String type) throws Exception { var result=mvc.perform(post("/api/v1/meal-plans").contentType(MediaType.APPLICATION_JSON).content("{\"startDate\":\""+date+"\",\"endDate\":\""+date+"\",\"slots\":[{\"date\":\""+date+"\",\"mealType\":\""+type+"\",\"recipeSelection\":{\"recipeId\":"+recipe+",\"targetServings\":1}}]}")).andExpect(status().isCreated()).andReturn();return ((Number)JsonPath.read(result.getResponse().getContentAsString(),"$.id")).longValue(); }
    private int count(String table) { return jdbc.queryForObject("SELECT count(*) FROM "+table,Integer.class); }
}
