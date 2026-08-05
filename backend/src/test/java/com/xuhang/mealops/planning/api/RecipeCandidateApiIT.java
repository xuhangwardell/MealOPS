package com.xuhang.mealops.planning.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import com.jayway.jsonpath.JsonPath;
import com.xuhang.mealops.support.PostgresTestConfiguration;

@SpringBootTest @Import(PostgresTestConfiguration.class) @Transactional
class RecipeCandidateApiIT {
    @Autowired WebApplicationContext context; @Autowired JdbcTemplate jdbc; MockMvc mvc;
    @BeforeEach void setup() { mvc = MockMvcBuilders.webAppContextSetup(context).build(); jdbc.update("DELETE FROM meal_plan"); jdbc.update("DELETE FROM recipe_ingredient"); jdbc.update("DELETE FROM recipe_step"); jdbc.update("DELETE FROM recipe"); }

    @Test void filtersByPersistedPreferencesAndRespondsInIdOrder() throws Exception {
        long normal = ingredient("Normal"); long excluded = ingredient("Excluded");
        long r1 = recipe("Eligible",20,normal); long r2 = recipe("Slow",40,normal); long r3 = recipe("Excluded",20,excluded);
        preferences(1, "30", "["+excluded+"]");
        mvc.perform(get("/api/v1/recipe-candidates")).andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].recipeId").value((int)r1))
                .andExpect(jsonPath("$.items[0].name").exists()).andExpect(jsonPath("$.items[0].baseServings").value(4))
                .andExpect(jsonPath("$.items[0].estimatedMinutes").value(20));
        preferences(7, "null", "[]");
        mvc.perform(get("/api/v1/recipe-candidates")).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].recipeId").value((int) r1))
                .andExpect(jsonPath("$.items[1].recipeId").value((int) r2))
                .andExpect(jsonPath("$.items[2].recipeId").value((int) r3));
        preferences(99, "10", "[]");
        mvc.perform(get("/api/v1/recipe-candidates")).andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test void keepsInclusiveTimeBoundaryAndReturnsEmptyLegitimately() throws Exception {
        mvc.perform(get("/api/v1/recipe-candidates")).andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
        long ingredient = ingredient("Boundary"); long r29=recipe("R29",29,ingredient); long r30=recipe("R30",30,ingredient); recipe("R31",31,ingredient);
        preferences(1,"30","[]");
        mvc.perform(get("/api/v1/recipe-candidates")).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].recipeId").value((int) r29))
                .andExpect(jsonPath("$.items[1].recipeId").value((int) r30))
                .andExpect(jsonPath("$.items.length()").value(2));
    }

    @Test void openApiContainsDerivedGetWithoutRankingFields() throws Exception {
        var result=mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/recipe-candidates'].get.responses['200']").exists()).andReturn();
        String json=result.getResponse().getContentAsString();
        String responseRef=JsonPath.read(json,"$.paths['/api/v1/recipe-candidates'].get.responses['200'].content['application/json'].schema['$ref']");
        String responseName=responseRef.substring(responseRef.lastIndexOf('/')+1);
        String candidateRef=JsonPath.read(json,"$.components.schemas['"+responseName+"'].properties.items.items['$ref']");
        String candidateName=candidateRef.substring(candidateRef.lastIndexOf('/')+1);
        java.util.Map<String,Object> properties=JsonPath.read(json,"$.components.schemas['"+candidateName+"'].properties");
        org.assertj.core.api.Assertions.assertThat(properties.keySet()).containsExactlyInAnyOrder("recipeId","name","baseServings","estimatedMinutes");
    }

    private long ingredient(String prefix) throws Exception {var r=mvc.perform(post("/api/v1/ingredients").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\""+prefix+UUID.randomUUID()+"\"}")).andExpect(status().isCreated()).andReturn();return ((Number)JsonPath.read(r.getResponse().getContentAsString(),"$.id")).longValue();}
    private long recipe(String prefix,int minutes,long ingredient) throws Exception {String body="{\"name\":\""+prefix+UUID.randomUUID()+"\",\"baseServings\":4,\"estimatedMinutes\":"+minutes+",\"ingredients\":[{\"ingredientId\":"+ingredient+",\"amount\":1,\"unit\":\"g\"}],\"steps\":[\"Cook\"]}";var r=mvc.perform(post("/api/v1/recipes").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated()).andReturn();return ((Number)JsonPath.read(r.getResponse().getContentAsString(),"$.id")).longValue();}
    private void preferences(int servings,String max,String exclusions) throws Exception {mvc.perform(put("/api/v1/planning-preferences").contentType(MediaType.APPLICATION_JSON).content("{\"defaultServings\":"+servings+",\"maxCookingMinutes\":"+max+",\"excludedIngredientIds\":"+exclusions+"}")).andExpect(status().isOk());}
}
