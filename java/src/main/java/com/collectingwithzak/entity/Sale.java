package com.collectingwithzak.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales")
@Getter
@Setter
public class Sale extends BaseEntity {

    @Column(name = "ebay_order_id", unique = true)
    private String ebayOrderId;

    @Column(name = "sale_date")
    private LocalDate saleDate;

    private String title;

    @Column(name = "buyer_username")
    private String buyerUsername;

    @Column(name = "gross_amount")
    private double grossAmount;

    @Column(name = "ebay_fees")
    private double ebayFees;

    @Column(name = "shipping_cost")
    private double shippingCost;

    @Column(name = "net_amount")
    private double netAmount;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "order_status")
    private String orderStatus;

    private String origin;

    private String status;

    @Column(name = "attributed_to")
    private String attributedTo;

    private String notes;

    @OneToMany(mappedBy = "sale", fetch = FetchType.LAZY)
    private List<TrackedItem> items = new ArrayList<>();
}
