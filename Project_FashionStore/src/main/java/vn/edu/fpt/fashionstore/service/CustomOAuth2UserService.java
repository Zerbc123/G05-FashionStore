package vn.edu.fpt.fashionstore.service;

import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import vn.edu.fpt.fashionstore.entity.Account;
import vn.edu.fpt.fashionstore.entity.Account;
import vn.edu.fpt.fashionstore.service.AccountService;
import java.util.ArrayList;
import java.util.Collection;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final AccountService accountService;
    

    public CustomOAuth2UserService(@Lazy AccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        
        // Gọi AccountService để tạo/cập nhật Account + Customer
        accountService.processOAuthPostLogin(email, name);

        Account account = accountService.getAccountByEmail(email);

        if(account == null){
            throw new OAuth2AuthenticationException("Account not found");
        }


        Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();

        String role = account.getRole().getRoleName();

        authorities.add(new SimpleGrantedAuthority(role));

        return new DefaultOAuth2User(
                authorities,
                oAuth2User.getAttributes(),
                "email"
        );

    }
}
