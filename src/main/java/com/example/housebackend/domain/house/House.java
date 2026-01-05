package com.example.housebackend.domain.house;

import com.example.housebackend.domain.common.BaseEntity;
import com.example.housebackend.domain.location.Region;
import com.example.housebackend.domain.location.SubwayLine;
import com.example.housebackend.domain.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "houses")
public class House extends BaseEntity {

    @Column(nullable = false, length = 128)
    private String title;

    @Column(length = 1024)
    private String description;

    @Column(nullable = false)
    private BigDecimal rentPrice;

    @Column
    private BigDecimal deposit;

    private Double area;

    @Column(length = 32)
    private String layout;

    @Column(length = 32)
    private String orientation;

    @Column(length = 128)
    private String address;

    private LocalDate availableFrom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    private Region region;

    @ManyToOne(fetch = FetchType.LAZY)
    private SubwayLine subwayLine;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private HouseStatus status = HouseStatus.DRAFT;

    private boolean recommended;

    @OneToMany(mappedBy = "house", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HouseMedia> media = new ArrayList<>();

    @ElementCollection
    private Set<String> amenities = new HashSet<>();

    public void replaceMedia(List<HouseMedia> items) {
        media.clear();
        if (items == null) {
            return;
        }
        for (HouseMedia item : items) {
            item.setHouse(this);
            media.add(item);
        }
    }
}
