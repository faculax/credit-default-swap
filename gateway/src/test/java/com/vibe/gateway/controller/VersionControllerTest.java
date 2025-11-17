package com.vibe.gateway.controller;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import com.creditdefaultswap.unit.platform.testing.allure.EpicType;
import com.creditdefaultswap.unit.platform.testing.allure.FeatureType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@Epic(EpicType.UNIT_TESTS)
@WebFluxTest(VersionController.class)
public class VersionControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @Feature(FeatureType.GATEWAY_SERVICE)
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
