package com.xuhang.mealops.planning.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;
import com.xuhang.mealops.support.PostgresTestConfiguration;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
class PlanningPreferencesApiIT {
    @Autowired WebApplicationContext context;
    MockMvc mvc;

    @BeforeEach
    void setup() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        mvc.perform(put("/api/v1/planning-preferences").contentType(MediaType.APPLICATION_JSON)
                .content("{\"defaultServings\":1,\"maxCookingMinutes\":null,\"excludedIngredientIds\":[]}"))
                .andExpect(status().isOk());
    }

    private long ingredient(String name) throws Exception {
        var result = mvc.perform(post("/api/v1/ingredients").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + UUID.randomUUID() + "\"}"))
                .andExpect(status().isCreated()).andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    @Test
    void getsDefaultsThenReplacesAndClearsPreferences() throws Exception {
        mvc.perform(get("/api/v1/planning-preferences")).andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultServings").value(1))
                .andExpect(jsonPath("$.maxCookingMinutes").doesNotExist())
                .andExpect(jsonPath("$.excludedIngredientIds").isArray())
                .andExpect(jsonPath("$.excludedIngredientIds.length()").value(0));
        long a = ingredient("PlanA");
        long b = ingredient("PlanB");
        long c = ingredient("PlanC");
        mvc.perform(put("/api/v1/planning-preferences").contentType(MediaType.APPLICATION_JSON)
                .content("{\"defaultServings\":2,\"maxCookingMinutes\":30,\"excludedIngredientIds\":[" + b + "," + a + "]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.defaultServings").value(2))
                .andExpect(jsonPath("$.maxCookingMinutes").value(30)).andExpect(jsonPath("$.excludedIngredientIds[0]").value((int) Math.min(a, b)))
                .andExpect(jsonPath("$.excludedIngredientIds[1]").value((int) Math.max(a, b)));
        mvc.perform(put("/api/v1/planning-preferences").contentType(MediaType.APPLICATION_JSON)
                .content("{\"defaultServings\":3,\"maxCookingMinutes\":null,\"excludedIngredientIds\":[" + c + "]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.maxCookingMinutes").doesNotExist())
                .andExpect(jsonPath("$.excludedIngredientIds[0]").value((int) c));
        mvc.perform(put("/api/v1/planning-preferences").contentType(MediaType.APPLICATION_JSON)
                .content("{\"defaultServings\":1,\"maxCookingMinutes\":null,\"excludedIngredientIds\":[]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.excludedIngredientIds.length()").value(0));
    }

    @Test
    void rejectsInvalidAndMissingUpdatesWithoutChangingExistingProfile() throws Exception {
        long existing = ingredient("Existing");
        mvc.perform(put("/api/v1/planning-preferences").contentType(MediaType.APPLICATION_JSON)
                .content("{\"defaultServings\":1,\"maxCookingMinutes\":20,\"excludedIngredientIds\":[" + existing + "]}"))
                .andExpect(status().isOk());
        mvc.perform(put("/api/v1/planning-preferences").contentType(MediaType.APPLICATION_JSON)
                .content("{\"defaultServings\":2,\"maxCookingMinutes\":30,\"excludedIngredientIds\":[" + existing + ",999999999]" + "}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("INGREDIENT_NOT_FOUND"));
        mvc.perform(get("/api/v1/planning-preferences")).andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultServings").value(1)).andExpect(jsonPath("$.maxCookingMinutes").value(20))
                .andExpect(jsonPath("$.excludedIngredientIds[0]").value((int) existing));
        mvc.perform(put("/api/v1/planning-preferences").contentType(MediaType.APPLICATION_JSON)
                .content("{\"defaultServings\":0,\"maxCookingMinutes\":0,\"excludedIngredientIds\":[" + existing + "," + existing + "]}"))
                .andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(put("/api/v1/planning-preferences").contentType(MediaType.APPLICATION_JSON)
                .content("{\"defaultServings\":2,\"maxCookingMinutes\":30,\"excludedIngredientIds\":[" + existing + "," + existing + "]}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(put("/api/v1/planning-preferences").contentType(MediaType.APPLICATION_JSON)
                .content("{\"defaultServings\":-1,\"maxCookingMinutes\":-1,\"excludedIngredientIds\":[]}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(put("/api/v1/planning-preferences").contentType(MediaType.APPLICATION_JSON)
                .content("{\"defaultServings\":2,\"maxCookingMinutes\":-1,\"excludedIngredientIds\":[]}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(put("/api/v1/planning-preferences").contentType(MediaType.APPLICATION_JSON)
                .content("{\"maxCookingMinutes\":30,\"excludedIngredientIds\":[]}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(put("/api/v1/planning-preferences").contentType(MediaType.APPLICATION_JSON)
                .content("{\"defaultServings\":1,\"maxCookingMinutes\":30}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(put("/api/v1/planning-preferences").contentType(MediaType.APPLICATION_JSON)
                .content("{\"defaultServings\":1,\"maxCookingMinutes\":null,\"excludedIngredientIds\":[null]}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(put("/api/v1/planning-preferences").contentType(MediaType.APPLICATION_JSON)
                .content("{\"defaultServings\":2,\"maxCookingMinutes\":30,\"excludedIngredientIds\":[0]}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(put("/api/v1/planning-preferences").contentType(MediaType.APPLICATION_JSON)
                .content("{\"defaultServings\":2,\"maxCookingMinutes\":30,\"excludedIngredientIds\":[-1]}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void documentsGetAndPutEndpoints() throws Exception {
        var docs = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(docs).contains("/api/v1/planning-preferences", "\"get\"", "\"put\"", "\"defaultServings\"", "\"maxCookingMinutes\"", "\"excludedIngredientIds\"", "\"maxCookingMinutes\":{\"type\":\"integer\",\"format\":\"int32\",\"minimum\":1");
    }
}
