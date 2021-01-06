package com.example.springwebfluxrepro.controllers;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.Context;

import java.time.Duration;
import java.util.List;

/**
 * Hit the controllers as follows:
 * http://localhost:8080/external/flux/{test}
 */
@RestController
public class WebFluxReproController {

    private static final ParameterizedTypeReference<List<String>> LIST_OF_STRING_TYPE_REF = new ParameterizedTypeReference<>() {
    };
    private final WebClient serviceClient = WebClient.create();

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

    @Trace
    public Mono<String> postExternalMonoAlternative(String value) {
        Mono<String> stringMono = serviceClient
                .post()
                .uri("http://example.net")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(value)
                .retrieve()
                .bodyToMono(String.class);
        return stringMono;
    }

    @GetMapping("/fireAndForget")
    public Mono<String> callWithFireAndForget() {
        return this.postExternalMono("foo")
                .doOnNext(this::fireAndForgetLogic); // doOnNext splits work off to separate thread and hits switchIfEmpty
    }

    @Trace(async = true)
    private void fireAndForgetLogic(final String tests) {
        Token token = NewRelic.getAgent().getTransaction().getToken();
        Mono.just(tests)
                .flatMap(this::postExternalMonoAlternative) // ---> NOT TRACKED external call
                .delayElement(Duration.ofSeconds(1))
                .subscribeOn(Schedulers.parallel())
                .subscriberContext(Context.of("nr-token", token))
                .doOnNext(s -> token.link())
                .subscribe(); // subscribe splits work off to separate thread and hits switchIfEmpty
    }

}
