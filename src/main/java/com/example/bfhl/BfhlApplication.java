package com.example.bfhl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@SpringBootApplication
public class BfhlApplication {

    public static void main(String[] args) {
        SpringApplication.run(BfhlApplication.class, args);
    }

    @Bean
    WebClient webClient() {
        return WebClient.builder().build();
    }

    @Bean
    CommandLineRunner runOnStartup(WebClient webClient) {
        return args -> {
            String name = "Shreyansh JAin";
            String regNo = "0101CS221127";
            String email = "shreyansh01122002@gmail.com";

            String generateUrl = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

            GenerateWebhookRequest requestBody = new GenerateWebhookRequest(name, regNo, email);

            GenerateWebhookResponse response = webClient.post()
                    .uri(generateUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(requestBody))
                    .retrieve()
                    .bodyToMono(GenerateWebhookResponse.class)
                    .onErrorResume(ex -> {
                        System.out.println("Failed to generate webhook: " + ex.getMessage());
                        return Mono.empty();
                    })
                    .block();

            String webhookUrl = (response != null && response.getWebhook() != null && !response.getWebhook().isBlank())
                    ? response.getWebhook()
                    : "https://bfhldevapigw.healthrx.co.in/hiring/testWebhook/JAVA";
            String accessToken = response != null ? response.getAccessToken() : null;

            String sqlPath = resolveSqlPathByRegNo(regNo);
            String sqlQuery = readSqlFromClasspath(sqlPath);

            if (sqlQuery == null || sqlQuery.isBlank()) {
                System.out.println("Could not read SQL from: " + sqlPath);
                return;
            }

            WebhookSubmitRequest submitRequest = new WebhookSubmitRequest(sqlQuery);

            String finalResponse = webClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> {
                        if (accessToken != null && !accessToken.isBlank()) {
                            headers.add("Authorization", accessToken);
                        }
                    })
                    .body(BodyInserters.fromValue(submitRequest))
                    .retrieve()
                    .bodyToMono(String.class)
                    .onErrorResume(ex -> {
                        System.out.println("Failed to post to webhook: " + ex.getMessage());
                        return Mono.just("{}");
                    })
                    .block();

            System.out.println("Webhook response: " + finalResponse);
        };
    }

    private String resolveSqlPathByRegNo(String regNo) {
        if (regNo == null || regNo.length() < 2) {
            return "sql/question1.sql";
        }
        String digitsSuffix = regNo.replaceAll("[^0-9]", "");
        if (digitsSuffix.length() < 2) {
            return "sql/question1.sql";
        }
        String lastTwo = digitsSuffix.substring(digitsSuffix.length() - 2);
        int number = Integer.parseInt(lastTwo);
        boolean isOdd = (number % 2) == 1;
        return isOdd ? "sql/question1.sql" : "sql/question2.sql";
    }

    private String readSqlFromClasspath(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            if (!resource.exists()) {
                return null;
            }
            try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                return FileCopyUtils.copyToString(reader).trim();
            }
        } catch (Exception ex) {
            System.out.println("Error reading SQL file '" + path + "': " + ex.getMessage());
            return null;
        }
    }

    public record GenerateWebhookRequest(
            @JsonProperty("name") String name,
            @JsonProperty("regNo") String regNo,
            @JsonProperty("email") String email
    ) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GenerateWebhookResponse {
        @JsonProperty("webhook")
        private String webhook;
        @JsonProperty("accessToken")
        private String accessToken;

        public String getWebhook() { return webhook; }
        public String getAccessToken() { return accessToken; }
        public void setWebhook(String webhook) { this.webhook = webhook; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    }

    public record WebhookSubmitRequest(@JsonProperty("finalQuery") String finalQuery) { }
}


