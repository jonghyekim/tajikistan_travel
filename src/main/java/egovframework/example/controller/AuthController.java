package egovframework.example.controller;

import egovframework.example.dto.auth.MemberRegisterRequest;
import egovframework.example.dto.auth.MemberLoginRequest;
import egovframework.example.dto.auth.TokenRefreshRequest;
import egovframework.example.dto.auth.TokenResponse;
import egovframework.example.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final MemberService memberService;

    @PostMapping("/signup")
    public ResponseEntity<Void> register(
            @RequestBody MemberRegisterRequest request
    ) {
        memberService.addUser(request);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @RequestBody MemberLoginRequest request
    ) {
        TokenResponse response = memberService.login(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @RequestBody TokenRefreshRequest request
    ) {
        TokenResponse response = memberService.refresh(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestBody TokenRefreshRequest request
    ) {
        memberService.logout(request);
        return ResponseEntity.ok().build();
    }
}