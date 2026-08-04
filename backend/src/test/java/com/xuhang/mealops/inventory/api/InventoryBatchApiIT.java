package com.xuhang.mealops.inventory.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
class InventoryBatchApiIT {
    @Autowired WebApplicationContext context; MockMvc mockMvc;
    @BeforeEach void setup() { mockMvc = MockMvcBuilders.webAppContextSetup(context).build(); }
    private long ingredient(String name) throws Exception { MvcResult r = mockMvc.perform(post("/api/v1/ingredients").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"" + name + "\"}" )).andExpect(status().isCreated()).andReturn(); return ((Number) JsonPath.read(r.getResponse().getContentAsString(), "$.id")).longValue(); }
    @Test void createsGetsListsAndValidatesInventoryBatches() throws Exception {
        String s = UUID.randomUUID().toString(); long ingredient = ingredient("Inventory API " + s);
        MvcResult created = mockMvc.perform(post("/api/v1/inventory/batches").contentType(MediaType.APPLICATION_JSON).content("{\"ingredientId\":"+ingredient+",\"amount\":0.5,\"unit\":\"kg\",\"expiresOn\":\"2026-08-10\"}" )).andExpect(status().isCreated()).andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/v1/inventory/batches/"))).andExpect(jsonPath("$.amount").value(500)).andExpect(jsonPath("$.unit").value("g")).andReturn();
        long id = ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.id")).longValue();
        mockMvc.perform(post("/api/v1/inventory/batches").contentType(MediaType.APPLICATION_JSON).content("{\"ingredientId\":"+ingredient+",\"amount\":6,\"unit\":\"piece\"}" )).andExpect(status().isCreated()).andExpect(jsonPath("$.unit").value("piece"));
        mockMvc.perform(get("/api/v1/inventory/batches/{id}", id)).andExpect(status().isOk()).andExpect(jsonPath("$.expiresOn").value("2026-08-10"));
        mockMvc.perform(get("/api/v1/inventory/batches")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));
        mockMvc.perform(get("/api/v1/inventory/batches").param("ingredientId", String.valueOf(ingredient))).andExpect(status().isOk()).andExpect(jsonPath("$[0].ingredientId").value(ingredient));
        mockMvc.perform(get("/api/v1/inventory/batches").param("ingredientId", "999999999")).andExpect(status().isOk()).andExpect(content().json("[]"));
        mockMvc.perform(get("/api/v1/inventory/batches/{id}", Long.MAX_VALUE)).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("INVENTORY_BATCH_NOT_FOUND"));
        mockMvc.perform(post("/api/v1/inventory/batches").contentType(MediaType.APPLICATION_JSON).content("{\"ingredientId\":"+ingredient+",\"amount\":0,\"unit\":\"g\"}" )).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mockMvc.perform(post("/api/v1/inventory/batches").contentType(MediaType.APPLICATION_JSON).content("{\"ingredientId\":999999999,\"amount\":1,\"unit\":\"g\"}" )).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("INGREDIENT_NOT_FOUND"));
        mockMvc.perform(post("/api/v1/inventory/batches").contentType(MediaType.APPLICATION_JSON).content("{\"ingredientId\":"+ingredient+",\"amount\":1,\"unit\":\"g\",\"expiresOn\":\"not-a-date\"}" )).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andExpect(jsonPath("$.paths['/api/v1/inventory/batches'].post.responses['201']").exists()).andExpect(jsonPath("$.paths['/api/v1/inventory/batches'].get.responses['200']").exists()).andExpect(jsonPath("$.paths['/api/v1/inventory/batches/{id}'].get.responses['200']").exists());
    }

    @Test void validatesConversionsErrorsAndIsolatesDeterministicOrdering() throws Exception {
        long ingredient = ingredient("Ordering " + UUID.randomUUID());
        MvcResult early = mockMvc.perform(post("/api/v1/inventory/batches").contentType(MediaType.APPLICATION_JSON).content("{\"ingredientId\":"+ingredient+",\"amount\":1,\"unit\":\"g\",\"expiresOn\":\"2026-08-01\"}" )).andExpect(status().isCreated()).andReturn();
        long earlyId = ((Number) JsonPath.read(early.getResponse().getContentAsString(), "$.id")).longValue();
        MvcResult same = mockMvc.perform(post("/api/v1/inventory/batches").contentType(MediaType.APPLICATION_JSON).content("{\"ingredientId\":"+ingredient+",\"amount\":2,\"unit\":\"g\",\"expiresOn\":\"2026-08-01\"}" )).andExpect(status().isCreated()).andReturn();
        long sameId = ((Number) JsonPath.read(same.getResponse().getContentAsString(), "$.id")).longValue();
        mockMvc.perform(post("/api/v1/inventory/batches").contentType(MediaType.APPLICATION_JSON).content("{\"ingredientId\":"+ingredient+",\"amount\":3,\"unit\":\"g\",\"expiresOn\":\"2026-08-02\"}" )).andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/inventory/batches").contentType(MediaType.APPLICATION_JSON).content("{\"ingredientId\":"+ingredient+",\"amount\":0.25,\"unit\":\"l\"}" )).andExpect(status().isCreated()).andExpect(jsonPath("$.amount").value(250)).andExpect(jsonPath("$.unit").value("ml")).andExpect(jsonPath("$.expiresOn").value(org.hamcrest.Matchers.nullValue()));
        mockMvc.perform(get("/api/v1/inventory/batches").param("ingredientId", String.valueOf(ingredient))).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(earlyId)).andExpect(jsonPath("$[1].id").value(sameId));
        mockMvc.perform(post("/api/v1/inventory/batches").contentType(MediaType.APPLICATION_JSON).content("{\"ingredientId\":"+ingredient+",\"amount\":1,\"unit\":\"g\",\"expiresOn\":\"2020-01-01\"}" )).andExpect(status().isCreated()).andExpect(jsonPath("$.expiresOn").value("2020-01-01"));
        var bad = mockMvc.perform(post("/api/v1/inventory/batches").contentType(MediaType.APPLICATION_JSON).content("{\"ingredientId\":"+ingredient+",\"amount\":-1,\"unit\":\"g\"}" )).andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)).andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.title").value("Validation failed")).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        bad.andReturn();
        mockMvc.perform(post("/api/v1/inventory/batches").contentType(MediaType.APPLICATION_JSON).content("{\"ingredientId\":"+ingredient+",\"amount\":1,\"unit\":\"abc\"}" )).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mockMvc.perform(post("/api/v1/inventory/batches").contentType(MediaType.APPLICATION_JSON).content("{\"ingredientId\":"+ingredient+",\"amount\":1," )).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }
}
