package com.example.tsp;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class DiagnosticGeo {
    public static void main(String[] args) {
        String[] testAddresses = {
                "hòa khánh",
                "hòa khánh, Vietnam",
                "dien bien phu da nang",
                "dien bien phu da nang, Vietnam"
        };

        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        for (String address : testAddresses) {
            System.out.println("\nTesting: " + address);
            try {
                String url = "https://nominatim.openstreetmap.org/search?format=json&limit=1&q=" +
                        java.net.URLEncoder.encode(address, java.nio.charset.StandardCharsets.UTF_8);

                System.out.println("URL: " + url);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", "Diagnostic-Tool/1.0")
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                System.out.println("Status: " + response.statusCode());
                System.out.println("Body: " + response.body());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
