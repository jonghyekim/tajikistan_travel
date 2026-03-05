package egovframework.example.repository;

import egovframework.example.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long>{
	
	Optional<RefreshToken> findByTokenHashAndRevokedAtIsNull(String tokenHash);
   
	// 규칙 1) 만료된 토큰 삭제: expires_at < now
    @Modifying
    @Query("delete from RefreshToken rt where rt.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);

    // 규칙 2) revoked 된 토큰 삭제(예: 7일 지난 것): revoked_at < cutoff
    @Modifying
    @Query("delete from RefreshToken rt where rt.revokedAt is not null and rt.revokedAt < :cutoff")
    int deleteOldRevoked(@Param("cutoff") Instant cutoff);
	
}
