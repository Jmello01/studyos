package com.studyos.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.studyos.dto.AreaRequest;
import com.studyos.dto.AreaResponse;
import com.studyos.exception.ConflictException;
import com.studyos.exception.ResourceNotFoundException;
import com.studyos.service.AreaService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AreaController.class)
class AreaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AreaService service;

    private AreaResponse area(long id, String name) {
        return new AreaResponse(id, name, null, null, null, 1, false);
    }

    @Test
    void listaAreas() throws Exception {
        when(service.list(false)).thenReturn(List.of(area(1, "FACULDADE"), area(2, "CLOUD")));

        mockMvc.perform(get("/api/areas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("FACULDADE"));
    }

    @Test
    void listaAreasIncluindoArquivadas() throws Exception {
        when(service.list(true)).thenReturn(List.of(area(1, "FACULDADE")));

        mockMvc.perform(get("/api/areas").param("includeArchived", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void criaAreaDevolve201ComLocation() throws Exception {
        when(service.create(any(AreaRequest.class))).thenReturn(area(10, "Estudos"));

        mockMvc.perform(post("/api/areas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Estudos", "color": "#112233"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/areas/10"))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Estudos"));
    }

    @Test
    void nomeEmBrancoDevolve400ComErroPorCampo() throws Exception {
        mockMvc.perform(post("/api/areas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.name").value("é obrigatório"));
    }

    @Test
    void corForaDoFormatoDevolve400() throws Exception {
        mockMvc.perform(post("/api/areas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Estudos", "color": "vermelho"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.color").value("deve estar no formato #RRGGBB"));
    }

    @Test
    void jsonMalFormadoDevolve400() throws Exception {
        mockMvc.perform(post("/api/areas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ isto não é json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nomeDuplicadoDevolve409() throws Exception {
        when(service.create(any(AreaRequest.class))).thenThrow(new ConflictException("Já existe uma área com o nome 'CLOUD'"));

        mockMvc.perform(post("/api/areas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "CLOUD"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Já existe uma área com o nome 'CLOUD'"));
    }

    @Test
    void areaInexistenteDevolve404() throws Exception {
        when(service.get(99L)).thenThrow(new ResourceNotFoundException("Área 99 não encontrada"));

        mockMvc.perform(get("/api/areas/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Área 99 não encontrada"));
    }

    @Test
    void idNaoNumericoDevolve400() throws Exception {
        mockMvc.perform(get("/api/areas/abc")).andExpect(status().isBadRequest());
    }

    @Test
    void arquivaArea() throws Exception {
        when(service.archive(eq(1L))).thenReturn(new AreaResponse(1L, "CLOUD", null, null, null, 6, true));

        mockMvc.perform(patch("/api/areas/1/archive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));
    }
}