package com.example.housebackend.domain.house;

import com.example.housebackend.domain.common.BaseEntity;
import com.example.housebackend.domain.common.MediaType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "house_media")
public class HouseMedia extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MediaType mediaType = MediaType.IMAGE;

    @Column(nullable = false, length = 512)
    private String url;

    @Column(length = 512)
    private String coverUrl;

    @Column(length = 256)
    private String description;

    @Column(name = "display_order")
    private Integer sortOrder = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "house_id")
    private House house;
}
