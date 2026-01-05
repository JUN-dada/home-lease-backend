package com.example.housebackend.config;

import com.example.housebackend.domain.announcement.SystemAnnouncement;
import com.example.housebackend.domain.certification.LandlordCertification;
import com.example.housebackend.domain.certification.LandlordCertificationStatus;
import com.example.housebackend.domain.house.House;
import com.example.housebackend.domain.house.HouseStatus;
import com.example.housebackend.domain.location.Region;
import com.example.housebackend.domain.location.SubwayLine;
import com.example.housebackend.domain.order.RentalOrder;
import com.example.housebackend.domain.order.RentalOrderStatus;
import com.example.housebackend.domain.user.User;
import com.example.housebackend.domain.user.UserRole;
import com.example.housebackend.domain.user.UserStatus;
import com.example.housebackend.repository.HouseRepository;
import com.example.housebackend.repository.RentalOrderRepository;
import com.example.housebackend.repository.RegionRepository;
import com.example.housebackend.repository.SubwayLineRepository;
import com.example.housebackend.repository.SystemAnnouncementRepository;
import com.example.housebackend.repository.UserRepository;
import com.example.housebackend.repository.LandlordCertificationRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RegionRepository regionRepository;
    private final SubwayLineRepository subwayLineRepository;
    private final HouseRepository houseRepository;
    private final SystemAnnouncementRepository announcementRepository;
    private final RentalOrderRepository rentalOrderRepository;
    private final LandlordCertificationRepository certificationRepository;

    @Override
    public void run(String... args) {
        User superAdmin = ensureUser("superadmin", "123456", "系统管理员", "18800000000", UserRole.ADMIN,
                "负责整个平台的配置管理。", "https://placehold.co/128x128/111827/FFFFFF?text=Admin");
        User landlord = ensureUser("landlord", "123456", "张房东", "18800000001", UserRole.LANDLORD,
                "热爱分享品质生活的资深房东。", "https://placehold.co/128x128/1d4ed8/FFFFFF?text=L");
        User tenant = ensureUser("tenant", "123456", "李租客", "18800000002", UserRole.USER,
                "喜欢靠近地铁口的精装小屋。", "https://placehold.co/128x128/0f766e/FFFFFF?text=U");

        ensureCertification(landlord, superAdmin);
        Map<String, Region> regions = ensureRegions();
        Map<String, SubwayLine> subwayLines = ensureSubways(regions);
        List<House> houses = ensureHouses(landlord, regions, subwayLines);
        ensureAnnouncements(superAdmin);
        ensureOrders(tenant, houses);
    }

    private User ensureUser(String username,
                            String rawPassword,
                            String fullName,
                            String phone,
                            UserRole role,
                            String bio,
                            String avatarUrl) {
        return userRepository.findByUsername(username)
                .orElseGet(() -> {
                    User user = new User();
                    user.setUsername(username);
                    user.setPassword(passwordEncoder.encode(rawPassword));
                    user.setFullName(fullName);
                    user.setPhone(phone);
                    user.setRole(role);
                    user.setStatus(UserStatus.ACTIVE);
                    user.setBio(bio);
                    user.setAvatarUrl(avatarUrl);
                    user.setEmail(username + "@demo.local");
                    return userRepository.save(user);
                });
    }

    private Map<String, Region> ensureRegions() {
        Map<String, Region> cache = new HashMap<>();
        cache.put("市中心", regionRepository.findByName("市中心").orElseGet(() -> {
            Region region = new Region();
            region.setName("市中心");
            region.setDescription("商务地标与购物中心聚集的核心区域");
            return regionRepository.save(region);
        }));
        cache.put("高新区", regionRepository.findByName("高新区").orElseGet(() -> {
            Region region = new Region();
            region.setName("高新区");
            region.setDescription("互联网公司云集，配套完善的新兴社区");
            return regionRepository.save(region);
        }));
        cache.put("临河区", regionRepository.findByName("临河区").orElseGet(() -> {
            Region region = new Region();
            region.setName("临河区");
            region.setDescription("靠近城市绿廊与滨江公园的宜居板块");
            return regionRepository.save(region);
        }));
        return cache;
    }

    private Map<String, SubwayLine> ensureSubways(Map<String, Region> regions) {
        Map<String, SubwayLine> cache = new HashMap<>();
        cache.put("1号线", findOrCreateSubway("1号线", "人民广场站", regions.get("市中心")));
        cache.put("2号线", findOrCreateSubway("2号线", "软件园站", regions.get("高新区")));
        cache.put("5号线", findOrCreateSubway("5号线", "滨江公园站", regions.get("临河区")));
        return cache;
    }

    private SubwayLine findOrCreateSubway(String lineName, String stationName, Region region) {
        Optional<SubwayLine> existing = subwayLineRepository.findAll().stream()
                .filter(line -> lineName.equals(line.getLineName()) && stationName.equals(line.getStationName()))
                .findFirst();
        if (existing.isPresent()) {
            return existing.get();
        }
        SubwayLine line = new SubwayLine();
        line.setLineName(lineName);
        line.setStationName(stationName);
        line.setRegion(region);
        return subwayLineRepository.save(line);
    }

    private List<House> ensureHouses(User landlord,
                                     Map<String, Region> regions,
                                     Map<String, SubwayLine> subwayLines) {
        if (houseRepository.count() > 0) {
            return houseRepository.findAll();
        }

        House cityLoft = new House();
        cityLoft.setTitle("市中心景观LOFT");
        cityLoft.setDescription("双层挑高、落地窗、步行 5 分钟到人民广场，适合年轻白领。");
        cityLoft.setRentPrice(new BigDecimal("5200"));
        cityLoft.setArea(65.0);
        cityLoft.setLayout("1室1厅1卫");
        cityLoft.setOrientation("南北通透");
        cityLoft.setAddress("人民路 88 号时代广场 A 座");
        cityLoft.setAvailableFrom(LocalDate.now().plusDays(3));
        cityLoft.setOwner(landlord);
        cityLoft.setRegion(regions.get("市中心"));
        cityLoft.setSubwayLine(subwayLines.get("1号线"));
        cityLoft.setStatus(HouseStatus.PUBLISHED);
        cityLoft.setRecommended(true);
        cityLoft.setAmenities(new HashSet<>(List.of("中央空调", "地暖", "智能门锁")));

        House techPark = new House();
        techPark.setTitle("高新区精装两居室");
        techPark.setDescription("全屋智能家居，带共享办公室，适合程序员合租。");
        techPark.setRentPrice(new BigDecimal("6800"));
        techPark.setArea(89.0);
        techPark.setLayout("2室2厅1卫");
        techPark.setOrientation("朝南");
        techPark.setAddress("软件园一路 66 号云谷公寓");
        techPark.setAvailableFrom(LocalDate.now().plusWeeks(1));
        techPark.setOwner(landlord);
        techPark.setRegion(regions.get("高新区"));
        techPark.setSubwayLine(subwayLines.get("2号线"));
        techPark.setStatus(HouseStatus.PUBLISHED);
        techPark.setAmenities(new HashSet<>(List.of("智能门锁", "独立书房", "共享健身房")));

        House riverSide = new House();
        riverSide.setTitle("滨江花园三居室");
        riverSide.setDescription("南向江景，带超大阳台与储物间，适合一家三口长期居住。");
        riverSide.setRentPrice(new BigDecimal("7800"));
        riverSide.setArea(112.0);
        riverSide.setLayout("3室2厅2卫");
        riverSide.setOrientation("朝南");
        riverSide.setAddress("滨江花园 3 期 12 栋");
        riverSide.setAvailableFrom(LocalDate.now().plusDays(10));
        riverSide.setOwner(landlord);
        riverSide.setRegion(regions.get("临河区"));
        riverSide.setSubwayLine(subwayLines.get("5号线"));
        riverSide.setStatus(HouseStatus.PUBLISHED);
        riverSide.setAmenities(new HashSet<>(List.of("江景阳台", "车位", "全屋家电")));

        return houseRepository.saveAll(List.of(cityLoft, techPark, riverSide));
    }

    private void ensureAnnouncements(User admin) {
        if (announcementRepository.count() > 0) {
            return;
        }
        SystemAnnouncement welcome = new SystemAnnouncement();
        welcome.setTitle("欢迎体验智慧租房平台");
        welcome.setContent("系统已预置演示数据，可使用 superadmin / 123456 登录后台。");
        welcome.setPinned(true);
        welcome.setCreatedBy(admin);

        SystemAnnouncement maintenance = new SystemAnnouncement();
        maintenance.setTitle("周末例行维护通知");
        maintenance.setContent("本周日 02:00-04:00 将升级数据库，期间服务可能短暂波动。");
        maintenance.setPinned(false);
        maintenance.setCreatedBy(admin);

        announcementRepository.saveAll(List.of(welcome, maintenance));
    }

    private void ensureOrders(User tenant, List<House> houses) {
        if (houses.isEmpty() || rentalOrderRepository.count() > 0) {
            return;
        }
        House loft = houses.get(0);
        House techPark = houses.size() > 1 ? houses.get(1) : loft;

        RentalOrder pending = new RentalOrder();
        pending.setHouse(loft);
        pending.setTenant(tenant);
        pending.setLandlord(loft.getOwner());
        pending.setStartDate(LocalDate.now().plusDays(15));
        pending.setEndDate(LocalDate.now().plusMonths(12));
        pending.setMonthlyRent(loft.getRentPrice());
        pending.setDeposit(loft.getRentPrice());
        pending.setStatus(RentalOrderStatus.PENDING);

        RentalOrder active = new RentalOrder();
        active.setHouse(techPark);
        active.setTenant(tenant);
        active.setLandlord(techPark.getOwner());
        active.setStartDate(LocalDate.now().minusMonths(1));
        active.setEndDate(LocalDate.now().plusMonths(11));
        active.setMonthlyRent(techPark.getRentPrice());
        active.setDeposit(techPark.getRentPrice());
        active.setStatus(RentalOrderStatus.ACTIVE);
        active.setConfirmedAt(LocalDate.now().minusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        active.setContractUrl("https://example.com/contracts/demo.pdf");

        rentalOrderRepository.saveAll(List.of(pending, active));
    }

    private void ensureCertification(User landlord, User admin) {
        boolean hasApproved = certificationRepository.findFirstByUserIdOrderByCreatedAtDesc(landlord.getId())
                .map(certification -> certification.getStatus() == LandlordCertificationStatus.APPROVED)
                .orElse(false);
        if (hasApproved) {
            return;
        }
        LandlordCertification certification = new LandlordCertification();
        certification.setUser(landlord);
        certification.setDocumentUrl("https://example.com/docs/landlord-cert.pdf");
        certification.setReason("演示环境自动认证");
        certification.setStatus(LandlordCertificationStatus.APPROVED);
        certification.setReviewedAt(java.time.Instant.now());
        certification.setReviewedBy(admin);
        certificationRepository.save(certification);
    }
}
