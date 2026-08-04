package com.xuhang.mealops.requirement.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.math.BigDecimal;
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

@SpringBootTest @Import(PostgresTestConfiguration.class)
class IngredientRequirementApiIT {
    @Autowired WebApplicationContext context; MockMvc mvc;
    @BeforeEach void setup(){mvc=MockMvcBuilders.webAppContextSetup(context).build();}
    private long ingredient(String n)throws Exception{var r=mvc.perform(post("/api/v1/ingredients").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\""+n+UUID.randomUUID()+"\"}")).andExpect(status().isCreated()).andReturn();return ((Number)JsonPath.read(r.getResponse().getContentAsString(),"$.id")).longValue();}
    private long recipe(String name,long id,String amount,String unit,int base)throws Exception{var body="{\"name\":\""+name+"\",\"baseServings\":"+base+",\"estimatedMinutes\":10,\"ingredients\":[{\"ingredientId\":"+id+",\"amount\":"+amount+",\"unit\":\""+unit+"\"}],\"steps\":[\"cook\"]}";var r=mvc.perform(post("/api/v1/recipes").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated()).andReturn();return ((Number)JsonPath.read(r.getResponse().getContentAsString(),"$.id")).longValue();}
    @Test void aggregatesSingleAndMultipleRecipesDeterministically()throws Exception{
        long x=ingredient("ReqX"), y=ingredient("ReqY"), a=recipe("A",x,"300","g",2), b=recipe("B",x,"200","g",1), c=recipe("C",x,"2","piece",1);
        mvc.perform(post("/api/v1/ingredient-requirements").contentType(MediaType.APPLICATION_JSON).content("{\"recipes\":[{\"recipeId\":"+a+",\"targetServings\":4}]}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.requirements[0].ingredientId").value(x)).andExpect(jsonPath("$.requirements[0].amount").value(600)).andExpect(jsonPath("$.requirements[0].unit").value("g"));
        mvc.perform(post("/api/v1/ingredient-requirements").contentType(MediaType.APPLICATION_JSON).content("{\"recipes\":[{\"recipeId\":"+b+",\"targetServings\":1},{\"recipeId\":"+a+",\"targetServings\":2},{\"recipeId\":"+c+",\"targetServings\":1}]}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.requirements.length()").value(2)).andExpect(jsonPath("$.requirements[0].unit").value("g")).andExpect(jsonPath("$.requirements[0].amount").value(500)).andExpect(jsonPath("$.requirements[1].unit").value("piece"));
    }
    @Test void duplicateSelectionsScaleIndependentlyAndMissingIs404()throws Exception{
        long x=ingredient("DupX"), r=recipe("D",x,"100","g",3);
        var single=mvc.perform(post("/api/v1/ingredient-requirements").contentType(MediaType.APPLICATION_JSON).content("{\"recipes\":[{\"recipeId\":"+r+",\"targetServings\":1}]}"))
            .andExpect(status().isOk()).andReturn();
        BigDecimal contribution=JsonPath.read(single.getResponse().getContentAsString(),"$.requirements[0].amount");
        var duplicate=mvc.perform(post("/api/v1/ingredient-requirements").contentType(MediaType.APPLICATION_JSON).content("{\"recipes\":[{\"recipeId\":"+r+",\"targetServings\":1},{\"recipeId\":"+r+",\"targetServings\":1}]}"))
            .andExpect(status().isOk()).andReturn();
        BigDecimal actual=JsonPath.read(duplicate.getResponse().getContentAsString(),"$.requirements[0].amount");
        assertThat(actual).isEqualByComparingTo(contribution.add(contribution));
        mvc.perform(post("/api/v1/ingredient-requirements").contentType(MediaType.APPLICATION_JSON).content("{\"recipes\":[{\"recipeId\":9223372036854775807,\"targetServings\":1}]}"))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("RECIPE_NOT_FOUND"));
    }
    @Test void validatesRequestsAndDocumentsEndpoint()throws Exception{
        String[] invalid = {"{}","{\"recipes\":[]}","{\"recipes\":[null]}","{\"recipes\":[{\"recipeId\":0,\"targetServings\":1}]}","{\"recipes\":[{\"recipeId\":1,\"targetServings\":0}]}","{\"recipes\":[{\"recipeId\":1,\"targetServings\":-1}]}"};
        for(String body: invalid)
            mvc.perform(post("/api/v1/ingredient-requirements").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andExpect(jsonPath("$.paths['/api/v1/ingredient-requirements'].post.responses['200']").exists());
    }
}
