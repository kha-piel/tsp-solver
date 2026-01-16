package com.example.tsp.model;

import lombok.Data;
import java.util.List;

@Data
public class DeliveryPointInput {
    private String address;
    private String earliest;
    private String latest;
}
