package com.example.SmartHelmet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String username;
    private Member.Role role;
    /** Region.id (nullable for CENTRAL/CITIZEN) */
    private String primaryRegionId;
    /** Display label: "전국" / "서울특별시" / "서울 강남구" / "시민" */
    private String regionName;
}
