package com.mariem.assurance;

import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/auth-proxy" )
public class KeycloakProxyController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String keycloakBaseUrl = "http://localhost:9090";

    @PostMapping("/token" )
    public ResponseEntity<Object> getToken(@RequestBody Map<String, String> formData) {
        String keycloakTokenUrl = keycloakBaseUrl + "/realms/assurance/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        formData.forEach(map::add);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        return restTemplate.exchange(
                keycloakTokenUrl,
                HttpMethod.POST,
                request,
                Object.class
        );
    }

    @GetMapping("/auth")
    public ResponseEntity<Object> redirectToAuth(@RequestParam Map<String, String> params) {
        String keycloakAuthUrl = keycloakBaseUrl + "/realms/assurance/protocol/openid-connect/auth";

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(keycloakAuthUrl);
        params.forEach(builder::queryParam);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(builder.toUriString()));

        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
