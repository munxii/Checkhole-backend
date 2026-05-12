package com.example.SmartHelmet.service;

import com.example.SmartHelmet.dto.LoginRequest;
import com.example.SmartHelmet.dto.LoginResponse;
import com.example.SmartHelmet.dto.Member;
import com.example.SmartHelmet.repository.MemberRepository;
import com.example.SmartHelmet.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final RegionAccessService regionAccessService;

    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByUsername(request.getId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        String token = jwtUtil.generateToken(member.getUsername());

        return LoginResponse.builder()
                .token(token)
                .username(member.getUsername())
                .role(member.getRole())
                .primaryRegionId(member.getPrimaryRegionId())
                .regionName(regionAccessService.displayName(member))
                .build();
    }

    public Member register(String username, String password, Member.Role role, String primaryRegionId) {
        if (memberRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("이미 존재하는 사용자입니다.");
        }
        Member member = Member.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .role(role)
                .primaryRegionId(primaryRegionId)
                .build();
        return memberRepository.save(member);
    }
}
