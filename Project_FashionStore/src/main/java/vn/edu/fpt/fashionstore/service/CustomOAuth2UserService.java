package vn.edu.fpt.fashionstore.service;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import vn.edu.fpt.fashionstore.entity.Account;
import vn.edu.fpt.fashionstore.entity.Role;
import vn.edu.fpt.fashionstore.repository.AccountRepository;
import vn.edu.fpt.fashionstore.repository.RoleRepository;

import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;

    public CustomOAuth2UserService(AccountRepository accountRepository, RoleRepository roleRepository) {
        this.accountRepository = accountRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");
        
        // Tìm hoặc tạo user trong database
        Optional<Account> existingAccount = accountRepository.findByUsername(email);
        
        if (existingAccount.isEmpty()) {
            // Tạo new user
            Account newAccount = new Account();
            newAccount.setUsername(email);
            newAccount.setEmail(email);
            newAccount.setFullName(name != null ? name : email);
            newAccount.setStatus("active");
            
            // Gán role USER (mặc định)
            Role userRole = roleRepository.findByRoleName("Customer")
                    .orElseGet(() -> {
                        Role newRole = new Role();
                        newRole.setRoleName("Customer");
                        return roleRepository.save(newRole);
                    });
            newAccount.setRole(userRole);
            
            accountRepository.save(newAccount);
        }
        
        return oAuth2User;
    }
}
