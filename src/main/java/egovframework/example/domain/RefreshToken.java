package egovframework.example.domain;

import org.hibernate.annotations.CreationTimestamp;
import lombok.Getter; 
import lombok.Setter;
import javax.persistence.*;
import java.time.Instant;

@Entity
@Getter
@Setter
public class RefreshToken {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
    @Column(name="member_id", nullable = false)
    private Long memberId;

    @Column(name="token_hash", nullable = false, length = 128)
    private String tokenHash;
	
    @Column(name="expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name="revoked_at")
    private Instant revokedAt;
	
    @CreationTimestamp
    @Column(name="created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
