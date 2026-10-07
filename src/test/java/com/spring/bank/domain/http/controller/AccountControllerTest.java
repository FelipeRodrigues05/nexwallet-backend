package com.spring.bank.domain.http.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.bank.common.exception.AccountTypeAlreadyExistsException;
import com.spring.bank.common.exception.InsufficientFundsException;
import com.spring.bank.domain.dto.account.AccountResponseDTO;
import com.spring.bank.domain.dto.account.DepositDTO;
import com.spring.bank.domain.dto.account.OpenAccountDTO;
import com.spring.bank.domain.dto.account.WithdrawDTO;
import com.spring.bank.domain.enums.account.AccountStatusEnum;
import com.spring.bank.domain.enums.account.AccountTypeEnum;
import com.spring.bank.domain.model.Account;
import com.spring.bank.domain.service.AccountService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
@Disabled("Legacy domain controllers are excluded from the application runtime")
class AccountControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private AccountService accountService;
    @MockBean private com.spring.bank.common.config.security.JwtTokenProvider jwtTokenProvider;

    private Account account;

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setId(1L);
        account.setNumber("10000001");
        account.setType(AccountTypeEnum.CHECKING);
        account.setStatus(AccountStatusEnum.ACTIVE);
        account.setBalance(new BigDecimal("0.00"));
    }

    @Test
    void createAccount_shouldReturn201WithAccountData() throws Exception {
        OpenAccountDTO dto = new OpenAccountDTO(1L, AccountTypeEnum.CHECKING);
        when(accountService.openAccount(any())).thenReturn(account);

        mockMvc.perform(post("/account/open")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.number").value("10000001"))
                .andExpect(jsonPath("$.type").value("CHECKING"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createAccount_shouldReturn409WhenDuplicateAccountType() throws Exception {
        OpenAccountDTO dto = new OpenAccountDTO(1L, AccountTypeEnum.CHECKING);
        when(accountService.openAccount(any())).thenThrow(new AccountTypeAlreadyExistsException("User already has a CHECKING account"));

        mockMvc.perform(post("/account/open")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict());
    }

    @Test
    void getAccount_shouldReturn200WithAccountData() throws Exception {
        when(accountService.getAccountByNumber("10000001")).thenReturn(account);

        mockMvc.perform(get("/account/10000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value("10000001"));
    }

    @Test
    void deposit_shouldReturn200OnSuccess() throws Exception {
        DepositDTO dto = new DepositDTO(1L, new BigDecimal("100.00"));
        account.setBalance(new BigDecimal("100.00"));
        when(accountService.deposit(any())).thenReturn(account);

        mockMvc.perform(post("/account/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(100.00));
    }

    @Test
    void deposit_shouldReturn422WhenInsufficientFunds() throws Exception {
        DepositDTO dto = new DepositDTO(1L, new BigDecimal("99999.00"));
        when(accountService.deposit(any())).thenThrow(new InsufficientFundsException("Insufficient balance"));

        mockMvc.perform(post("/account/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void withdraw_shouldReturn200OnSuccess() throws Exception {
        WithdrawDTO dto = new WithdrawDTO(1L, new BigDecimal("50.00"));
        when(accountService.withdraw(any())).thenReturn(account);

        mockMvc.perform(post("/account/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }
}
