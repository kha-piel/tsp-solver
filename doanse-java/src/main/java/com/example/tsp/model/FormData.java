package com.example.tsp.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class FormData {
    @JsonProperty("kho_hang")
    private String warehouseAddress;
    
    @JsonProperty("cac_diem_giao")
    private List<DeliveryPointInput> deliveryPoints;
    
    private String mode;
    
    @JsonProperty("start_time")
    private String startTime;
}
