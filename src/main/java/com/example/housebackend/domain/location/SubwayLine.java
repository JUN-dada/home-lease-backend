package com.example.housebackend.domain.location;

import com.example.housebackend.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "subway_lines")
public class SubwayLine extends BaseEntity {

    @Column(nullable = false, length = 64)
    private String lineName;

    @Column(length = 64)
    private String stationName;

    @ManyToOne
    private Region region;
}
