package egovframework.example.dto.auth;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TokenLogoutRequest {
    private String refreshToken; // 바디 방식용 (쿠키로 전환하면 안 보내도 됨)
}