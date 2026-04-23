package com.collectingwithzak.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sealed_products")
@Getter
@Setter
@SQLDelete(sql = "UPDATE sealed_products SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
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

    @Column(name = "market_price", columnDefinition = "numeric(10,2)")
    private double marketPrice;

    @Column(name = "low_price", columnDefinition = "numeric(10,2)")
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
