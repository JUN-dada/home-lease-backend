package com.example.housebackend.service;

import com.example.housebackend.domain.chat.ChatMessage;
import com.example.housebackend.domain.contact.ContactRecord;
import com.example.housebackend.domain.user.User;
import com.example.housebackend.domain.user.UserRole;
import com.example.housebackend.exception.AccessDeniedException;
import com.example.housebackend.exception.BadRequestException;
import com.example.housebackend.exception.ResourceNotFoundException;
import com.example.housebackend.repository.ChatMessageRepository;
import com.example.housebackend.repository.ContactRecordRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ContactRecordRepository contactRecordRepository;

    @Transactional
    public ChatMessage sendMessage(Long contactId, User sender, String content, List<String> rawImageUrls) {
        ContactRecord record = requireContact(contactId);
        ensureParticipant(record, sender);
        if (!StringUtils.hasText(content) && (rawImageUrls == null || rawImageUrls.stream().noneMatch(StringUtils::hasText))) {
            throw new BadRequestException("消息内容不能为空");
        }
        ChatMessage message = new ChatMessage();
        message.setContactRecord(record);
        message.setSender(sender);
        message.setSenderRole(sender.getRole());
        message.setContent(StringUtils.hasText(content) ? content.trim() : null);
        message.setImageUrls(sanitizeImageUrls(rawImageUrls));
        return chatMessageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public Page<ChatMessage> listMessages(Long contactId, User requester, Pageable pageable) {
        ContactRecord record = requireContact(contactId);
        ensureParticipant(record, requester);
        return chatMessageRepository.findByContactRecordId(contactId, pageable);
    }

    private ContactRecord requireContact(Long contactId) {
        return contactRecordRepository.findWithRelationsById(contactId)
                .orElseThrow(() -> new ResourceNotFoundException("联系记录不存在"));
    }

    private void ensureParticipant(ContactRecord record, User user) {
        if (user.getRole() == UserRole.ADMIN) {
            return;
        }
        Long userId = user.getId();
        if (record.getTenant() != null && record.getTenant().getId().equals(userId)) {
            return;
        }
        if (record.getLandlord() != null && record.getLandlord().getId().equals(userId)) {
            return;
        }
        throw new AccessDeniedException("无权访问该聊天");
    }

    private List<String> sanitizeImageUrls(List<String> imageUrls) {
        if (imageUrls == null) {
            return new ArrayList<>();
        }
        List<String> sanitized = new ArrayList<>();
        for (String url : imageUrls) {
            if (StringUtils.hasText(url)) {
                sanitized.add(url.trim());
            }
        }
        return sanitized;
    }
}
