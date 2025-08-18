package com.mariem.assurance.services;



import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class IAService {

    private RestTemplate restTemplate = new RestTemplate();

    public String predict(double feature1, double feature2) {
        String url = "http://localhost:8000/predict";

        String json = String.format("{\"feature1\": %f, \"feature2\": %f}", feature1, feature2);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<>(json, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        return response.getBody();
    }
}
