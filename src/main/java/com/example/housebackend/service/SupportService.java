package com.example.housebackend.service;

import com.example.housebackend.domain.support.SupportMessage;
import com.example.housebackend.domain.support.SupportTicket;
import com.example.housebackend.domain.support.SupportTicketStatus;
import com.example.housebackend.domain.user.User;
import com.example.housebackend.domain.user.UserRole;
import com.example.housebackend.dto.DtoMapper;
import com.example.housebackend.exception.AccessDeniedException;
import com.example.housebackend.exception.BadRequestException;
import com.example.housebackend.exception.ResourceNotFoundException;
import com.example.housebackend.repository.SupportMessageRepository;
import com.example.housebackend.repository.SupportTicketRepository;
import com.example.housebackend.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SupportService {

    private final SupportTicketRepository ticketRepository;
    private final SupportMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public SupportService(SupportTicketRepository ticketRepository,
                          SupportMessageRepository messageRepository,
                          UserRepository userRepository,
                          SimpMessagingTemplate messagingTemplate) {
        this.ticketRepository = ticketRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public SupportTicket createTicket(User requester, String subject, String category, String message) {
        if (!StringUtils.hasText(subject)) {
            throw new BadRequestException("请输入工单主题");
        }
        SupportTicket ticket = new SupportTicket();
        ticket.setRequester(userRepository.getReferenceById(requester.getId()));
        ticket.setSubject(subject.trim());
        ticket.setCategory(StringUtils.hasText(category) ? category.trim() : null);
        ticket.setStatus(SupportTicketStatus.OPEN);
        ticket.setLatestMessage(summarize(message));
        SupportTicket savedTicket = ticketRepository.save(ticket);
        if (StringUtils.hasText(message)) {
            SupportMessage first = buildMessage(savedTicket, requester, message, List.of());
            messageRepository.save(first);
        }
        notifyAdmins(savedTicket);
        return savedTicket;
    }

    @Transactional
    public SupportMessage sendMessage(Long ticketId, User sender, String content, List<String> attachments) {
        if (!StringUtils.hasText(content) && (attachments == null || attachments.isEmpty())) {
            throw new BadRequestException("消息内容不能为空");
        }
        SupportTicket ticket = requireTicket(ticketId);
        ensureParticipant(ticket, sender);
        if (sender.getRole() == UserRole.ADMIN && ticket.getHandler() == null) {
            ticket.setHandler(userRepository.getReferenceById(sender.getId()));
            ticket.setStatus(SupportTicketStatus.IN_PROGRESS);
        }
        SupportMessage message = buildMessage(ticket, sender, content, attachments);
        ticket.setLatestMessage(summarize(content));
        if (ticket.getStatus() == SupportTicketStatus.RESOLVED || ticket.getStatus() == SupportTicketStatus.CLOSED) {
            ticket.setStatus(SupportTicketStatus.IN_PROGRESS);
            ticket.setClosedAt(null);
        }
        SupportMessage saved = messageRepository.save(message);
        ticketRepository.save(ticket);
        if (sender.getRole() != UserRole.ADMIN) {
            notifyAdmins(ticket);
        }
        return messageRepository.findWithRelationsById(saved.getId()).orElse(saved);
    }

    @Transactional(readOnly = true)
    public Page<SupportTicket> listForUser(Long userId, Pageable pageable) {
        return ticketRepository.findByRequesterId(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<SupportTicket> listForAdmin(SupportTicketStatus status, Pageable pageable) {
        if (status != null) {
            return ticketRepository.findByStatus(status, pageable);
        }
        return ticketRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<SupportMessage> listMessages(Long ticketId, User user, Pageable pageable) {
        SupportTicket ticket = requireTicket(ticketId);
        ensureParticipant(ticket, user);
        return messageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId, pageable);
    }

    @Transactional
    public SupportTicket updateStatus(Long ticketId, SupportTicketStatus status, User actor) {
        SupportTicket ticket = requireTicket(ticketId);
        ensureParticipant(ticket, actor);
        ticket.setStatus(status);
        if (status == SupportTicketStatus.RESOLVED || status == SupportTicketStatus.CLOSED) {
            ticket.setClosedAt(Instant.now());
        } else {
            ticket.setClosedAt(null);
        }
        SupportTicket saved = ticketRepository.save(ticket);
        notifyTicketParticipants(saved);
        return saved;
    }

    private SupportTicket requireTicket(Long ticketId) {
        return ticketRepository.findWithParticipantsById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("工单不存在"));
    }

    private void ensureParticipant(SupportTicket ticket, User user) {
        if (user.getRole() == UserRole.ADMIN) {
            return;
        }
        Long userId = user.getId();
        if (ticket.getRequester() != null && ticket.getRequester().getId().equals(userId)) {
            return;
        }
        if (ticket.getHandler() != null && ticket.getHandler().getId().equals(userId)) {
            return;
        }
        throw new AccessDeniedException("无权访问该工单");
    }

    private SupportMessage buildMessage(SupportTicket ticket, User sender, String content, List<String> attachments) {
        SupportMessage message = new SupportMessage();
        message.setTicket(ticket);
        message.setSender(userRepository.getReferenceById(sender.getId()));
        message.setSenderRole(sender.getRole());
        message.setContent(StringUtils.hasText(content) ? content.trim() : null);
        message.setAttachmentUrls(sanitizeAttachments(attachments));
        return message;
    }

    private List<String> sanitizeAttachments(List<String> attachments) {
        if (attachments == null) {
            return new ArrayList<>();
        }
        List<String> sanitized = new ArrayList<>();
        for (String url : attachments) {
            if (StringUtils.hasText(url)) {
                sanitized.add(url.trim());
            }
        }
        return sanitized;
    }

    private String summarize(String message) {
        if (!StringUtils.hasText(message)) {
            return null;
        }
        String trimmed = message.trim();
        return trimmed.length() > 60 ? trimmed.substring(0, 60) + "…" : trimmed;
    }

    private void notifyAdmins(SupportTicket ticket) {
        if (messagingTemplate != null) {
            messagingTemplate.convertAndSend("/topic/admin/support", DtoMapper.toSupportTicket(ticket));
        }
    }

    private void notifyTicketParticipants(SupportTicket ticket) {
        if (messagingTemplate == null) {
            return;
        }
        var ticketResponse = DtoMapper.toSupportTicket(ticket);
        if (ticket.getRequester() != null) {
            messagingTemplate.convertAndSend("/topic/users/" + ticket.getRequester().getId(), ticketResponse);
        }
        if (ticket.getHandler() != null) {
            messagingTemplate.convertAndSend("/topic/users/" + ticket.getHandler().getId(), ticketResponse);
        }
    }
}
