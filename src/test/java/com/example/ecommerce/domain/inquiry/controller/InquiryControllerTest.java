package com.example.ecommerce.domain.inquiry.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ecommerce.domain.inquiry.dto.request.InquiryCreateRequest;
import com.example.ecommerce.domain.inquiry.dto.response.InquiryResponse;
import com.example.ecommerce.domain.inquiry.enums.InquiryStatus;
import com.example.ecommerce.domain.inquiry.service.InquiryService;
import com.example.ecommerce.domain.user.entity.User;
import com.example.ecommerce.domain.user.enums.Provider;
import com.example.ecommerce.domain.user.enums.Role;
import com.example.ecommerce.domain.user.enums.UserStatus;
import com.example.ecommerce.global.exception.CustomException;
import com.example.ecommerce.global.exception.ErrorCode;
import com.example.ecommerce.support.TestAuthentication;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class InquiryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InquiryService inquiryService;

    private User testUser(Long id) {
        User user = User.builder()
                .email("buyer@test.com")
                .name("구매자")
                .provider(Provider.LOCAL)
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    void createInquiry_returns201_whenAuthenticated() throws Exception {
        InquiryResponse response = new InquiryResponse(
                1L, null, "배송", "배송 문의", "언제 오나요?", null, InquiryStatus.WAITING, LocalDateTime.now(), null);
        when(inquiryService.createInquiry(eq(1L), any())).thenReturn(response);

        InquiryCreateRequest request = new InquiryCreateRequest("배송", "배송 문의", "언제 오나요?", null);

        mockMvc.perform(post("/api/v1/inquiries")
                        .with(TestAuthentication.asUser(testUser(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("배송 문의"));
    }

    @Test
    void getMyInquiry_returns403_whenNotOwner() throws Exception {
        when(inquiryService.getMyInquiry(eq(2L), eq(1L)))
                .thenThrow(new CustomException(ErrorCode.INQUIRY_ACCESS_DENIED));

        mockMvc.perform(get("/api/v1/inquiries/1")
                        .with(TestAuthentication.asUser(testUser(2L))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("INQUIRY_ACCESS_DENIED"));
    }

    @Test
    void createInquiry_returns401_whenNotAuthenticated() throws Exception {
        InquiryCreateRequest request = new InquiryCreateRequest("배송", "배송 문의", "언제 오나요?", null);

        mockMvc.perform(post("/api/v1/inquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
