package com.spring.bank.domain.http.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.bank.common.exception.InsufficientFundsException;
import com.spring.bank.domain.dto.investment.ApplyInvestmentDTO;
import com.spring.bank.domain.dto.investment.RedeemInvestmentDTO;
import com.spring.bank.domain.enums.account.AccountStatusEnum;
import com.spring.bank.domain.enums.account.AccountTypeEnum;
import com.spring.bank.domain.model.Account;
import com.spring.bank.domain.service.InvestmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InvestmentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Disabled("Legacy domain controllers are excluded from the application runtime")
class InvestmentControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private InvestmentService investmentService;
    @MockBean private com.spring.bank.common.config.security.JwtTokenProvider jwtTokenProvider;

    private Account investmentAccount;

    @BeforeEach
    void setUp() {
        investmentAccount = new Account();
        investmentAccount.setId(2L);
        investmentAccount.setNumber("20000002");
        investmentAccount.setType(AccountTypeEnum.INVESTMENT);
        investmentAccount.setStatus(AccountStatusEnum.ACTIVE);
        investmentAccount.setBalance(new BigDecimal("1500.00"));
    }

    @Test
    void apply_shouldReturn201WithInvestmentAccountData() throws Exception {
        ApplyInvestmentDTO dto = new ApplyInvestmentDTO(1L, new BigDecimal("500.00"));
        when(investmentService.apply(any())).thenReturn(investmentAccount);

        mockMvc.perform(post("/investment/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("INVESTMENT"))
                .andExpect(jsonPath("$.balance").value(1500.00));
    }

    @Test
    void apply_shouldReturn422WhenInsufficientFunds() throws Exception {
        ApplyInvestmentDTO dto = new ApplyInvestmentDTO(1L, new BigDecimal("99999.00"));
        when(investmentService.apply(any())).thenThrow(new InsufficientFundsException("Insufficient balance"));

        mockMvc.perform(post("/investment/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void redeem_shouldReturn200WithUpdatedInvestmentBalance() throws Exception {
        investmentAccount.setBalance(new BigDecimal("1000.00"));
        RedeemInvestmentDTO dto = new RedeemInvestmentDTO(1L, new BigDecimal("500.00"));
        when(investmentService.redeem(any())).thenReturn(investmentAccount);

        mockMvc.perform(post("/investment/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1000.00));
    }

    @Test
    void redeem_shouldReturn422WhenInvestmentBalanceInsufficient() throws Exception {
        RedeemInvestmentDTO dto = new RedeemInvestmentDTO(1L, new BigDecimal("99999.00"));
        when(investmentService.redeem(any())).thenThrow(new InsufficientFundsException("Insufficient balance"));

        mockMvc.perform(post("/investment/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnprocessableEntity());
    }
}
