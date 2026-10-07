package com.spring.bank.domain.http.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.bank.common.exception.CardNotFoundException;
import com.spring.bank.common.exception.InsufficientFundsException;
import com.spring.bank.common.exception.InvalidCvvException;
import com.spring.bank.domain.dto.card.CardResponseDTO;
import com.spring.bank.domain.dto.card.CreateCardDTO;
import com.spring.bank.domain.dto.card.PurchaseDTO;
import com.spring.bank.domain.enums.card.CardStatus;
import com.spring.bank.domain.enums.card.CardType;
import com.spring.bank.domain.model.Account;
import com.spring.bank.domain.model.Card;
import com.spring.bank.domain.model.User;
import com.spring.bank.domain.service.CardService;
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
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CardController.class)
@AutoConfigureMockMvc(addFilters = false)
@Disabled("Legacy domain controllers are excluded from the application runtime")
class CardControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private CardService cardService;
    @MockBean private com.spring.bank.common.config.security.JwtTokenProvider jwtTokenProvider;

    private Card card;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setName("John");

        Account account = new Account();
        account.setUser(user);

        card = new Card();
        card.setId("uuid-123");
        card.setAccount(account);
        card.setCardType(CardType.DEBIT);
        card.setCardStatus(CardStatus.ACTIVE);
        card.setNumber("9900000000000001");
        card.setExpirationDate(LocalDate.now().plusYears(3));
    }

    @Test
    void createCard_shouldReturn201WithCardData() throws Exception {
        when(cardService.createCard(1L, CardType.DEBIT)).thenReturn(card);

        mockMvc.perform(post("/card")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCardDTO(1L, CardType.DEBIT))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("DEBIT"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void listCards_shouldReturn200WithCardList() throws Exception {
        when(cardService.listCards(1L)).thenReturn(List.of(new CardResponseDTO(card)));

        mockMvc.perform(get("/card/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("DEBIT"));
    }

    @Test
    void purchase_shouldReturn200WhenPaymentSucceeds() throws Exception {
        PurchaseDTO dto = new PurchaseDTO("9900000000000001", "123", new BigDecimal("50.00"), CardType.DEBIT);
        doNothing().when(cardService).processPayment(any());

        mockMvc.perform(post("/card/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(cardService).processPayment(any());
    }

    @Test
    void purchase_shouldReturn422WhenInsufficientFunds() throws Exception {
        PurchaseDTO dto = new PurchaseDTO("9900000000000001", "123", new BigDecimal("9999.00"), CardType.DEBIT);
        doThrow(new InsufficientFundsException("Insufficient funds")).when(cardService).processPayment(any());

        mockMvc.perform(post("/card/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void purchase_shouldReturn422WhenCvvIsInvalid() throws Exception {
        PurchaseDTO dto = new PurchaseDTO("9900000000000001", "000", new BigDecimal("50.00"), CardType.DEBIT);
        doThrow(new InvalidCvvException("Invalid CVV")).when(cardService).processPayment(any());

        mockMvc.perform(post("/card/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void purchase_shouldReturn404WhenCardNotFound() throws Exception {
        PurchaseDTO dto = new PurchaseDTO("0000000000000000", "000", new BigDecimal("50.00"), CardType.DEBIT);
        doThrow(new CardNotFoundException("Card not found")).when(cardService).processPayment(any());

        mockMvc.perform(post("/card/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }
}
