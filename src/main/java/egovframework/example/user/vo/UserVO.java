package egovframework.example.user.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class UserVO {
	private Integer userId;
    private String loginId;
    private String password;
    private String userName;
    private String nickname;
    private String role;
    private String langCode;
    private String useYn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
