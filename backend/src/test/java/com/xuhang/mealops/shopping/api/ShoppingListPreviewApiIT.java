package com.xuhang.mealops.shopping.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;
import com.xuhang.mealops.support.PostgresTestConfiguration;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
class ShoppingListPreviewApiIT {
    @Autowired WebApplicationContext context;
    MockMvc mvc;

    @BeforeEach
    void setup() { mvc = MockMvcBuilders.webAppContextSetup(context).build(); }

    private long ingredient(String suffix) throws Exception {
        var result = mvc.perform(post("/api/v1/ingredients").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Shop" + suffix + UUID.randomUUID() + "\"}"))
                .andExpect(status().isCreated()).andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private long recipe(long ingredientId, String amount, String unit) throws Exception {
        var result = mvc.perform(post("/api/v1/recipes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"ShopR" + UUID.randomUUID() + "\",\"baseServings\":1,\"estimatedMinutes\":1,\"ingredients\":[{\"ingredientId\":"
                        + ingredientId + ",\"amount\":" + amount + ",\"unit\":\"" + unit + "\"}],\"steps\":[\"cook\"]}"))
                .andExpect(status().isCreated()).andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private void batch(long ingredientId, String amount, String unit, String expiresOn) throws Exception {
        var expiry = expiresOn == null ? "" : ",\"expiresOn\":\"" + expiresOn + "\"";
        mvc.perform(post("/api/v1/inventory/batches").contentType(MediaType.APPLICATION_JSON)
                .content("{\"ingredientId\":" + ingredientId + ",\"amount\":" + amount + ",\"unit\":\"" + unit + "\"" + expiry + "}"))
                .andExpect(status().isCreated());
    }

    private ResultActions preview(String recipes) throws Exception {
        return mvc.perform(post("/api/v1/shopping-list-previews").contentType(MediaType.APPLICATION_JSON)
                .content("{\"recipes\":[" + recipes + "]}"));
    }

    @Test
    void calculatesShortageAndDocumentsEndpoint() throws Exception {
        long ingredientId = ingredient("Basic");
        long recipeId = recipe(ingredientId, "800", "g");
        batch(ingredientId, "300", "g", null);
        preview("{\"recipeId\":" + recipeId + ",\"targetServings\":1}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].requiredAmount").value(800))
                .andExpect(jsonPath("$.items[0].availableAmount").value(300))
                .andExpect(jsonPath("$.items[0].shortageAmount").value(500)).andExpect(jsonPath("$.items[0].unit").value("g"));

        var docs = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/v3/api-docs"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(docs).contains("/api/v1/shopping-list-previews", "\"200\"", "\"items\"",
                "\"ingredientId\"", "\"requiredAmount\"", "\"availableAmount\"", "\"shortageAmount\"", "\"unit\"");
    }

    @Test
    void expiredInventoryParticipatesInAccounting() throws Exception {
        long ingredientId = ingredient("Expired");
        long recipeId = recipe(ingredientId, "500", "g");
        batch(ingredientId, "300", "g", "2020-01-01");
        preview("{\"recipeId\":" + recipeId + ",\"targetServings\":1}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].requiredAmount").value(500))
                .andExpect(jsonPath("$.items[0].availableAmount").value(300))
                .andExpect(jsonPath("$.items[0].shortageAmount").value(200)).andExpect(jsonPath("$.items[0].unit").value("g"));
    }

    @Test
    void duplicateRecipeSelectionsContributeIndependently() throws Exception {
        long ingredientId = ingredient("Duplicate");
        long recipeId = recipe(ingredientId, "100", "g");
        batch(ingredientId, "50", "g", null);
        preview("{\"recipeId\":" + recipeId + ",\"targetServings\":1},{\"recipeId\":" + recipeId + ",\"targetServings\":1}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].requiredAmount").value(200))
                .andExpect(jsonPath("$.items[0].availableAmount").value(50))
                .andExpect(jsonPath("$.items[0].shortageAmount").value(150));
    }

    @Test
    void responseItemsHaveDeterministicIngredientOrdering() throws Exception {
        long highIngredient = ingredient("High");
        long lowIngredient = ingredient("Low");
        long highRecipe = recipe(highIngredient, "100", "g");
        long lowRecipe = recipe(lowIngredient, "100", "g");
        var result = preview("{\"recipeId\":" + highRecipe + ",\"targetServings\":1},{\"recipeId\":" + lowRecipe + ",\"targetServings\":1}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(2)).andReturn();
        var body = result.getResponse().getContentAsString();
        long first = ((Number) JsonPath.read(body, "$.items[0].ingredientId")).longValue();
        long second = ((Number) JsonPath.read(body, "$.items[1].ingredientId")).longValue();
        assertThat(first).isLessThan(second);
        assertThat(first).isIn(lowIngredient, highIngredient);
        assertThat(second).isIn(lowIngredient, highIngredient);
    }
}
