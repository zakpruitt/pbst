package com.zakpruitt.collectingwithzak.dto.common;

public record LabeledStat(String label, long count) {

    /**
     * JPQL constructor-expression entry point — enums and other label types arrive as Object.
     */
    public LabeledStat(Object label, long count) {
        this(label.toString(), count);
    }
}
