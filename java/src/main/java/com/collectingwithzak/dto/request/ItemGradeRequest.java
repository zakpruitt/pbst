package com.collectingwithzak.dto.request;

import lombok.Data;

@Data
public class ItemGradeRequest {
    private Long itemId;
    private String grade;
    private double upcharge;
}
