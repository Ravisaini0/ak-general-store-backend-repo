package com.akgeneralstore.dto.response;

import com.akgeneralstore.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private UserRole role;
    private Long userId;
    private String name;
    private String email;
    private String phone;
    private boolean emailVerified;
    private boolean blocked;
}
