package com.zakpruitt.pbst.enums;

public enum PSAGrade {
    PSA_1("Poor"),
    PSA_2("Fair"),
    PSA_3("Very Good"),
    PSA_4("Good"),
    PSA_5("Excellent"),
    PSA_6("Excellent-Mint"),
    PSA_7("Near Mint"),
    PSA_8("Near Mint-Mint"),
    PSA_9("Mint"),
    PSA_10("Gem Mint");

    private final String description;

    PSAGrade(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
