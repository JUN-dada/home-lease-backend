package com.example.housebackend.dto;

import com.example.housebackend.domain.announcement.SystemAnnouncement;
import com.example.housebackend.domain.chat.ChatMessage;
import com.example.housebackend.domain.certification.LandlordCertification;
import com.example.housebackend.domain.contact.ContactRecord;
import com.example.housebackend.domain.common.MediaType;
import com.example.housebackend.domain.house.House;
import com.example.housebackend.domain.house.HouseMedia;
import com.example.housebackend.domain.house.HouseFavorite;
import com.example.housebackend.domain.order.RentalOrder;
import com.example.housebackend.domain.support.SupportMessage;
import com.example.housebackend.domain.support.SupportTicket;
import com.example.housebackend.domain.user.User;
import com.example.housebackend.domain.user.UserMedia;
import com.example.housebackend.dto.announcement.AnnouncementResponse;
import com.example.housebackend.dto.certification.CertificationResponse;
import com.example.housebackend.dto.chat.ChatMessageResponse;
import com.example.housebackend.dto.contact.ContactResponse;
import com.example.housebackend.dto.support.SupportMessageResponse;
import com.example.housebackend.dto.support.SupportTicketResponse;
import com.example.housebackend.dto.house.HouseMediaResponse;
import com.example.housebackend.dto.house.FavoriteResponse;
import com.example.housebackend.dto.house.HouseResponse;
import com.example.housebackend.dto.location.RegionResponse;
import com.example.housebackend.dto.location.SubwayResponse;
import com.example.housebackend.dto.order.RentalOrderResponse;
import com.example.housebackend.dto.user.UserMediaResponse;
import com.example.housebackend.dto.user.UserProfileResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

public final class DtoMapper {

    private DtoMapper() {
    }

    public static UserProfileResponse toProfile(User user) {
        List<UserMediaResponse> gallery = user.getMedia() == null ? Collections.emptyList() : user.getMedia().stream()
                .sorted(Comparator.comparing(UserMedia::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(item -> new UserMediaResponse(
                        item.getId(),
                        item.getMediaType(),
                        item.getUrl(),
                        item.getCoverUrl(),
                        item.getDescription(),
                        item.getSortOrder()))
                .collect(Collectors.toList());

        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getAvatarUrl(),
                user.getEmail(),
                user.getPhone(),
                user.getGender(),
                user.getBio(),
                user.getIdNumber(),
                user.getRole(),
                gallery);
    }

    public static HouseResponse toHouse(House house) {
        List<HouseMediaResponse> media = house.getMedia() == null ? Collections.emptyList() : house.getMedia().stream()
                .sorted(Comparator.comparing(HouseMedia::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(item -> new HouseMediaResponse(
                        item.getId(),
                        item.getMediaType(),
                        item.getUrl(),
                        item.getCoverUrl(),
                        item.getDescription(),
                        item.getSortOrder()))
                .collect(Collectors.toList());

        List<String> imageUrls = media.stream()
                .filter(item -> item.type() == MediaType.IMAGE)
                .map(HouseMediaResponse::url)
                .collect(Collectors.toList());

        User owner = house.getOwner();

        return new HouseResponse(
                house.getId(),
                house.getTitle(),
                house.getDescription(),
                house.getRentPrice(),
                house.getDeposit(),
                house.getArea(),
                house.getLayout(),
                house.getOrientation(),
                house.getAddress(),
                house.getAvailableFrom(),
                owner != null ? owner.getId() : null,
                owner != null ? owner.getFullName() : null,
                owner != null ? owner.getAvatarUrl() : null,
                owner != null ? owner.getPhone() : null,
                owner != null ? owner.getEmail() : null,
                house.getRegion() != null ? house.getRegion().getName() : null,
                house.getSubwayLine() != null ? house.getSubwayLine().getLineName() : null,
                house.getAmenities(),
                house.isRecommended(),
                house.getStatus(),
                imageUrls,
                media);
    }

    public static RentalOrderResponse toOrder(RentalOrder order) {
        User terminationRequester = order.getTerminationRequester();
        User terminationResolver = order.getTerminationResolver();
        return new RentalOrderResponse(
                order.getId(),
                order.getHouse().getId(),
                order.getHouse().getTitle(),
                order.getTenant().getFullName(),
                order.getLandlord().getFullName(),
                order.getStartDate(),
                order.getEndDate(),
                order.getMonthlyRent(),
                order.getDeposit(),
                order.getStatus(),
                order.getContractUrl(),
                order.getTerminationStatus(),
                order.getTerminationReason(),
                order.getTerminationFeedback(),
                order.getTerminationRequestedAt(),
                order.getTerminationResolvedAt(),
                terminationRequester != null ? terminationRequester.getFullName() : null,
                terminationResolver != null ? terminationResolver.getFullName() : null);
    }

    public static ContactResponse toContact(ContactRecord record) {
        return new ContactResponse(
                record.getId(),
                record.getHouse().getId(),
                record.getHouse().getTitle(),
                record.getTenant().getFullName(),
                record.getLandlord().getFullName(),
                record.getMessage(),
                record.getPreferredVisitTime(),
                record.getStatus(),
                record.getHandledAt(),
                record.getRemarks());
    }

    public static ChatMessageResponse toChatMessage(ChatMessage message) {
        User sender = message.getSender();
        ContactRecord record = message.getContactRecord();
        return new ChatMessageResponse(
                message.getId(),
                record != null ? record.getId() : null,
                sender != null ? sender.getId() : null,
                sender != null ? sender.getFullName() : null,
                sender != null ? sender.getAvatarUrl() : null,
                message.getSenderRole(),
                message.getContent(),
                listOrEmpty(message.getImageUrls()),
                message.getCreatedAt(),
                record != null && record.getHouse() != null ? record.getHouse().getId() : null,
                record != null && record.getHouse() != null ? record.getHouse().getTitle() : null,
                record != null && record.getTenant() != null ? record.getTenant().getFullName() : null,
                record != null && record.getLandlord() != null ? record.getLandlord().getFullName() : null);
    }

    public static SupportTicketResponse toSupportTicket(SupportTicket ticket) {
        return new SupportTicketResponse(
                ticket.getId(),
                ticket.getSubject(),
                ticket.getCategory(),
                ticket.getStatus(),
                ticket.getLatestMessage(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getRequester() != null ? ticket.getRequester().getId() : null,
                ticket.getRequester() != null ? ticket.getRequester().getFullName() : null,
                ticket.getHandler() != null ? ticket.getHandler().getId() : null,
                ticket.getHandler() != null ? ticket.getHandler().getFullName() : null);
    }

    public static SupportMessageResponse toSupportMessage(SupportMessage message) {
        SupportTicket ticket = message.getTicket();
        User sender = message.getSender();
        return new SupportMessageResponse(
                message.getId(),
                ticket != null ? ticket.getId() : null,
                ticket != null ? ticket.getSubject() : null,
                sender != null ? sender.getId() : null,
                sender != null ? sender.getFullName() : null,
                sender != null ? sender.getAvatarUrl() : null,
                message.getSenderRole(),
                message.getContent(),
                listOrEmpty(message.getAttachmentUrls()),
                message.getCreatedAt());
    }

    public static AnnouncementResponse toAnnouncement(SystemAnnouncement announcement) {
        return new AnnouncementResponse(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getContent(),
                announcement.isPinned(),
                announcement.getCreatedAt(),
                announcement.getCreatedBy() != null ? announcement.getCreatedBy().getFullName() : null);
    }

    public static FavoriteResponse toFavorite(HouseFavorite favorite) {
        return new FavoriteResponse(
                favorite.getId(),
                favorite.getHouse().getId(),
                favorite.getHouse().getTitle(),
                favorite.getHouse().getOwner() != null ? favorite.getHouse().getOwner().getFullName() : null);
    }

    public static CertificationResponse toCertification(LandlordCertification certification) {
        List<String> attachments = new ArrayList<>(listOrEmpty(certification.getDocumentUrls()));
        if (attachments.isEmpty() && StringUtils.hasText(certification.getDocumentUrl())) {
            attachments.add(certification.getDocumentUrl());
        }
        return new CertificationResponse(
                certification.getId(),
                certification.getStatus(),
                attachments,
                certification.getReason(),
                certification.getCreatedAt(),
                certification.getReviewedAt(),
                certification.getUser() != null ? certification.getUser().getId() : null,
                certification.getUser() != null ? certification.getUser().getFullName() : null);
    }

    public static RegionResponse toRegion(com.example.housebackend.domain.location.Region region) {
        return new RegionResponse(region.getId(), region.getName(), region.getDescription());
    }

    public static SubwayResponse toSubway(com.example.housebackend.domain.location.SubwayLine subwayLine) {
        return new SubwayResponse(
                subwayLine.getId(),
                subwayLine.getLineName(),
                subwayLine.getStationName(),
                subwayLine.getRegion() != null ? subwayLine.getRegion().getId() : null);
    }

    public static <T> List<T> listOrEmpty(List<T> list) {
        return Objects.requireNonNullElse(list, Collections.emptyList());
    }
}
