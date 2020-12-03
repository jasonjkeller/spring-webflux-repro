# spring-weblux-repro

The New Relic Java agent (as of 6.2.1) currently isn't instrumenting some parts of the Spring WebFlux reactive code. This app reproduces the issue.

## Usage

Add the `-javaagent:/path/to/newrelic.jar` flag to the `SpringWebfluxReproApplication` run config.

If you hit either endpoint you'll only see one external call but each should have two:

http://localhost:8080/external/flux/flatMap  
http://localhost:8080/external/flux/flatMapMany
