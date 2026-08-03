package com.example.english_app.security.oauth2;

import com.example.english_app.entity.enums.AuthProvider;
import com.example.english_app.entity.enums.Role;
import com.example.english_app.entity.user.User;
import com.example.english_app.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) {
        // Lấy thông tin từ Google
        OAuth2User oAuth2User = super.loadUser(request);
        Map<String, Object> attributes =  oAuth2User.getAttributes();

        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String picture = (String) attributes.get("picture");
        String googleId = (String) attributes.get("sub");

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setFullName(name);
                    newUser.setAvatarUrl(picture);
                    newUser.setProvider(AuthProvider.GOOGLE);
                    newUser.setProviderId(googleId);
                    newUser.setRole(Role.STUDENT);
                    newUser.setIsActive(true);
                    newUser.setPassword(java.util.UUID.randomUUID().toString());
                    return userRepository.save(newUser);
                });
        return new CustomOAuth2User(user, attributes);
    }
}