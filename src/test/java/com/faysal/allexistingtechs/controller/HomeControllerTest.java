package com.faysal.allexistingtechs.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class HomeControllerTest {

    private final HomeController homeController = new HomeController();

    @Test
    void getHome_shouldReturnWelcomeMessage() {
        assertEquals("Welcome Home", homeController.getHome());
    }

    @Test
    void homeEndpoint_shouldReturnWelcomeMessage() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(homeController).build();

        mockMvc.perform(get("/home"))
                .andExpect(status().isOk())
                .andExpect(content().string("Welcome Home"));
    }
}