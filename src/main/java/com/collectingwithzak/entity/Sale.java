package com.collectingwithzak.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales")
@Getter
@Setter
@SQLDelete(sql = "UPDATE sales SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Sale extends BaseEntity {

    @Column(name = "ebay_order_id", unique = true)
    private String ebayOrderId;

    @Column(name = "sale_date")
    private LocalDate saleDate;

    private String title;

    @Column(name = "buyer_username")
    private String buyerUsername;

    @Column(name = "gross_amount", columnDefinition = "numeric(10,2)")
    private double grossAmount;

    @Column(name = "ebay_fees", columnDefinition = "numeric(10,2)")
    private double ebayFees;

    @Column(name = "shipping_cost", columnDefinition = "numeric(10,2)")
    private double shippingCost;

    @Column(name = "net_amount", columnDefinition = "numeric(10,2)")
    private double netAmount;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "order_status")
    private String orderStatus;

    private String origin = "EBAY";

    private String status = "STAGED";

    @Column(name = "attributed_to")
    private String attributedTo;

    private String notes;

    @OneToMany(mappedBy = "sale", fetch = FetchType.LAZY)
    private List<TrackedItem> items = new ArrayList<>();
}
