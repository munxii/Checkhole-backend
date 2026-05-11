package com.example.SmartHelmet.config;

import com.example.SmartHelmet.dto.Alert;
import com.example.SmartHelmet.dto.Member;
import com.example.SmartHelmet.dto.Pipe;
import com.example.SmartHelmet.repository.AlertRepository;
import com.example.SmartHelmet.repository.MemberRepository;
import com.example.SmartHelmet.repository.PipeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final PipeRepository pipeRepository;
    private final AlertRepository alertRepository;
    private final PasswordEncoder passwordEncoder;

    private record PipeBackfill(String address, LocalDate installedAt) {}

    private static final Map<String, PipeBackfill> PIPE_META = Map.of(
            "강남 하수관 A-1", new PipeBackfill("서울 강남구 테헤란로 152", LocalDate.of(2018, 3, 15)),
            "마포 상수관 B-2", new PipeBackfill("서울 마포구 월드컵북로 396", LocalDate.of(2019, 7, 22)),
            "해운대 하수관 C-3", new PipeBackfill("부산 해운대구 해운대해변로 264", LocalDate.of(2017, 11, 8)),
            "수성 상수관 D-4", new PipeBackfill("대구 수성구 동대구로 350", LocalDate.of(2020, 5, 30)),
            "연수 하수관 E-5", new PipeBackfill("인천 연수구 컨벤시아대로 165", LocalDate.of(2016, 9, 12))
    );

    @Override
    public void run(String... args) {
        seedAdmin();
        List<Pipe> pipes = seedPipes();
        migratePipes();
        seedAlerts(pipes);
        migrateAlerts();
    }

    private void seedAdmin() {
        if (memberRepository.existsByUsername("admin")) {
            log.info("[DataInitializer] admin 계정 이미 존재 — 스킵");
            return;
        }
        Member admin = Member.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .region("전체")
                .role(Member.Role.SUPER)
                .build();
        memberRepository.save(admin);
        log.info("[DataInitializer] admin 계정 생성 완료");
    }

    private List<Pipe> seedPipes() {
        if (pipeRepository.count() > 0) {
            log.info("[DataInitializer] Pipe 데이터 이미 존재 — 시드 스킵 (필요시 backfill 진행)");
            return pipeRepository.findAll();
        }

        LocalDateTime now = LocalDateTime.now();
        List<Pipe> pipes = List.of(
                buildPipe("강남 하수관 A-1", "서울 강남구", 37.4979, 127.0276, Pipe.Status.NORMAL, 12.3, now),
                buildPipe("마포 상수관 B-2", "서울 마포구", 37.5663, 126.9019, Pipe.Status.CAUTION, 45.7, now),
                buildPipe("해운대 하수관 C-3", "부산 해운대구", 35.1631, 129.1635, Pipe.Status.DANGER, 82.1, now),
                buildPipe("수성 상수관 D-4", "대구 수성구", 35.8583, 128.6311, Pipe.Status.NORMAL, 18.9, now),
                buildPipe("연수 하수관 E-5", "인천 연수구", 37.4106, 126.6780, Pipe.Status.CAUTION, 51.4, now)
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
            if (dirty) {
                pipeRepository.save(p);
                updated++;
            }
        }
        if (updated > 0) {
            log.info("[DataInitializer] Pipe {}건 backfill (address/installedAt)", updated);
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
                    .status(status)
                    .message(message)
                    .createdAt(LocalDateTime.now().minusMinutes(i * 15L))
                    .build();
            alertRepository.save(alert);
        }
        log.info("[DataInitializer] Alert 10건 생성");
    }

    private void migrateAlerts() {
        List<Alert> needsBackfill = alertRepository.findAll().stream()
                .filter(a -> a.getPipeName() == null)
                .toList();
        if (needsBackfill.isEmpty()) {
            log.info("[DataInitializer] Alert backfill 대상 없음");
            return;
        }

        Map<String, String> nameById = new HashMap<>();
        for (Pipe p : pipeRepository.findAll()) {
            nameById.put(p.getId(), p.getName());
        }

        int updated = 0;
        for (Alert a : needsBackfill) {
            String name = nameById.get(a.getPipeId());
            if (name != null) {
                a.setPipeName(name);
                alertRepository.save(a);
                updated++;
            }
        }
        log.info("[DataInitializer] Alert {}건 backfill (pipeName)", updated);
    }
}
