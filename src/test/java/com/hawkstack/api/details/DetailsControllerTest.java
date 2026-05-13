package com.hawkstack.api.details;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hawkstack.api.auth.dto.LoginRequest;
import com.hawkstack.api.auth.dto.LoginResponse;
import com.hawkstack.api.auth.dto.RegisterRequest;
import com.hawkstack.api.details.dto.DetailsRequest;
import com.hawkstack.api.storage.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class DetailsControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean S3Service s3Service;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        when(s3Service.upload(any(), anyString()))
                .thenReturn("https://s3.example.com/file.pdf");

        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("details@example.com");
        reg.setPassword("password123");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)));

        LoginRequest login = new LoginRequest();
        login.setEmail("details@example.com");
        login.setPassword("password123");
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
                .andReturn();
        LoginResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), LoginResponse.class);
        token = response.getToken();
    }

    @Test
    void getDetails_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/details"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createDetails_withValidToken_returns201() throws Exception {
        DetailsRequest request = new DetailsRequest();
        request.setName("Eikshit Singhal");
        request.setEmail("eikshit@example.com");
        request.setCountryCode("+91");
        request.setPhone("9876543210");
        request.setAddress("Bangalore, Karnataka");

        mockMvc.perform(post("/api/details")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Eikshit Singhal"))
                .andExpect(jsonPath("$.phone").value("9876543210"));
    }

    @Test
    void createDetails_twice_returns409() throws Exception {
        DetailsRequest request = new DetailsRequest();
        request.setName("First");
        mockMvc.perform(post("/api/details")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        mockMvc.perform(post("/api/details")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void getDetails_afterCreate_returnsRecord() throws Exception {
        DetailsRequest request = new DetailsRequest();
        request.setName("Test User");
        mockMvc.perform(post("/api/details")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        mockMvc.perform(get("/api/details")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test User"));
    }

    @Test
    void getDetails_whenNoneExist_returns404() throws Exception {
        mockMvc.perform(get("/api/details")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateDetails_changesFields() throws Exception {
        DetailsRequest create = new DetailsRequest();
        create.setName("Original");
        mockMvc.perform(post("/api/details")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(create)));

        DetailsRequest update = new DetailsRequest();
        update.setName("Updated");
        mockMvc.perform(put("/api/details")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    void deleteDetails_returns204_andSubsequentGetReturns404() throws Exception {
        DetailsRequest create = new DetailsRequest();
        create.setName("To Delete");
        mockMvc.perform(post("/api/details")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(create)));

        mockMvc.perform(delete("/api/details")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/details")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void uploadPdf_savesUrlInRecord() throws Exception {
        DetailsRequest create = new DetailsRequest();
        create.setName("PDF User");
        mockMvc.perform(post("/api/details")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(create)));

        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", "pdf content".getBytes());

        mockMvc.perform(multipart("/api/details/upload/pdf")
                .file(file)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pdfUrl").value("https://s3.example.com/file.pdf"));
    }

    @Test
    void uploadVideo_savesUrlInRecord() throws Exception {
        DetailsRequest create = new DetailsRequest();
        create.setName("Video User");
        mockMvc.perform(post("/api/details")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(create)));

        MockMultipartFile file = new MockMultipartFile(
                "file", "intro.mp4", "video/mp4", "video content".getBytes());

        mockMvc.perform(multipart("/api/details/upload/video")
                .file(file)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.videoUrl").value("https://s3.example.com/file.pdf"));
    }
}
