package com.example.tsp.service;

import com.example.tsp.model.AddressData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoutingService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String OSRM_URL_TEMPLATE = "http://router.project-osrm.org/table/v1/driving/%s?annotations=distance,duration";

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RoutingMatrix {
        private double[][] distances;
        private double[][] durations;
    }

    public RoutingMatrix getRouteInfo(List<AddressData> locations) {
        String locationsStr = locations.stream()
                .map(l -> l.getLon() + "," + l.getLat())
                .collect(Collectors.joining(";"));

        String url = String.format(OSRM_URL_TEMPLATE, locationsStr);

        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                String response = restTemplate.getForObject(url, String.class);
                if (response != null) {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode root = mapper.readTree(response);

                    if ("Ok".equals(root.path("code").asText())) {
                        JsonNode distNode = root.get("distances");
                        JsonNode durNode = root.get("durations");

                        int size = distNode.size();
                        double[][] distances = new double[size][size];
                        double[][] durations = new double[size][size];

                        for (int i = 0; i < size; i++) {
                            for (int j = 0; j < size; j++) {
                                distances[i][j] = distNode.get(i).get(j).asDouble();
                                durations[i][j] = durNode.get(i).get(j).asDouble();
                            }
                        }
                        return new RoutingMatrix(distances, durations);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error calling OSRM API (attempt " + (attempt + 1) + "): " + e.getMessage());
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return null;
    }
}
