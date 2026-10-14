package com.studyos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AreaApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void fluxoCompletoDeUmaArea() throws Exception {
        mockMvc.perform(get("/api/areas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("FACULDADE"));

        mockMvc.perform(post("/api/areas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Idiomas Extras", "color": "#AABBCC"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.position").value(10));

        mockMvc.perform(post("/api/areas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "idiomas extras"}
                                """))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/areas/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Idiomas", "description": "Espanhol e francês"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Idiomas"));

        mockMvc.perform(put("/api/areas/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Idiomas", "description": "Espanhol, francês e italiano"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value(10));

        mockMvc.perform(get("/api/areas/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Idiomas"))
                .andExpect(jsonPath("$.description").value("Espanhol, francês e italiano"));

        mockMvc.perform(patch("/api/areas/10/archive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));

        mockMvc.perform(get("/api/areas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(9));

        mockMvc.perform(get("/api/areas").param("includeArchived", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10));

        mockMvc.perform(get("/api/areas/9999")).andExpect(status().isNotFound());
    }
}