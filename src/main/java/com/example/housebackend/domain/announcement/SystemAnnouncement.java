package com.example.housebackend.domain.announcement;

import com.example.housebackend.domain.common.BaseEntity;
import com.example.housebackend.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "system_announcements")
public class SystemAnnouncement extends BaseEntity {

    @Column(nullable = false, length = 128)
    private String title;

    @Column(nullable = false, length = 2048)
    private String content;

    private boolean pinned;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
}
