package com.collectingwithzak.dto.common;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LabeledStat {
    private String label;
    private long count;

    public LabeledStat(Object label, long count) {
        this.label = label.toString();
        this.count = count;
    }
}
