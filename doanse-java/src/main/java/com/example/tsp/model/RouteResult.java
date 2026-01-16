package com.example.tsp.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteResult {
    private String name;
    private List<AddressData> path;
    
    @JsonProperty("distance_km")
    private double distanceKm;
    
    @JsonProperty("exec_time_ms")
    private double execTimeMs;
    
    // For Reroute response
    @JsonProperty("total_duration_text")
    private String totalDurationText;
}
