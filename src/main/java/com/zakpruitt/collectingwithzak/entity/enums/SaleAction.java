package com.zakpruitt.collectingwithzak.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Triage actions on a staged sale — each is just a (status, attribution) assignment.
 */
@Getter
@RequiredArgsConstructor
public enum SaleAction {
    IGNORE(SaleStatus.IGNORED, ""),
    VINCE(SaleStatus.IGNORED, "vince"),
    UNSTAGE(SaleStatus.STAGED, "");

    private final SaleStatus targetStatus;
    private final String attributedTo;
}
