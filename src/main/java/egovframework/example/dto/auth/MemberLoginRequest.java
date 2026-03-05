package egovframework.example.dto.auth;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MemberLoginRequest {
	private String username;
    private String password;
}
