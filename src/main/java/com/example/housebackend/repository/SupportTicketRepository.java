package com.example.housebackend.repository;

import com.example.housebackend.domain.support.SupportTicket;
import com.example.housebackend.domain.support.SupportTicketStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    Page<SupportTicket> findByRequesterId(Long requesterId, Pageable pageable);

    Page<SupportTicket> findByStatus(SupportTicketStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"requester", "handler"})
    Optional<SupportTicket> findWithParticipantsById(Long id);
}
