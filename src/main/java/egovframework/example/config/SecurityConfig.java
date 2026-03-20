package egovframework.example.config;

import egovframework.example.security.JwtAuthenticationFilter;
import egovframework.example.util.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtUtil jwtUtil) throws Exception {

        http
            .csrf().disable()

            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()

            .authorizeRequests()
            	.antMatchers("/me/**").authenticated()
            	.antMatchers(HttpMethod.POST, "/api/reviews/**").authenticated()
            	.antMatchers(HttpMethod.DELETE, "/api/reviews/**").authenticated()
            	.anyRequest().permitAll()
            .and()

            .addFilterBefore(new JwtAuthenticationFilter(jwtUtil), 
            		UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
