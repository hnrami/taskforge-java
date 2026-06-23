package com.taskforge.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class WorkflowApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ------------------------------------------------------------------
    // POST /api/workflows — success
    // ------------------------------------------------------------------

    @Test
    void createWorkflow_validRequest_returns201() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "test-workflow",
                "tasks", List.of(
                        Map.of("id", "A", "type", "notification", "dependsOn", List.of()),
                        Map.of("id", "B", "type", "notification", "dependsOn", List.of("A"))
                )
        ));

        mockMvc.perform(post("/api/workflows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("test-workflow"))
                .andExpect(jsonPath("$.tasks", hasSize(2)));
    }

    // ------------------------------------------------------------------
    // GET /api/workflows/{id}
    // ------------------------------------------------------------------

    @Test
    void getWorkflow_existingId_returns200() throws Exception {
        // Create first
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "get-test-workflow",
                "tasks", List.of(Map.of("id", "A", "type", "notification"))
        ));

        MvcResult created = mockMvc.perform(post("/api/workflows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        String id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        // Fetch
        mockMvc.perform(get("/api/workflows/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("get-test-workflow"));
    }

    @Test
    void getWorkflow_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/workflows/non-existent-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("non-existent-id")));
    }

    // ------------------------------------------------------------------
    // Validation — cycle detection
    // ------------------------------------------------------------------

    @Test
    void createWorkflow_cycleDetected_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "cycle-workflow",
                "tasks", List.of(
                        Map.of("id", "A", "type", "notification", "dependsOn", List.of("C")),
                        Map.of("id", "B", "type", "notification", "dependsOn", List.of("A")),
                        Map.of("id", "C", "type", "notification", "dependsOn", List.of("B"))
                )
        ));

        mockMvc.perform(post("/api/workflows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Cycle detected")));
    }

    // ------------------------------------------------------------------
    // Validation — missing dependency
    // ------------------------------------------------------------------

    @Test
    void createWorkflow_missingDependency_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "missing-dep-workflow",
                "tasks", List.of(
                        Map.of("id", "B", "type", "notification", "dependsOn", List.of("A"))
                        // A does not exist
                )
        ));

        mockMvc.perform(post("/api/workflows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Unknown dependency")));
    }

    // ------------------------------------------------------------------
    // Validation — unknown task type
    // ------------------------------------------------------------------

    @Test
    void createWorkflow_unknownTaskType_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "unknown-type-workflow",
                "tasks", List.of(
                        Map.of("id", "A", "type", "totally-unknown-handler")
                )
        ));

        mockMvc.perform(post("/api/workflows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Unknown task type")));
    }
}
