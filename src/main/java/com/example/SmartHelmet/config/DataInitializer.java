package com.example.SmartHelmet.config;

import com.example.SmartHelmet.dto.Alert;
import com.example.SmartHelmet.dto.Member;
import com.example.SmartHelmet.dto.Pipe;
import com.example.SmartHelmet.dto.Region;
import com.example.SmartHelmet.dto.SensorReading;
import com.example.SmartHelmet.repository.AlertRepository;
import com.example.SmartHelmet.repository.MemberRepository;
import com.example.SmartHelmet.repository.PipeRepository;
import com.example.SmartHelmet.repository.RegionRepository;
import com.example.SmartHelmet.repository.SensorReadingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final PipeRepository pipeRepository;
    private final AlertRepository alertRepository;
    private final SensorReadingRepository sensorReadingRepository;
    private final RegionRepository regionRepository;
    private final PasswordEncoder passwordEncoder;
    private final MongoTemplate mongoTemplate;

    private record PipeBackfill(String address, LocalDate installedAt, String regionId) {}

    private static final Map<String, PipeBackfill> PIPE_META = Map.of(
            "강남 하수관 A-1", new PipeBackfill("서울 강남구 테헤란로 152",      LocalDate.of(2018, 3, 15),  "seoul-gangnam"),
            "마포 상수관 B-2", new PipeBackfill("서울 마포구 월드컵북로 396",    LocalDate.of(2019, 7, 22),  "seoul-mapo"),
            "해운대 하수관 C-3", new PipeBackfill("부산 해운대구 해운대해변로 264", LocalDate.of(2017, 11, 8),  "busan-haeundae"),
            "수성 상수관 D-4", new PipeBackfill("대구 수성구 동대구로 350",      LocalDate.of(2020, 5, 30),  "daegu-suseong"),
            "연수 하수관 E-5", new PipeBackfill("인천 연수구 컨벤시아대로 165",  LocalDate.of(2016, 9, 12),  "incheon-yeonsu")
    );

    private record RegionSeed(
            String id, String name, Region.Level level, String parentId,
            double centerLat, double centerLng, double zoom
    ) {}

    private static final List<RegionSeed> REGION_SEEDS = List.of(
            new RegionSeed("seoul",            "서울특별시", Region.Level.METRO,    null, 37.5665, 126.9780, 11),
            new RegionSeed("busan",            "부산광역시", Region.Level.METRO,    null, 35.1796, 129.0756, 11),
            new RegionSeed("daegu",            "대구광역시", Region.Level.METRO,    null, 35.8714, 128.6014, 11),
            new RegionSeed("incheon",          "인천광역시", Region.Level.METRO,    null, 37.4563, 126.7052, 11),
            new RegionSeed("seoul-gangnam",    "강남구",   Region.Level.DISTRICT, "seoul",   37.5172, 127.0473, 13),
            new RegionSeed("seoul-mapo",       "마포구",   Region.Level.DISTRICT, "seoul",   37.5663, 126.9019, 13),
            new RegionSeed("busan-haeundae",   "해운대구", Region.Level.DISTRICT, "busan",   35.1631, 129.1635, 13),
            new RegionSeed("daegu-suseong",    "수성구",   Region.Level.DISTRICT, "daegu",   35.8581, 128.6306, 13),
            new RegionSeed("incheon-yeonsu",   "연수구",   Region.Level.DISTRICT, "incheon", 37.4106, 126.6789, 13)
    );

    @Override
    public void run(String... args) {
        migrateLegacyMemberSchema();   // raw role/region migration first
        seedRegions();
        seedAdmin();
        seedTestAccounts();
        List<Pipe> pipes = seedPipes();
        migratePipes();
        seedAlerts(pipes);
        migrateAlerts();
        seedSensorReadings(pipes);
    }

    /** Raw MongoTemplate updates that bypass Member deserialization (old enum values would fail). */
    private void migrateLegacyMemberSchema() {
        long superCount = mongoTemplate.updateMulti(
                Query.query(Criteria.where("role").is("SUPER")),
                Update.update("role", "CENTRAL"),
                Member.class
        ).getModifiedCount();
        long regionalCount = mongoTemplate.updateMulti(
                Query.query(Criteria.where("role").is("REGIONAL")),
                Update.update("role", "METRO"),
                Member.class
        ).getModifiedCount();
        long unsetRegion = mongoTemplate.updateMulti(
                Query.query(Criteria.where("region").exists(true)),
                new Update().unset("region"),
                Member.class
        ).getModifiedCount();
        if (superCount + regionalCount + unsetRegion > 0) {
            log.info("[DataInitializer] Member schema 마이그레이션: SUPER→CENTRAL {}, REGIONAL→METRO {}, region 필드 제거 {}",
                    superCount, regionalCount, unsetRegion);
        }
    }

    private void seedRegions() {
        int created = 0;
        for (RegionSeed s : REGION_SEEDS) {
            if (regionRepository.existsById(s.id())) continue;
            regionRepository.save(Region.builder()
                    .id(s.id())
                    .name(s.name())
                    .level(s.level())
                    .parentId(s.parentId())
                    .centerLat(s.centerLat())
                    .centerLng(s.centerLng())
                    .zoom(s.zoom())
                    .build());
            created++;
        }
        if (created > 0) {
            log.info("[DataInitializer] Region {}건 생성 (총 {}건 중)", created, REGION_SEEDS.size());
        } else {
            log.info("[DataInitializer] Region 데이터 이미 존재 — 스킵");
        }
    }

    private void seedAdmin() {
        if (memberRepository.existsByUsername("admin")) {
            log.info("[DataInitializer] admin 계정 이미 존재 — 스킵");
            return;
        }
        memberRepository.save(Member.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .role(Member.Role.CENTRAL)
                .primaryRegionId(null)
                .build());
        log.info("[DataInitializer] admin 계정 생성 (CENTRAL)");
    }

    private void seedTestAccounts() {
        seedAccount("seoul",    "seoul123",   Member.Role.METRO,    "seoul");
        seedAccount("haeundae", "hd123",      Member.Role.DISTRICT, "busan-haeundae");
        seedAccount("citizen",  "citizen123", Member.Role.CITIZEN,  null);
    }

    private void seedAccount(String username, String password, Member.Role role, String regionId) {
        if (memberRepository.existsByUsername(username)) {
            log.info("[DataInitializer] {} 계정 이미 존재 — 스킵", username);
            return;
        }
        memberRepository.save(Member.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .role(role)
                .primaryRegionId(regionId)
                .build());
        log.info("[DataInitializer] {} 계정 생성 (role={}, regionId={})", username, role, regionId);
    }

    private List<Pipe> seedPipes() {
        if (pipeRepository.count() > 0) {
            log.info("[DataInitializer] Pipe 데이터 이미 존재 — 시드 스킵 (필요시 backfill 진행)");
            return pipeRepository.findAll();
        }

        LocalDateTime now = LocalDateTime.now();
        List<Pipe> pipes = List.of(
                buildPipe("강남 하수관 A-1",   "서울 강남구",   37.4979, 127.0276, Pipe.Status.NORMAL,  12.3, now),
                buildPipe("마포 상수관 B-2",   "서울 마포구",   37.5663, 126.9019, Pipe.Status.CAUTION, 45.7, now),
                buildPipe("해운대 하수관 C-3", "부산 해운대구", 35.1631, 129.1635, Pipe.Status.DANGER,  82.1, now),
                buildPipe("수성 상수관 D-4",   "대구 수성구",   35.8583, 128.6311, Pipe.Status.NORMAL,  18.9, now),
                buildPipe("연수 하수관 E-5",   "인천 연수구",   37.4106, 126.6780, Pipe.Status.CAUTION, 51.4, now)
        );
        List<Pipe> saved = pipeRepository.saveAll(pipes);
        log.info("[DataInitializer] Pipe {}건 생성", saved.size());
        return saved;
    }

    private Pipe buildPipe(String name, String region, double lat, double lng,
                           Pipe.Status status, double sensorValue, LocalDateTime updatedAt) {
        PipeBackfill meta = PIPE_META.get(name);
        return Pipe.builder()
                .name(name)
                .region(region)
                .regionId(meta == null ? null : meta.regionId())
                .address(meta == null ? null : meta.address())
                .lat(lat).lng(lng)
                .status(status)
                .sensorValue(sensorValue)
                .installedAt(meta == null ? null : meta.installedAt())
                .updatedAt(updatedAt)
                .build();
    }

    private void migratePipes() {
        List<Pipe> all = pipeRepository.findAll();
        int updated = 0;
        for (Pipe p : all) {
            PipeBackfill meta = PIPE_META.get(p.getName());
            if (meta == null) continue;
            boolean dirty = false;
            if (p.getAddress() == null) {
                p.setAddress(meta.address());
                dirty = true;
            }
            if (p.getInstalledAt() == null) {
                p.setInstalledAt(meta.installedAt());
                dirty = true;
            }
            if (p.getRegionId() == null) {
                p.setRegionId(meta.regionId());
                dirty = true;
            }
            if (dirty) {
                pipeRepository.save(p);
                updated++;
            }
        }
        if (updated > 0) {
            log.info("[DataInitializer] Pipe {}건 backfill (address/installedAt/regionId)", updated);
        } else {
            log.info("[DataInitializer] Pipe backfill 대상 없음");
        }
    }

    private void seedAlerts(List<Pipe> pipes) {
        if (alertRepository.count() > 0) {
            log.info("[DataInitializer] Alert 데이터 이미 존재 — 시드 스킵 (필요시 backfill 진행)");
            return;
        }
        if (pipes.isEmpty()) return;

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        Pipe.Status[] statuses = { Pipe.Status.CAUTION, Pipe.Status.DANGER };

        for (int i = 0; i < 10; i++) {
            Pipe pipe = pipes.get(rnd.nextInt(pipes.size()));
            Pipe.Status status = statuses[rnd.nextInt(statuses.length)];
            String message = (status == Pipe.Status.DANGER)
                    ? "[위험] " + pipe.getName() + " 센서값 임계치 초과"
                    : "[주의] " + pipe.getName() + " 수치 상승 감지";

            Alert alert = Alert.builder()
                    .pipeId(pipe.getId())
                    .pipeName(pipe.getName())
                    .regionId(pipe.getRegionId())
                    .status(status)
                    .message(message)
                    .createdAt(LocalDateTime.now().minusMinutes(i * 15L))
                    .build();
            alertRepository.save(alert);
        }
        log.info("[DataInitializer] Alert 10건 생성");
    }

    private void migrateAlerts() {
        Map<String, Pipe> pipeById = new HashMap<>();
        for (Pipe p : pipeRepository.findAll()) {
            pipeById.put(p.getId(), p);
        }

        int updated = 0;
        for (Alert a : alertRepository.findAll()) {
            Pipe pipe = pipeById.get(a.getPipeId());
            if (pipe == null) continue;
            boolean dirty = false;
            if (a.getPipeName() == null) {
                a.setPipeName(pipe.getName());
                dirty = true;
            }
            if (a.getRegionId() == null && pipe.getRegionId() != null) {
                a.setRegionId(pipe.getRegionId());
                dirty = true;
            }
            if (dirty) {
                alertRepository.save(a);
                updated++;
            }
        }
        if (updated > 0) {
            log.info("[DataInitializer] Alert {}건 backfill (pipeName/regionId)", updated);
        } else {
            log.info("[DataInitializer] Alert backfill 대상 없음");
        }
    }

    private void seedSensorReadings(List<Pipe> pipes) {
        if (sensorReadingRepository.count() > 0) {
            log.info("[DataInitializer] SensorReading 데이터 이미 존재 — 스킵");
            return;
        }
        if (pipes.isEmpty()) return;

        LocalDateTime now = LocalDateTime.now();
        Random rnd = new Random(42);
        int count = 0;

        for (Pipe pipe : pipes) {
            for (int i = 23; i >= 0; i--) {
                double progress = (24.0 - i) / 24.0;
                LocalDateTime t = now.minusHours(i);

                SensorReading r = SensorReading.builder()
                        .pipeId(pipe.getId())
                        .timestamp(t)
                        .displacement(value(pipe.getStatus(), progress, 1.2, 4.5, 10.5, 0.6, rnd))
                        .pressure   (value(pipe.getStatus(), progress, 1.7, 4.5, 8.0,  0.4, rnd))
                        .moisture   (value(pipe.getStatus(), progress, 37,  62,  85,   3.0, rnd))
                        .vibration  (value(pipe.getStatus(), progress, 8,   24,  46,   2.5, rnd))
                        .build();
                sensorReadingRepository.save(r);
                count++;
            }
        }
        log.info("[DataInitializer] SensorReading {}건 생성 (파이프 {}개 × 24h)", count, pipes.size());
    }

    private double value(Pipe.Status status, double progress,
                         double normalMid, double cautionMid, double dangerMid,
                         double noise, Random rnd) {
        double mid;
        double trendFactor;
        switch (status) {
            case DANGER  -> { mid = dangerMid;  trendFactor = 0.30; }
            case CAUTION -> { mid = cautionMid; trendFactor = 0.20; }
            default      -> { mid = normalMid;  trendFactor = 0.00; }
        }
        double startOffset = -mid * trendFactor / 2.0;
        double trend = mid * trendFactor * progress;
        double v = mid + startOffset + trend + rnd.nextGaussian() * noise;
        return Math.round(v * 10.0) / 10.0;
    }
}
