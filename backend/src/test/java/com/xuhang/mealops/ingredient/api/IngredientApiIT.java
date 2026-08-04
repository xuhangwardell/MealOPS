package com.xuhang.mealops.ingredient.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import java.util.UUID;

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
class IngredientApiIT {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @org.junit.jupiter.api.BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void createsGetsAndRenamesIngredient() throws Exception {
        String name = "API Egg " + UUID.randomUUID();
        MvcResult created = mockMvc.perform(post("/api/v1/ingredients")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/v1/ingredients/[0-9]+")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value(name))
                .andReturn();

        Number idValue = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        Long id = idValue.longValue();
        mockMvc.perform(get("/api/v1/ingredients/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value(name));

        String renamed = "Renamed Egg " + UUID.randomUUID();
        mockMvc.perform(put("/api/v1/ingredients/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + renamed + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value(renamed));
    }

    @Test
    void returnsProblemDetailsForMissingDuplicateAndBlankRequests() throws Exception {
        mockMvc.perform(get("/api/v1/ingredients/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Ingredient not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("INGREDIENT_NOT_FOUND"));

        String suffix = UUID.randomUUID().toString();
        String firstName = "Egg-" + suffix;
        mockMvc.perform(post("/api/v1/ingredients")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + firstName + "\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/ingredients")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"ＥＧＧ-" + suffix + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INGREDIENT_NAME_ALREADY_EXISTS"));

        mockMvc.perform(post("/api/v1/ingredients")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void acceptsOneHundredSupplementaryCodePoints() throws Exception {
        String name = "😀".repeat(100);

        mockMvc.perform(post("/api/v1/ingredients")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void mapsDomainUnicodeWhitespaceValidationToProblemDetail() throws Exception {
        mockMvc.perform(post("/api/v1/ingredients")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\u00a0\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void renameDuplicateReturnsConflictAndKeepsOriginalRecord() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String egg = "Egg-" + suffix;
        String milk = "Milk-" + suffix;

        MvcResult createdMilk = mockMvc.perform(post("/api/v1/ingredients")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + milk + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        mockMvc.perform(post("/api/v1/ingredients")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + egg + "\"}"))
                .andExpect(status().isCreated());
        Long milkId = ((Number) JsonPath.read(createdMilk.getResponse().getContentAsString(), "$.id")).longValue();

        mockMvc.perform(put("/api/v1/ingredients/{id}", milkId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"ＥＧＧ-" + suffix + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INGREDIENT_NAME_ALREADY_EXISTS"));

        mockMvc.perform(get("/api/v1/ingredients/{id}", milkId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(milk));
    }

    @Test
    void exposesIngredientOperationsInOpenApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/ingredients'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/ingredients'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/ingredients/{id}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/ingredients/{id}'].put").exists());
    }
}
