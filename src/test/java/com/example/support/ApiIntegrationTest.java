package com.example.support;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest @AutoConfigureMockMvc @Testcontainers
class ApiIntegrationTest {
    @Container static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void props(DynamicPropertyRegistry props) {
        props.add("spring.datasource.url", postgres::getJdbcUrl);
        props.add("spring.datasource.username", postgres::getUsername);
        props.add("spring.datasource.password", postgres::getPassword);
    }
    @Autowired MockMvc mvc;
    private static final String CREATE = "{\"title\":\"Printer broken\",\"categoryId\":2,\"priority\":\"HIGH\"}";
    private String create() throws Exception {
        String json = mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content(CREATE))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return new com.fasterxml.jackson.databind.ObjectMapper().readTree(json).get("id").asText();
    }
    @Test void migrationsLoadCategories() throws Exception {
        mvc.perform(get("/api/categories")).andExpect(status().isOk()).andExpect(jsonPath("$.length()", is(3)));
    }
    @Test void missingTicketIs404() throws Exception {
        mvc.perform(get("/api/tickets/999999999")).andExpect(status().isNotFound()).andExpect(jsonPath("$.status", is(404)));
    }
    @Test void validationRejectsBlankTitle() throws Exception {
        mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"\",\"categoryId\":1}"))
            .andExpect(status().isBadRequest());
    }
    @Test void createAndReadPersistsTicket() throws Exception {
        String id = create();
        mvc.perform(get("/api/tickets/" + id)).andExpect(status().isOk())
            .andExpect(jsonPath("$.title", is("Printer broken"))).andExpect(jsonPath("$.status", is("NEW")));
    }
    @Test void filteringReturnsHighTickets() throws Exception {
        create();
        mvc.perform(get("/api/tickets").param("priority", "HIGH").param("size", "5"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()", greaterThanOrEqualTo(1)));
    }
    @Test void historyPersistsWithStatus() throws Exception {
        String id = create();
        mvc.perform(patch("/api/tickets/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"IN_PROGRESS\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status", is("IN_PROGRESS")));
        mvc.perform(get("/api/tickets/" + id + "/history"))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].newStatus", is("IN_PROGRESS")));
    }
    @Test void lifecycleEndsWithResolvedAndKotlinReport() throws Exception {
        String id = create();
        mvc.perform(patch("/api/tickets/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"IN_PROGRESS\"}")).andExpect(status().isOk());
        mvc.perform(patch("/api/tickets/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"RESOLVED\"}")).andExpect(status().isOk());
        mvc.perform(get("/api/reports/summary").param("from", "2020-01-01T00:00:00Z").param("to", "2030-01-01T00:00:00Z"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.byStatus.RESOLVED", greaterThanOrEqualTo(1)));
    }
    @Test void forbiddenOperationKeepsStateAndHistory() throws Exception {
        String id = create();
        mvc.perform(patch("/api/tickets/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"RESOLVED\"}")).andExpect(status().isConflict());
        mvc.perform(get("/api/tickets/" + id)).andExpect(jsonPath("$.status", is("NEW")));
        mvc.perform(get("/api/tickets/" + id + "/history")).andExpect(jsonPath("$.length()", is(0)));
    }

    @Test
    void ticketWithoutDeadlineIsNotOverdue() throws Exception {
        String id = create();

        mvc.perform(get("/api/tickets")
                        .param("overdue", "false")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id",
                        hasItem(Integer.parseInt(id))));
    }
}
