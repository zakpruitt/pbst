package com.zakpruitt.pbst.dtos.pokewallet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaginationDto {
    private int page;
    private int total_pages;
}