package com.campus.management.service;

import com.campus.management.entity.SysUser;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserService userService;

    public CustomUserDetailsService(UserService userService) {
        this.userService = userService;
    }

    /**
     * 根据用户名加载用户认证信息
     *
     * @param username 登录时输入的用户名
     * @return Spring Security标准用户对象
     * @throws UsernameNotFoundException 当用户不存在时抛出，终止认证流程
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userService.findByUsername(username);
        // 用户不存在，直接终止认证流程
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        // 构建Spring Security认证用户对象
        return User.withUsername(user.getUsername())
            .password(user.getPassword())
            .authorities(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
            .build();
    }
}
