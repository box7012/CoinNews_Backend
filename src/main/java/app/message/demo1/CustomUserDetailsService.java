package app.message.demo1;

import java.util.Arrays;
import java.util.Collection;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;  // UserRepository를 통해 사용자 정보를 조회

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 데이터베이스에서 사용자 정보 조회
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // CustomUserDetails로 변환하여 반환
        return new CustomUserDetails(user.getEmail(), user.getPassword(), getAuthorities(user));
    }

    // 권한을 가져오는 메서드
    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        // 권한 정보 로딩 (예시)
        return Arrays.asList(new SimpleGrantedAuthority("ROLE_USER"));
    }
}