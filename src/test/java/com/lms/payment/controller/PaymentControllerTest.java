package com.lms.payment.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.payment.dto.request.InitiatePaymentRequestDto;
import com.lms.payment.dto.response.PaymentHistoryResponseDto;
import com.lms.payment.dto.response.PaymentOrderResponseDto;
import com.lms.payment.service.PaymentService;
import com.lms.payment.vo.PaymentStatus;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private PaymentOrderResponseDto paymentOrderResponseDto;
    private PaymentHistoryResponseDto paymentHistoryResponseDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        objectMapper = new ObjectMapper();

        paymentOrderResponseDto = new PaymentOrderResponseDto();
        paymentOrderResponseDto.setStripeSessionId("sess_123");
        paymentOrderResponseDto.setStatus("PENDING");
        paymentOrderResponseDto.setAmount(BigDecimal.valueOf(500));
        paymentOrderResponseDto.setCurrency("INR");

        paymentHistoryResponseDto = new PaymentHistoryResponseDto();
        paymentHistoryResponseDto.setId(1L);
        paymentHistoryResponseDto.setCourseId(101L);
        paymentHistoryResponseDto.setCourseTitle("Java Course");
        paymentHistoryResponseDto.setAmount(BigDecimal.valueOf(500));
        paymentHistoryResponseDto.setCurrency("INR");
        paymentHistoryResponseDto.setStatus(PaymentStatus.SUCCESS);
    }

    @Test
    void initiatePayment_shouldReturnCreated() throws Exception {
        InitiatePaymentRequestDto request = new InitiatePaymentRequestDto();
        request.setCourseId(101L);
        request.setAmount(BigDecimal.valueOf(500));
        request.setCurrency("INR");

        when(paymentService.initiatePayment(eq(10L), any(InitiatePaymentRequestDto.class)))
                .thenReturn(paymentOrderResponseDto);

        mockMvc.perform(post("/api/v1/payments/initiate")
                        .principal(createAuth(10L, "ROLE_STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Payment order created"))
                .andExpect(jsonPath("$.data.stripeSessionId").value("sess_123"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.amount").value(500))
                .andExpect(jsonPath("$.data.currency").value("INR"));
    }

    @Test
    void getHistory_shouldReturnList() throws Exception {
        when(paymentService.getPaymentHistory(10L))
                .thenReturn(List.of(paymentHistoryResponseDto));

        mockMvc.perform(get("/api/v1/payments/history")
                        .principal(createAuth(10L, "ROLE_STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Payment history"))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].courseId").value(101))
                .andExpect(jsonPath("$.data[0].courseTitle").value("Java Course"));
    }

    @Test
    void verifyPayment_shouldReturnStatus() throws Exception {
        when(paymentService.verifyPayment("sess_123"))
                .thenReturn(paymentOrderResponseDto);

        mockMvc.perform(get("/api/v1/payments/verify/sess_123")
                        .principal(createAuth(10L, "ROLE_STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Payment status"))
                .andExpect(jsonPath("$.data.stripeSessionId").value("sess_123"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void getAllTransactions_shouldReturnPage() throws Exception {
        Page<PaymentHistoryResponseDto> page =
                new PageImpl<>(List.of(paymentHistoryResponseDto), PageRequest.of(0, 20), 1);

        when(paymentService.getAllTransactions(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/payments/admin/all")
                        .param("page", "0")
                        .param("size", "20")
                        .principal(createAuth(1L, "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("All transactions"))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].courseId").value(101));
    }

    @Test
    void initiatePayment_shouldThrowException_whenAuthenticationIsNull() {
        InitiatePaymentRequestDto request = new InitiatePaymentRequestDto();

        assertThrows(NullPointerException.class,
                () -> paymentController.initiatePayment(null, request));
    }

    private Authentication createAuth(Long userId, String role) {
        return new Authentication() {
            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return List.of(new SimpleGrantedAuthority(role));
            }

            @Override
            public Object getCredentials() {
                return userId;
            }

            @Override
            public Object getDetails() {
                return null;
            }

            @Override
            public Object getPrincipal() {
                return "user";
            }

            @Override
            public boolean isAuthenticated() {
                return true;
            }

            @Override
            public void setAuthenticated(boolean isAuthenticated) {
            }

            @Override
            public String getName() {
                return "user";
            }
        };
    }
}