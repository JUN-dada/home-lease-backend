package com.example.housebackend.domain.chat;

import com.example.housebackend.domain.common.BaseEntity;
import com.example.housebackend.domain.contact.ContactRecord;
import com.example.housebackend.domain.user.User;
import com.example.housebackend.domain.user.UserRole;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "chat_messages")
public class ChatMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id", nullable = false)
    private ContactRecord contactRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private UserRole senderRole;

    @Column(length = 1024)
    private String content;

    @ElementCollection
    @CollectionTable(name = "chat_message_images", joinColumns = @JoinColumn(name = "message_id"))
    @Column(name = "image_url", length = 256)
    private List<String> imageUrls = new ArrayList<>();
}
