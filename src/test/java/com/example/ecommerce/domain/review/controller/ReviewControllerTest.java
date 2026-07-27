package com.example.ecommerce.domain.review.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ecommerce.domain.review.dto.request.ReviewCreateRequest;
import com.example.ecommerce.domain.review.dto.request.ReviewUpdateRequest;
import com.example.ecommerce.domain.review.dto.response.ReviewResponse;
import com.example.ecommerce.domain.review.service.ReviewService;
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
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReviewService reviewService;

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
    void createReview_returns201_whenAuthenticated() throws Exception {
        ReviewResponse response = new ReviewResponse(1L, 1L, 10L, "탐미오족", 5, "맛있어요", LocalDateTime.now());
        when(reviewService.createReview(eq(1L), any())).thenReturn(response);

        ReviewCreateRequest request = new ReviewCreateRequest(1L, 10L, 5, "맛있어요");

        mockMvc.perform(post("/api/v1/reviews")
                        .with(TestAuthentication.asUser(testUser(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(5));
    }

    @Test
    void updateReview_returns403_whenNotOwner() throws Exception {
        when(reviewService.updateReview(eq(2L), eq(1L), any()))
                .thenThrow(new CustomException(ErrorCode.REVIEW_ACCESS_DENIED));

        ReviewUpdateRequest request = new ReviewUpdateRequest(1, "별로예요");

        mockMvc.perform(put("/api/v1/reviews/1")
                        .with(TestAuthentication.asUser(testUser(2L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("REVIEW_ACCESS_DENIED"));
    }

    @Test
    void deleteReview_returns403_whenNotOwner() throws Exception {
        doThrow(new CustomException(ErrorCode.REVIEW_ACCESS_DENIED))
                .when(reviewService).deleteReview(2L, 1L);

        mockMvc.perform(delete("/api/v1/reviews/1")
                        .with(TestAuthentication.asUser(testUser(2L))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("REVIEW_ACCESS_DENIED"));
    }
}
