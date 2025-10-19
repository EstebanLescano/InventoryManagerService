package org.lea.imsfront.controllers;

import org.lea.imsfront.models.ReservationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/inventory")
public class InventoryController {
    private final WebClient webClient;

    public InventoryController(WebClient.Builder webClientBuilder,
                               @Value("${backend.base-url}") String backendUrl) {
        this.webClient = webClientBuilder.baseUrl(backendUrl).build();
    }

    @GetMapping("/reserve")
    public String showForm() {
        return "reserve";
    }

    @PostMapping("/reserve")
    public Mono<String> reserveStock(
            @RequestParam String storeId,
            @RequestParam String sku,
            @RequestParam int quantity,
            Model model) {

        ReservationRequest request = new ReservationRequest(storeId, sku, quantity);

        return webClient.post()
                .uri("/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    model.addAttribute("message", response);
                    model.addAttribute("success", true);
                    return "reserve";
                })
                .onErrorResume(error -> {
                    model.addAttribute("message", "Error en la reserva: " + error.getMessage());
                    model.addAttribute("success", false);
                    return Mono.just("reserve");
                });
    }
}

