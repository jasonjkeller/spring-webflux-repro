package com.example.springwebfluxrepro.controllers;

import com.newrelic.api.agent.Trace;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Hit the controllers as follows:
 * http://localhost:8080/external/flux/{test}
 */
@RestController
public class WebFluxReproController {

    private final WebClient serviceClient = WebClient.create();
    private static final ParameterizedTypeReference<List<String>> LIST_OF_STRING_TYPE_REF = new ParameterizedTypeReference<>() {
    };

    @GetMapping("/external/flux/flatMap")
    public Flux<String> getExternalsWithReturnTypeFlux() {
        return getExternalFlux()
                .flatMap(this::postExternalMono); // (1) external call
    }

    @GetMapping("/external/flux/flatMapMany")
    public Flux<String> getExternalsWithFlatMapManyUsage() {
        return getExternalMono()
                .flatMapMany(Flux::fromIterable)
                .flatMap(this::postExternalMono); // (2) external call
    }

    @Trace
    public Flux<String> getExternalFlux() {
        return serviceClient
                .get()
                .uri("http://example.com")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToFlux(String.class);
    }

    @Trace
    public Mono<List<String>> getExternalMono() {
        return serviceClient
                .get()
                .uri("http://example.net")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(LIST_OF_STRING_TYPE_REF);
    }

    @Trace
    public Mono<String> postExternalMono(String value) {
        return serviceClient
                .post()
                .uri("http://example.org")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(value)
                .retrieve()
                .bodyToMono(String.class);
    }
}
