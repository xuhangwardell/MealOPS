package com.xuhang.mealops.inventory.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import com.jayway.jsonpath.JsonPath;
import com.xuhang.mealops.support.PostgresTestConfiguration;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
class InventoryConsumptionClosureApiIT {
    @Autowired WebApplicationContext context;
    MockMvc mvc;
    @BeforeEach void setup() { mvc = MockMvcBuilders.webAppContextSetup(context).build(); }
    private long ingredient() throws Exception {
        var r = mvc.perform(post("/api/v1/ingredients").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Closure " + UUID.randomUUID() + "\"}"))
                .andExpect(status().isCreated()).andReturn();
        return ((Number) JsonPath.read(r.getResponse().getContentAsString(), "$.id")).longValue();
    }
    private long batch(long ingredient, String amount, String unit, String expiry) throws Exception {
        var body = "{\"ingredientId\":" + ingredient + ",\"amount\":" + amount + ",\"unit\":\"" + unit
                + "\",\"expiresOn\":\"" + expiry + "\"}";
        var r = mvc.perform(post("/api/v1/inventory/batches").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        return ((Number) JsonPath.read(r.getResponse().getContentAsString(), "$.id")).longValue();
    }
    @Test void singleBatchGetAndExactDepletionAreObservable() throws Exception {
        long i = ingredient(), b = batch(i, "200", "g", "2026-08-01");
        mvc.perform(post("/api/v1/inventory/consumptions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"ingredientId\":" + i + ",\"amount\":200,\"unit\":\"g\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.allocations[0].batchId").value(b));
        mvc.perform(get("/api/v1/inventory/batches/" + b)).andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(0)).andExpect(jsonPath("$.unit").value("g"));
        mvc.perform(get("/api/v1/inventory/batches").param("ingredientId", Long.toString(i)))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }
    @Test void fefoFractionalPieceAndDimensionIsolationAreObservable() throws Exception {
        long i = ingredient(), early = batch(i, "200", "g", "2026-08-01"), late = batch(i, "300", "g", "2026-08-02");
        mvc.perform(post("/api/v1/inventory/consumptions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"ingredientId\":" + i + ",\"amount\":400,\"unit\":\"g\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.allocations[0].batchId").value(early))
                .andExpect(jsonPath("$.allocations[0].amount").value(200))
                .andExpect(jsonPath("$.allocations[1].batchId").value(late))
                .andExpect(jsonPath("$.allocations[1].remainingAfter").value(100));
        long piece = batch(i, "1", "piece", "2026-08-03");
        mvc.perform(post("/api/v1/inventory/consumptions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"ingredientId\":" + i + ",\"amount\":0.5,\"unit\":\"piece\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.amount").value(0.5)).andExpect(jsonPath("$.unit").value("piece"));
        mvc.perform(get("/api/v1/inventory/batches/" + piece)).andExpect(status().isOk()).andExpect(jsonPath("$.amount").value(0.5));
        long g = batch(i, "100", "g", "2026-08-04"), p = batch(i, "2", "piece", "2026-08-05");
        mvc.perform(post("/api/v1/inventory/consumptions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"ingredientId\":" + i + ",\"amount\":201,\"unit\":\"g\"}"))
                .andExpect(status().isConflict()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409)).andExpect(jsonPath("$.title").value("Insufficient inventory"))
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_INVENTORY"));
        mvc.perform(get("/api/v1/inventory/batches/" + g)).andExpect(jsonPath("$.amount").value(100));
        mvc.perform(get("/api/v1/inventory/batches/" + p)).andExpect(jsonPath("$.amount").value(2));
    }
    @Test void invalidAndUnknownConsumptionRequestsUseProblemDetails() throws Exception {
        long i = ingredient(); batch(i, "100", "g", "2026-08-01");
        for (String body : new String[]{"{\"ingredientId\":" + i + ",\"amount\":-1,\"unit\":\"g\"}",
                "{\"ingredientId\":" + i + ",\"amount\":1,\"unit\":\"wat\"}"}) {
            mvc.perform(post("/api/v1/inventory/consumptions").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.title").value("Validation failed"))
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andExpect(jsonPath("$.paths['/api/v1/inventory/consumptions'].post").exists());
    }
}
