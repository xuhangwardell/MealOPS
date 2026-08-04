package com.xuhang.mealops.shopping.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;
import com.xuhang.mealops.support.PostgresTestConfiguration;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
class ShoppingListPreviewClosureApiIT {
    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    MockMvc mvc;

    @BeforeEach void setup() { mvc = MockMvcBuilders.webAppContextSetup(context).build(); }

    private long ingredient(String name) throws Exception {
        var result = mvc.perform(post("/api/v1/ingredients").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + UUID.randomUUID() + "\"}"))
                .andExpect(status().isCreated()).andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private long recipe(long ingredientId, String amount, String unit) throws Exception {
        var result = mvc.perform(post("/api/v1/recipes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"S" + UUID.randomUUID() + "\",\"baseServings\":1,\"estimatedMinutes\":1,\"ingredients\":[{\"ingredientId\":" + ingredientId + ",\"amount\":" + amount + ",\"unit\":\"" + unit + "\"}],\"steps\":[\"cook\"]}"))
                .andExpect(status().isCreated()).andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private void batch(long ingredientId, String amount, String unit, String expiry) throws Exception {
        mvc.perform(post("/api/v1/inventory/batches").contentType(MediaType.APPLICATION_JSON)
                .content("{\"ingredientId\":" + ingredientId + ",\"amount\":" + amount + ",\"unit\":\"" + unit + "\",\"expiresOn\":\"" + expiry + "\"}"))
                .andExpect(status().isCreated());
    }

    private ResultActions preview(long recipeId) throws Exception {
        return mvc.perform(post("/api/v1/shopping-list-previews").contentType(MediaType.APPLICATION_JSON)
                .content("{\"recipes\":[{\"recipeId\":" + recipeId + ",\"targetServings\":1}]}") );
    }

    @Test
    void coversNoInventoryMultipleBatchesFullOverstockAndDimension() throws Exception {
        long ingredientId = ingredient("NoInv");
        long recipeId = recipe(ingredientId, "500", "g");
        preview(recipeId).andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].availableAmount").value(0)).andExpect(jsonPath("$.items[0].shortageAmount").value(500));
        batch(ingredientId, "100", "g", "2020-01-01");
        batch(ingredientId, "200", "g", "2020-01-02");
        preview(recipeId).andExpect(jsonPath("$.items[0].availableAmount").value(300)).andExpect(jsonPath("$.items[0].shortageAmount").value(200));
        batch(ingredientId, "500", "g", "2020-01-03");
        preview(recipeId).andExpect(jsonPath("$.items.length()").value(0));
        long dimensionIngredient = ingredient("Dim");
        long dimensionRecipe = recipe(dimensionIngredient, "500", "g");
        batch(dimensionIngredient, "5", "piece", "2020-01-04");
        preview(dimensionRecipe).andExpect(jsonPath("$.items[0].availableAmount").value(0)).andExpect(jsonPath("$.items[0].shortageAmount").value(500));
    }

    @Test
    void provesDepletedReadOnlyMissingRecipeAndValidation() throws Exception {
        long ingredientId = ingredient("Read");
        long recipeId = recipe(ingredientId, "500", "g");
        batch(ingredientId, "100", "g", "2020-01-01");
        long batchId = jdbc.queryForObject("select id from inventory_batch where ingredient_id=?", Long.class, ingredientId);
        jdbc.update("insert into inventory_batch(ingredient_id,remaining_amount,unit_code,expires_on) values(?,?,?,?)", ingredientId, 0, "g", LocalDate.of(2020, 1, 1));
        long version = jdbc.queryForObject("select version from inventory_batch where id=?", Long.class, batchId);
        int transactionCount = jdbc.queryForObject("select count(*) from inventory_transaction", Integer.class);
        int allocationCount = jdbc.queryForObject("select count(*) from inventory_transaction_allocation", Integer.class);
        preview(recipeId).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].availableAmount").value(100)).andExpect(jsonPath("$.items[0].shortageAmount").value(400));
        assertThat(jdbc.queryForObject("select remaining_amount from inventory_batch where id=?", java.math.BigDecimal.class, batchId)).isEqualByComparingTo("100");
        assertThat(jdbc.queryForObject("select version from inventory_batch where id=?", Long.class, batchId)).isEqualTo(version);
        assertThat(jdbc.queryForObject("select count(*) from inventory_transaction", Integer.class)).isEqualTo(transactionCount);
        assertThat(jdbc.queryForObject("select count(*) from inventory_transaction_allocation", Integer.class)).isEqualTo(allocationCount);
        preview(Long.MAX_VALUE).andExpect(status().isNotFound()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)).andExpect(jsonPath("$.code").value("RECIPE_NOT_FOUND"));
        mvc.perform(post("/api/v1/shopping-list-previews").contentType(MediaType.APPLICATION_JSON).content("{\"recipes\":[]}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }
}
