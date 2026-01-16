package com.example.tsp.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressData {
    @JsonProperty("display_name")
    private String displayName;
    private double lat;
    private double lon;
    
    // Optional schedule info for TSPTW results
    private ScheduleInfo schedule;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ScheduleInfo {
        private String arrival;
        private String wait;
        private String departure;
    }
}
