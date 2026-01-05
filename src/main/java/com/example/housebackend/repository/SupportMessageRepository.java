package com.example.housebackend.repository;

import com.example.housebackend.domain.support.SupportMessage;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportMessageRepository extends JpaRepository<SupportMessage, Long> {

    @EntityGraph(attributePaths = {"sender", "ticket", "ticket.requester", "ticket.handler"})
    Page<SupportMessage> findByTicketIdOrderByCreatedAtAsc(Long ticketId, Pageable pageable);

    @EntityGraph(attributePaths = {"sender", "ticket", "ticket.requester", "ticket.handler"})
    Optional<SupportMessage> findWithRelationsById(Long id);
}
