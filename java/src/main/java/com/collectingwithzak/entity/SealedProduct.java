package com.collectingwithzak.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sealed_products")
@Getter
@Setter
@SoftDelete(columnName = "deleted_at")
public class SealedProduct {

    @Id
    private String id;

    private String name;

    @Column(name = "set_code")
    private String setCode;

    @Column(name = "set_name")
    private String setName;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "market_price")
    private double marketPrice;

    @Column(name = "low_price")
    private double lowPrice;

    @Column(name = "last_price_sync")
    private LocalDateTime lastPriceSync;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
