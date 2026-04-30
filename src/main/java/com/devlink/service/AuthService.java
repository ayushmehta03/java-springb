package com.devlink.service;

import com.devlink.dto.request.AuthRequest;
import com.devlink.dto.response.ResponseDto;
import com.devlink.model.User;
import com.devlink.repository.UserRepository;
import com.devlink.util.JwtUtil;
import com.devlink.util.OtpUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OtpUtil otpUtil;
    private final EmailService emailService;
    private final CookieService cookieService;

    @Value("${app.avatar.base-url}")
    private String avatarBaseUrl;

    public void register(AuthRequest.Register req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new ResponseStatusException(CONFLICT, "User already exists");
        }

        String seed = req.getUserName() != null && !req.getUserName().isBlank()
                ? req.getUserName() : req.getEmail();
        String avatarUrl = avatarBaseUrl + "?seed=" + URLEncoder.encode(seed, StandardCharsets.UTF_8);

        String otp = otpUtil.generateOtp();
        String otpHash = passwordEncoder.encode(otp);

        User user = new User();
        user.setUserId(UUID.randomUUID().toString());
        user.setUserName(req.getUserName());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setVerified(false);
        user.setRole("user");
        user.setOtpHash(otpHash);
        user.setOtpExpiry(Instant.now().plusSeconds(600)); // 10 minutes
        user.setProfileImage(avatarUrl);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);

        emailService.sendOtpEmail(req.getEmail(), otp);
    }

    public void verifyOtp(AuthRequest.VerifyOtp req, HttpServletResponse response) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));

        if (user.isVerified()) {
            throw new ResponseStatusException(BAD_REQUEST, "User already verified");
        }
        if (Instant.now().isAfter(user.getOtpExpiry())) {
            throw new ResponseStatusException(BAD_REQUEST, "OTP expired");
        }
        if (!passwordEncoder.matches(req.getOtp(), user.getOtpHash())) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid OTP");
        }

        user.setVerified(true);
        user.setOtpHash(null);
        user.setOtpExpiry(null);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole());
        cookieService.setAuthCookie(response, token);
    }

    public void login(AuthRequest.Login req, HttpServletResponse response) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "No account found with this email"));

        if (!user.isVerified()) {
            throw new ResponseStatusException(FORBIDDEN, "Account not verified");
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole());
        cookieService.setAuthCookie(response, token);
    }

    public void logout(HttpServletResponse response) {
        cookieService.clearAuthCookie(response);
    }

    public ResponseDto.MeResponse getMe(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "User not found"));
        return new ResponseDto.MeResponse(
                user.getId(),
                user.getUserName(),
                user.getEmail(),
                user.getProfileImage(),
                user.getRole(),
                user.getCreatedAt()
        );
    }

    public void resendOtp(AuthRequest.ResendOtp req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));

        if (user.isVerified()) {
            throw new ResponseStatusException(BAD_REQUEST, "User already verified");
        }

        String otp = otpUtil.generateOtp();
        user.setOtpHash(passwordEncoder.encode(otp));
        user.setOtpExpiry(Instant.now().plusSeconds(600));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        emailService.sendOtpEmail(req.getEmail(), otp);
    }
}
