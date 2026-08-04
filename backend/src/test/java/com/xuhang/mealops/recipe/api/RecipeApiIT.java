package com.xuhang.mealops.recipe.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import com.jayway.jsonpath.JsonPath;
import com.xuhang.mealops.support.PostgresTestConfiguration;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
class RecipeApiIT {
    @Autowired WebApplicationContext context;
    MockMvc mockMvc;
    @BeforeEach void setUp() { mockMvc = MockMvcBuilders.webAppContextSetup(context).build(); }

    private long ingredient(String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/ingredients").contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"" + name + "\"}")).andExpect(status().isCreated()).andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    @Test void createsAndGetsCanonicalRecipe() throws Exception {
        String suffix = UUID.randomUUID().toString(); long a=ingredient("Recipe A "+suffix), b=ingredient("Recipe B "+suffix), c=ingredient("Recipe C "+suffix);
        String body="{\"name\":\"番茄炒蛋 "+suffix+"\",\"baseServings\":2,\"estimatedMinutes\":15,\"ingredients\":[{\"ingredientId\":"+a+",\"amount\":0.5,\"unit\":\"kg\"},{\"ingredientId\":"+b+",\"amount\":0.25,\"unit\":\"l\"},{\"ingredientId\":"+c+",\"amount\":2,\"unit\":\"piece\"}],\"steps\":[\"切菜\",\"翻炒\"]}";
        MvcResult created=mockMvc.perform(post("/api/v1/recipes").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated()).andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/v1/recipes/[0-9]+"))).andExpect(jsonPath("$.ingredients[0].amount").value(500)).andExpect(jsonPath("$.ingredients[0].unit").value("g")).andExpect(jsonPath("$.ingredients[1].amount").value(250)).andExpect(jsonPath("$.ingredients[1].unit").value("ml")).andReturn();
        long id=((Number)JsonPath.read(created.getResponse().getContentAsString(),"$.id")).longValue();
        mockMvc.perform(get("/api/v1/recipes/{id}",id)).andExpect(status().isOk()).andExpect(jsonPath("$.ingredients[2].unit").value("piece")).andExpect(jsonPath("$.steps[0].position").value(1));
    }
    @Test void validatesMissingOrderDuplicateUnknownAndNullInputs() throws Exception {
        String s=UUID.randomUUID().toString(); long a=ingredient("Missing A "+s);
        String base="\"name\":\"R\" ,\"baseServings\":1,\"estimatedMinutes\":0,\"steps\":[\"x\"]";
        mockMvc.perform(get("/api/v1/recipes/{id}",Long.MAX_VALUE)).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("RECIPE_NOT_FOUND"));
        mockMvc.perform(post("/api/v1/recipes").contentType(MediaType.APPLICATION_JSON).content("{"+base+",\"ingredients\":[{\"ingredientId\":999999998,\"amount\":1,\"unit\":\"g\"},{\"ingredientId\":999999999,\"amount\":1,\"unit\":\"g\"}]}")).andExpect(status().isNotFound()).andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("999999998")));
        mockMvc.perform(post("/api/v1/recipes").contentType(MediaType.APPLICATION_JSON).content("{"+base+",\"ingredients\":[{\"ingredientId\":"+a+",\"amount\":1,\"unit\":\"g\"},{\"ingredientId\":"+a+",\"amount\":1,\"unit\":\"g\"}]}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mockMvc.perform(post("/api/v1/recipes").contentType(MediaType.APPLICATION_JSON).content("{"+base+",\"ingredients\":[{\"ingredientId\":"+a+",\"amount\":1,\"unit\":\"KG\"}]}")).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/recipes").contentType(MediaType.APPLICATION_JSON).content("{"+base+",\"ingredients\":[null]}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }
    @Test void validatesUnicodeAndOpenApi() throws Exception {
        long a=ingredient("Unicode "+UUID.randomUUID());
        mockMvc.perform(post("/api/v1/recipes").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\\u00a0\\u2003\",\"baseServings\":1,\"estimatedMinutes\":0,\"ingredients\":[{\"ingredientId\":"+a+",\"amount\":1,\"unit\":\"g\"}],\"steps\":[\"\\u00a0\"]}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andExpect(jsonPath("$.paths['/api/v1/recipes'].post.responses['201']").exists()).andExpect(jsonPath("$.paths['/api/v1/recipes/{id}'].get.responses['200']").exists());
    }
}
