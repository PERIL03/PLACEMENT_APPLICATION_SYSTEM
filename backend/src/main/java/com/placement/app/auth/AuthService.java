package com.placement.app.auth;

import com.placement.app.security.JwtService;
import com.placement.app.user.AppUser;
import com.placement.app.user.AppUserRepository;
import com.placement.app.user.UserRole;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (appUserRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username is already taken");
        }

        AppUser user = new AppUser();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(request.role());

        AppUser savedUser = appUserRepository.save(user);
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(savedUser.getUsername())
                .password(savedUser.getPassword())
                .roles(savedUser.getRole().name())
                .build();

        String token = jwtService.generateToken(userDetails);
        return new AuthResponse(token, savedUser.getUsername(), savedUser.getRole());
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.username(),
                request.password()));

        UserDetails principal = (UserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(principal);
        String role = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring(5))
                .findFirst()
                .orElse(UserRole.STUDENT.name());

        return new AuthResponse(token, principal.getUsername(), UserRole.valueOf(role));
    }

    public record RegisterRequest(String username, String password, UserRole role) {
    }

    public record LoginRequest(String username, String password) {
    }

    public record AuthResponse(String token, String username, UserRole role) {
    }
}
