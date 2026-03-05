package egovframework.example.dto.auth;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MemberRegisterRequest {
    private String username;
    private String email;
    private String password;
    private String nickname;
}

