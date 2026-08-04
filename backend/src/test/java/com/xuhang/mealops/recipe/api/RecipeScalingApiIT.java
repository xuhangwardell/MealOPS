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
class RecipeScalingApiIT {
    @Autowired WebApplicationContext context;
    MockMvc mockMvc;
    @BeforeEach void setup(){mockMvc=MockMvcBuilders.webAppContextSetup(context).build();}
    private long ingredient(String name) throws Exception {MvcResult r=mockMvc.perform(post("/api/v1/ingredients").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\""+name+"\"}" )).andExpect(status().isCreated()).andReturn();return ((Number)JsonPath.read(r.getResponse().getContentAsString(),"$.id")).longValue();}
    @Test void scalesReadOnlyAndPreservesBaseRecipe() throws Exception {
        String s=UUID.randomUUID().toString();long a=ingredient("ScaleA"+s),b=ingredient("ScaleB"+s);
        String body="{\"name\":\"Scale Recipe"+s+"\",\"baseServings\":2,\"estimatedMinutes\":15,\"ingredients\":[{\"ingredientId\":"+a+",\"amount\":300,\"unit\":\"g\"},{\"ingredientId\":"+b+",\"amount\":1,\"unit\":\"piece\"}],\"steps\":[\"cook\"]}";
        MvcResult created=mockMvc.perform(post("/api/v1/recipes").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated()).andReturn();long id=((Number)JsonPath.read(created.getResponse().getContentAsString(),"$.id")).longValue();
        mockMvc.perform(get("/api/v1/recipes/{id}/scaled",id).param("targetServings","1")).andExpect(status().isOk()).andExpect(jsonPath("$.baseServings").value(2)).andExpect(jsonPath("$.targetServings").value(1)).andExpect(jsonPath("$.estimatedMinutes").value(15)).andExpect(jsonPath("$.ingredients[0].amount").value(150)).andExpect(jsonPath("$.ingredients[1].amount").value(0.5)).andExpect(jsonPath("$.steps[0].position").value(1));
        mockMvc.perform(get("/api/v1/recipes/{id}",id)).andExpect(status().isOk()).andExpect(jsonPath("$.ingredients[0].amount").value(300)).andExpect(jsonPath("$.ingredients[1].amount").value(1));
    }
    @Test void validatesScaleErrorsAndDocumentsEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/recipes/{id}/scaled",Long.MAX_VALUE).param("targetServings","1")).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("RECIPE_NOT_FOUND"));
        mockMvc.perform(get("/api/v1/recipes/{id}/scaled",1).param("targetServings","0")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mockMvc.perform(get("/api/v1/recipes/{id}/scaled",1).param("targetServings","-1"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mockMvc.perform(get("/api/v1/recipes/{id}/scaled",1)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mockMvc.perform(get("/api/v1/recipes/{id}/scaled",1).param("targetServings","abc")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andExpect(jsonPath("$.paths['/api/v1/recipes/{id}/scaled'].get.responses['200']").exists());
    }
}
