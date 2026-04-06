package egovframework.example.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import egovframework.example.domain.Member;
import egovframework.example.domain.MemberStatus;
import egovframework.example.domain.RefreshToken;

import egovframework.example.dto.auth.*;

import egovframework.example.dto.auth.MemberRegisterRequest;
import egovframework.example.repository.MemberRepository;
import egovframework.example.repository.RefreshTokenRepository;
import egovframework.example.util.JwtUtil;
import egovframework.example.util.TokenHash;
import egovframework.example.global.UnauthorizedException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor
public class MemberService {
	private final MemberRepository memberRepository;
	private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    
    private void cleanupTokens() {
        Instant now = Instant.now();
        Instant cutoff = now.minus(7, ChronoUnit.DAYS);

        refreshTokenRepository.deleteExpired(now);
        refreshTokenRepository.deleteOldRevoked(cutoff);
    }

    @Scheduled(
        fixedDelayString = "${auth.token-cleanup-delay-ms:1800000}",
        initialDelayString = "${auth.token-cleanup-initial-delay-ms:60000}"
    )
    @Transactional
    public void cleanupTokensOnSchedule() {
        cleanupTokens();
    }
	
    //register
	@Transactional
	public void addUser(MemberRegisterRequest request) {
        if (memberRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("이미 사용 중인 username 입니다.");
        }
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 email 입니다.");
        }
        if (memberRepository.existsByNickname(request.getNickname())) {
            throw new IllegalArgumentException("이미 사용 중인 nickname 입니다.");
        }
        Member member = new Member();
    	member.setUsername(request.getUsername());
    	member.setEmail(request.getEmail());
    	member.setNickname(request.getNickname());
    	member.setStatus(MemberStatus.ACTIVE);
    	member.setDeletedAt(null);
    	
    	String hash = passwordEncoder.encode(request.getPassword());
    	member.setPasswordHash(hash);
    	memberRepository.save(member);
	}
	
	//login
	@Transactional
    public LoginResponseDto login(MemberLoginRequest request) {
		Member member = memberRepository.findByUsername(request.getUsername())
                .orElseThrow(() ->
                new UnauthorizedException("아이디 또는 비밀번호가 올바르지 않습니다.")
                );
                
        if (!passwordEncoder.matches(request.getPassword(), member.getPasswordHash())) {
            throw new UnauthorizedException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        String accessToken = jwtUtil.createAccessToken(member.getId());
        String refreshToken = jwtUtil.createRefreshToken(member.getId());

        RefreshToken rt = new RefreshToken();
        rt.setMemberId(member.getId());
        rt.setTokenHash(TokenHash.sha256Hex(refreshToken));
        rt.setExpiresAt(jwtUtil.getRefreshExpiresAt()); 
        rt.setRevokedAt(null);
        
        refreshTokenRepository.save(rt);
        return new LoginResponseDto(accessToken, refreshToken, member.getNickname());
    }
	
	//Access token reissue
	@Transactional
    public TokenRefreshResponseDto refresh(TokenRefreshRequest request) {
        String refreshToken = request.getRefreshToken();
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new UnauthorizedException("refresh token이 필요합니다.");
        }
        
        // 1) JWT 서명/만료 검증
        if (!jwtUtil.isValid(refreshToken)) {
            throw new UnauthorizedException("유효하지 않은 refresh token 입니다.");
        }
        if (!jwtUtil.isRefreshToken(refreshToken)) {
            throw new UnauthorizedException("refresh token이 아닙니다.");
        }

        // 2) DB에 저장된 refresh인지 확인 (로그아웃/폐기 대비)
        String hash = TokenHash.sha256Hex(refreshToken);

        RefreshToken saved = refreshTokenRepository
                .findByTokenHashAndRevokedAtIsNull(hash)
                .orElseThrow(() ->
                        new UnauthorizedException("이미 폐기되었거나 존재하지 않는 refresh token 입니다.")
                );
        
     // 2-1) ✅ 토큰 주인 검증
        Long memberIdFromJwt = jwtUtil.getMemberId(refreshToken); // JWT subject에서
        if (!saved.getMemberId().equals(memberIdFromJwt)) {
            throw new UnauthorizedException("refresh token 주인이 일치하지 않습니다.");
        }
        
        // 3) DB 기준 만료 확인(안전망)
        if (saved.getExpiresAt().isBefore(Instant.now())) {
            saved.setRevokedAt(Instant.now());
            refreshTokenRepository.save(saved);
            throw new UnauthorizedException("만료된 refresh token 입니다.");
        }

        // 4) 새 access 발급
        Long memberId = memberIdFromJwt;
        
        String newAccessToken = jwtUtil.createAccessToken(memberId);
        
        // ✅ 5) Refresh Token Rotation
        // 5-1) 기존 refresh 폐기
        saved.setRevokedAt(Instant.now());
        refreshTokenRepository.save(saved);
        
        // 5-2) 새 refresh 발급 + DB 저장
        String newRefreshToken = jwtUtil.createRefreshToken(memberId);

        RefreshToken newRt = new RefreshToken();
        newRt.setMemberId(memberId);
        newRt.setTokenHash(TokenHash.sha256Hex(newRefreshToken));
        newRt.setExpiresAt(jwtUtil.getRefreshExpiresAt());
        newRt.setRevokedAt(null);

        refreshTokenRepository.save(newRt);
        
     // ✅ 응답은 새 refresh로 내려줌
        return new TokenRefreshResponseDto(newAccessToken, newRefreshToken);
    }
	
	//logout
	@Transactional
	public void logout(TokenRefreshRequest request) {
	    String refreshToken = request.getRefreshToken();
	    if (refreshToken == null || refreshToken.isBlank()) return;

	    String hash = TokenHash.sha256Hex(refreshToken);

	    refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hash)
	            .ifPresent(rt -> {
	                rt.setRevokedAt(Instant.now());
	                // refreshTokenRepository.save(rt); // 필요하면 명시적으로
	            });
	}
}
