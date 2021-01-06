package com.example.springwebfluxrepro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.tools.agent.ReactorDebugAgent;

@SpringBootApplication
public class SpringWebfluxReproApplication {

    public static void main(String[] args) {
        ReactorDebugAgent.init();
        // Use it only if you see that some call-sites are not instrumented
        ReactorDebugAgent.processExistingClasses();
        SpringApplication.run(SpringWebfluxReproApplication.class, args);
    }

}
