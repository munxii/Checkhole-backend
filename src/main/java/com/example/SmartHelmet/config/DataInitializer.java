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

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final PipeRepository pipeRepository;
    private final AlertRepository alertRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedAdmin();
        List<Pipe> pipes = seedPipes();
        seedAlerts(pipes);
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
            log.info("[DataInitializer] Pipe 데이터 이미 존재 — 스킵");
            return pipeRepository.findAll();
        }

        LocalDateTime now = LocalDateTime.now();
        List<Pipe> pipes = List.of(
                Pipe.builder()
                        .name("강남 하수관 A-1")
                        .region("서울 강남구")
                        .lat(37.4979).lng(127.0276)
                        .status(Pipe.Status.NORMAL)
                        .sensorValue(12.3)
                        .updatedAt(now)
                        .build(),
                Pipe.builder()
                        .name("마포 상수관 B-2")
                        .region("서울 마포구")
                        .lat(37.5663).lng(126.9019)
                        .status(Pipe.Status.CAUTION)
                        .sensorValue(45.7)
                        .updatedAt(now)
                        .build(),
                Pipe.builder()
                        .name("해운대 하수관 C-3")
                        .region("부산 해운대구")
                        .lat(35.1631).lng(129.1635)
                        .status(Pipe.Status.DANGER)
                        .sensorValue(82.1)
                        .updatedAt(now)
                        .build(),
                Pipe.builder()
                        .name("수성 상수관 D-4")
                        .region("대구 수성구")
                        .lat(35.8583).lng(128.6311)
                        .status(Pipe.Status.NORMAL)
                        .sensorValue(18.9)
                        .updatedAt(now)
                        .build(),
                Pipe.builder()
                        .name("연수 하수관 E-5")
                        .region("인천 연수구")
                        .lat(37.4106).lng(126.6780)
                        .status(Pipe.Status.CAUTION)
                        .sensorValue(51.4)
                        .updatedAt(now)
                        .build()
        );
        List<Pipe> saved = pipeRepository.saveAll(pipes);
        log.info("[DataInitializer] Pipe {}건 생성", saved.size());
        return saved;
    }

    private void seedAlerts(List<Pipe> pipes) {
        if (alertRepository.count() > 0) {
            log.info("[DataInitializer] Alert 데이터 이미 존재 — 스킵");
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
                    .status(status)
                    .message(message)
                    .createdAt(LocalDateTime.now().minusMinutes(i * 15L))
                    .build();
            alertRepository.save(alert);
        }
        log.info("[DataInitializer] Alert 10건 생성");
    }
}
