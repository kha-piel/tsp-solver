package com.example.tsp.service;

import com.example.tsp.model.AddressData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class GeocodingService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";

    public AddressData getCoordsFromAddress(String address) {

        AddressData result = queryNominatim(address);

        if (result == null && !containsVietnam(address)) {
            result = queryNominatim(address + ", Vietnam");
        }

        if (result == null) {
            String heuristic = addCommaBeforeCity(address);
            if (!heuristic.equals(address)) {
                result = queryNominatim(heuristic);
                if (result == null && !containsVietnam(heuristic)) {
                    result = queryNominatim(heuristic + ", Vietnam");
                }
            }
        }
        return result;
    }

    private boolean containsVietnam(String addr) {
        String lower = addr.toLowerCase();
        return lower.contains("vietnam") || lower.contains("việt nam");
    }

    private String addCommaBeforeCity(String address) {
        String lower = address.toLowerCase();
        String[] cities = { "da nang", "đà nẵng", "ha noi", "hà nội", "ho chi minh", "hồ chí minh", "hcm", "can tho",
                "cần thơ", "hai phong", "hải phòng" };

        for (String city : cities) {
            int idx = lower.lastIndexOf(city);
            if (idx > 0) {

                char charBefore = address.charAt(idx - 1);
                if (charBefore != ',' && charBefore != ' ') {
                    // e.g. "phudanang" -> ignore
                } else if (charBefore == ' ') {
                    return address.substring(0, idx).trim() + ", " + address.substring(idx);
                }
            }
        }
        return address;
    }

    private AddressData queryNominatim(String query) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "TSP-Solver-App/1.0");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(NOMINATIM_URL)
                .queryParam("q", query)
                .queryParam("format", "json")
                .queryParam("limit", 1);

        String url = builder.build().toUriString();

        try {
            System.out.println("DEBUG: Querying Nominatim: " + url);
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response.getBody());
                if (root.isArray() && root.size() > 0) {
                    JsonNode first = root.get(0);
                    AddressData data = new AddressData();
                    data.setDisplayName(first.get("display_name").asText());
                    data.setLat(first.get("lat").asDouble());
                    data.setLon(first.get("lon").asDouble());
                    return data;
                }
            }
        } catch (Exception e) {
            System.err.println("Error calling Nominatim API for '" + query + "': " + e.getMessage());
        }
        return null;
    }
}
