package ru.balandin.docflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.balandin.docflow.audit.DocumentEventRepository;
import ru.balandin.docflow.document.DocumentRepository;

import java.util.UUID;
import java.util.regex.Pattern;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentWorkflowTest {
    @Autowired MockMvc mvc;
    @Autowired DocumentRepository documents;
    @Autowired DocumentEventRepository events;

    @BeforeEach
    void cleanDatabase() {
        events.deleteAll();
        documents.deleteAll();
    }

    @Test
    void authorSubmitsAndApproverApprovesWithAuditHistory() throws Exception {
        UUID id = createDocument();
        mvc.perform(get("/api/documents/{id}", id).with(httpBasic("bob", "approver_test")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/documents/{id}/submit", id).with(httpBasic("alice", "author_test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
        mvc.perform(post("/api/documents/{id}/approve", id)
                        .with(httpBasic("bob", "approver_test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Reviewed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
        mvc.perform(get("/api/documents/{id}/events", id).with(httpBasic("alice", "author_test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("CREATED"))
                .andExpect(jsonPath("$[1].action").value("SUBMITTED"))
                .andExpect(jsonPath("$[2].action").value("APPROVED"));
        mvc.perform(get("/api/documents").with(httpBasic("bob", "approver_test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void rejectsInvalidTransitionsAndRoleViolations() throws Exception {
        UUID id = createDocument();
        mvc.perform(post("/api/documents/{id}/approve", id).with(httpBasic("alice", "author_test")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/documents/{id}/submit", id).with(httpBasic("bob", "approver_test")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/documents/{id}/submit", id).with(httpBasic("alice", "author_test")))
                .andExpect(status().isOk());
        mvc.perform(post("/api/documents/{id}/submit", id).with(httpBasic("alice", "author_test")))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/documents/{id}/reject", id)
                        .with(httpBasic("bob", "approver_test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/documents/{id}/reject", id)
                        .with(httpBasic("bob", "approver_test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Missing details\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
        mvc.perform(post("/api/documents/{id}/approve", id).with(httpBasic("bob", "approver_test")))
                .andExpect(status().isConflict());
    }

    @Test
    void validatesInputAndRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/documents")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/documents")
                        .with(httpBasic("alice", "author_test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"body\":\"text\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/documents?page=-1").with(httpBasic("alice", "author_test")))
                .andExpect(status().isBadRequest());
    }

    private UUID createDocument() throws Exception {
        String json = mvc.perform(post("/api/documents")
                        .with(httpBasic("alice", "author_test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Contract\",\"body\":\"Please approve\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();
        var matcher = Pattern.compile("\"id\":\"([^\"]+)\"").matcher(json);
        if (!matcher.find()) throw new AssertionError("id missing: " + json);
        return UUID.fromString(matcher.group(1));
    }
}
