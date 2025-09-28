package com.kukokuk.security;

import com.kukokuk.domain.user.vo.User;
import java.io.Serial;
import java.util.Collection;
import lombok.Getter;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class SecurityUser implements UserDetails, CredentialsContainer {

    @Serial
    private static final long serialVersionUID = 8794484743983436451L;

    private final User user;

    public SecurityUser(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getRoleNames()
            .stream()
            .map(SimpleGrantedAuthority::new)
            .toList();
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isEnabled() {
        return "N".equals(user.getIsDeleted());
    }

    // 인증 완료 후 Spring Security가 자동으로 호출
    @Override
    public void eraseCredentials() {
        user.setPassword(null);   // ✅ password 제거
    }
}
