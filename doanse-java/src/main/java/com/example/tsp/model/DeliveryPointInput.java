package com.example.tsp.model;

import lombok.Data;
import java.util.List;

@Data
public class DeliveryPointInput {
    private String address;
    private String earliest; // e.g. "09:00"
    private String latest;   // e.g. "11:00"
}
