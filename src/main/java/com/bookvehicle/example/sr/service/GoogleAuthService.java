package com.bookvehicle.example.sr.service;

import com.bookvehicle.example.sr.model.*;
import com.bookvehicle.example.sr.repository.CustomerRepository;
import com.bookvehicle.example.sr.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.time.LocalDateTime;

@Service
@Transactional
public class GoogleAuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;

    @Value("${app.google.client-id}")
    private String googleClientId;

    public GoogleAuthService(UserRepository userRepository,
                              CustomerRepository customerRepository) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
    }

    /**
     * Xác thực Google ID Token và trả về User.
     * - Nếu email đã tồn tại → link Google ID vào user hiện có
     * - Nếu email chưa có → tạo user mới role CUSTOMER
     *
     * @param idTokenString token nhận từ Google Sign-In frontend
     * @return User nếu thành công, null nếu token không hợp lệ
     */
    public User authenticateWithGoogle(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                return null; // Token không hợp lệ
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String googleId = payload.getSubject();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String pictureUrl = (String) payload.get("picture");

            // Tìm user theo Google ID
            User existingUser = userRepository.findByGoogleId(googleId).orElse(null);
            if (existingUser != null) {
                return existingUser;
            }

            // Tìm user theo email
            User userByEmail = userRepository.findByEmail(email.toLowerCase()).orElse(null);
            if (userByEmail != null) {
                // Chỉ cho phép CUSTOMER đăng nhập bằng Google
                if (userByEmail.getRole() != Role.CUSTOMER) {
                    System.err.println("Google Auth: Email " + email + " thuộc tài khoản " + userByEmail.getRole() + ". Chỉ CUSTOMER mới được đăng nhập bằng Google.");
                    return null;
                }
                // Link Google ID vào tài khoản CUSTOMER hiện có
                userByEmail.setGoogleId(googleId);
                if (userByEmail.getAvatarUrl() == null && pictureUrl != null) {
                    userByEmail.setAvatarUrl(pictureUrl);
                }
                userByEmail.setEmailVerified(true);
                if (userByEmail.getEmailVerifiedAt() == null) {
                    userByEmail.setEmailVerifiedAt(LocalDateTime.now());
                }
                return userRepository.save(userByEmail);
            }

            // Tạo user mới (role CUSTOMER)
            User newUser = new User();
            newUser.setEmail(email.toLowerCase());
            newUser.setGoogleId(googleId);
            newUser.setPassword(null); // Google-only user, no password
            newUser.setPhone("GOOGLE_" + googleId.substring(0, Math.min(10, googleId.length())));
            newUser.setRole(Role.CUSTOMER);
            newUser.setIsActive(true);
            newUser.setEmailVerified(true);
            newUser.setEmailVerifiedAt(LocalDateTime.now());
            if (pictureUrl != null) {
                newUser.setAvatarUrl(pictureUrl);
            }
            User savedUser = userRepository.save(newUser);

            // Tạo customer profile
            Customer customer = new Customer();
            customer.setUserId(savedUser.getId());
            customer.setFullName(name != null ? name : email.split("@")[0]);
            customer.setMembership(MembershipType.STANDARD);
            customerRepository.save(customer);

            return savedUser;

        } catch (Exception e) {
            System.err.println("Google Auth Error: " + e.getMessage());
            return null;
        }
    }
}
