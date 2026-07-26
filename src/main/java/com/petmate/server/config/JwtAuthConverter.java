package com.petmate.server.config;

import com.petmate.server.entity.User;
import com.petmate.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository userRepository;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        Collection<GrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority("ROLE_GUEST"));

        if (email != null) {
            Optional<User> userOptional = userRepository.findByEmail(email);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                java.time.Instant issuedAt = jwt.getIssuedAt();
                
                // Reject if token was issued before tokensValidAfter
                if (user.getTokensValidAfter() != null && issuedAt != null && issuedAt.isBefore(user.getTokensValidAfter())) {
                    throw new org.springframework.security.authentication.BadCredentialsException("Token has been revoked due to password change or security event.");
                }
                
                String role = user.getRole().name();
                authorities = Collections.singleton(new SimpleGrantedAuthority("ROLE_" + role));
            }
        }

        return new JwtAuthenticationToken(jwt, authorities);
    }
}
