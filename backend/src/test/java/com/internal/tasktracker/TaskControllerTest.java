package com.internal.tasktracker;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Runs against the seed data in data.sql (49 tasks, ids 20 and 21 archived).
@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void statusFilterAppliesWithoutSearchTerm() throws Exception {
        mockMvc.perform(get("/api/tasks").param("status", "DONE").param("pageSize", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.items[*].status", everyItem(is("DONE"))));
    }

    @Test
    void archivedTasksAreExcluded() throws Exception {
        mockMvc.perform(get("/api/tasks").param("pageSize", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(47))
                .andExpect(jsonPath("$.items[*].archived", everyItem(is(false))));
    }

    @Test
    void searchMatchesTitleOrDescription() throws Exception {
        // "api" is in the description only for tasks 11, 12, 16, 40 and 46
        mockMvc.perform(get("/api/tasks").param("q", "api").param("pageSize", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(8))
                .andExpect(jsonPath("$.items[*].id", hasItems(2, 12, 46)))
                .andExpect(jsonPath("$.items[*].id", not(hasItems(20, 21))));

        // "refactor" is in the title only, for task 4
        mockMvc.perform(get("/api/tasks").param("q", "refactor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value(4));
    }

    @Test
    void statusFilterAppliesToTitleMatches() throws Exception {
        // task 2 ("Update API rate limiting") matches by title but is IN_PROGRESS
        mockMvc.perform(get("/api/tasks").param("q", "api").param("status", "OPEN").param("pageSize", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(6))
                .andExpect(jsonPath("$.items[*].status", everyItem(is("OPEN"))));
    }

    @Test
    void statusIsCaseInsensitive() throws Exception {
        mockMvc.perform(get("/api/tasks").param("status", "done"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(5));
    }

    @Test
    void pageBeyondLastReturnsEmptyItems() throws Exception {
        mockMvc.perform(get("/api/tasks").param("page", "99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.total").value(47));
    }

    @Test
    void hugePageDoesNotOverflow() throws Exception {
        // (page - 1) * pageSize exceeds Integer.MAX_VALUE here
        mockMvc.perform(get("/api/tasks").param("page", "30000000").param("pageSize", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.total").value(47));
    }

    @Test
    void invalidStatusIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/tasks").param("status", "foo"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidPageIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/tasks").param("page", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidPageSizeIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/tasks").param("pageSize", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/tasks").param("pageSize", "-5"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/tasks").param("pageSize", "101"))
                .andExpect(status().isBadRequest());
    }
}
