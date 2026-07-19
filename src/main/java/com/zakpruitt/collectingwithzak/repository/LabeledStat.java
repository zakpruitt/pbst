package com.zakpruitt.collectingwithzak.repository;

public record LabeledStat(String label, long count) {

    public LabeledStat(Object label, long count) {
        this(label.toString(), count);
    }
}
