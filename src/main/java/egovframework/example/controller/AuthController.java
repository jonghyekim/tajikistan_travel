package egovframework.example.controller;

import egovframework.example.dto.auth.*;
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
    public ResponseEntity<LoginResponseDto> login(
            @RequestBody MemberLoginRequest request
    ) {
    	LoginResponseDto response = memberService.login(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponseDto> refresh(
            @RequestBody TokenRefreshRequest request
    ) {
    	TokenRefreshResponseDto response = memberService.refresh(request);
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