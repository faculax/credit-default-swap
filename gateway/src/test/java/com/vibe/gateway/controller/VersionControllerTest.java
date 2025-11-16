package com.vibe.gateway.controller;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@Epic("Unit Tests")
@WebFluxTest(VersionController.class)
public class VersionControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @Feature("Gateway Service")
    @Story("Version API - Get Version Info")
    public void testGetVersionInfo() throws Exception {
        webTestClient.get()
                .uri("/api/version")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").exists();
    }
} 