package com.spring.bank.domain.http.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.bank.common.exception.InsufficientFundsException;
import com.spring.bank.common.exception.SavingsBoxNotFoundException;
import com.spring.bank.domain.dto.savingsbox.CreateSavingsBoxDTO;
import com.spring.bank.domain.dto.savingsbox.SavingsBoxDepositDTO;
import com.spring.bank.domain.dto.savingsbox.SavingsBoxWithdrawDTO;
import com.spring.bank.domain.enums.account.AccountStatusEnum;
import com.spring.bank.domain.enums.account.AccountTypeEnum;
import com.spring.bank.domain.model.Account;
import com.spring.bank.domain.model.SavingsBox;
import com.spring.bank.domain.service.SavingsBoxService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SavingsBoxController.class)
@AutoConfigureMockMvc(addFilters = false)
@Disabled("Legacy domain controllers are excluded from the application runtime")
class SavingsBoxControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private SavingsBoxService savingsBoxService;
    @MockBean private com.spring.bank.common.config.security.JwtTokenProvider jwtTokenProvider;

    private Account account;
    private SavingsBox savingsBox;

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setId(1L);
        account.setType(AccountTypeEnum.CHECKING);
        account.setStatus(AccountStatusEnum.ACTIVE);
        account.setBalance(new BigDecimal("1000.00"));

        savingsBox = new SavingsBox();
        savingsBox.setId(1L);
        savingsBox.setAccount(account);
        savingsBox.setName("Viagem Europa");
        savingsBox.setImageUrl("https://example.com/europa.jpg");
        savingsBox.setBalance(new BigDecimal("500.00"));
    }

    @Test
    void create_shouldReturn201WithBoxData() throws Exception {
        CreateSavingsBoxDTO dto = new CreateSavingsBoxDTO(1L, "Viagem Europa", "https://example.com/europa.jpg", null);
        when(savingsBoxService.create(any())).thenReturn(savingsBox);

        mockMvc.perform(post("/savings-box")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Viagem Europa"))
                .andExpect(jsonPath("$.balance").value(500.00))
                .andExpect(jsonPath("$.accountId").value(1));
    }

    @Test
    void create_shouldReturn400WhenNameIsBlank() throws Exception {
        CreateSavingsBoxDTO dto = new CreateSavingsBoxDTO(1L, "", "https://example.com/img.jpg", null);

        mockMvc.perform(post("/savings-box")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deposit_shouldReturn200WithUpdatedBalance() throws Exception {
        savingsBox.setBalance(new BigDecimal("800.00"));
        SavingsBoxDepositDTO dto = new SavingsBoxDepositDTO(new BigDecimal("300.00"));
        when(savingsBoxService.deposit(eq(1L), any())).thenReturn(savingsBox);

        mockMvc.perform(post("/savings-box/1/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(800.00));
    }

    @Test
    void deposit_shouldReturn422WhenInsufficientAccountFunds() throws Exception {
        SavingsBoxDepositDTO dto = new SavingsBoxDepositDTO(new BigDecimal("9999.00"));
        when(savingsBoxService.deposit(eq(1L), any())).thenThrow(new InsufficientFundsException("Insufficient funds"));

        mockMvc.perform(post("/savings-box/1/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void withdraw_shouldReturn200WithUpdatedBalance() throws Exception {
        savingsBox.setBalance(new BigDecimal("200.00"));
        SavingsBoxWithdrawDTO dto = new SavingsBoxWithdrawDTO(new BigDecimal("300.00"));
        when(savingsBoxService.withdraw(eq(1L), any())).thenReturn(savingsBox);

        mockMvc.perform(post("/savings-box/1/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(200.00));
    }

    @Test
    void withdraw_shouldReturn422WhenInsufficientBoxBalance() throws Exception {
        SavingsBoxWithdrawDTO dto = new SavingsBoxWithdrawDTO(new BigDecimal("9999.00"));
        when(savingsBoxService.withdraw(eq(1L), any())).thenThrow(new InsufficientFundsException("Insufficient balance in savings box"));

        mockMvc.perform(post("/savings-box/1/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void listByAccount_shouldReturn200WithBoxList() throws Exception {
        when(savingsBoxService.listByAccount(1L)).thenReturn(List.of(savingsBox));

        mockMvc.perform(get("/savings-box/account/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Viagem Europa"));
    }

    @Test
    void getById_shouldReturn200WithBoxData() throws Exception {
        when(savingsBoxService.getById(1L)).thenReturn(savingsBox);

        mockMvc.perform(get("/savings-box/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Viagem Europa"));
    }

    @Test
    void getById_shouldReturn404WhenBoxNotFound() throws Exception {
        when(savingsBoxService.getById(99L)).thenThrow(new SavingsBoxNotFoundException("Savings box with ID 99 not found"));

        mockMvc.perform(get("/savings-box/99"))
                .andExpect(status().isNotFound());
    }
}
