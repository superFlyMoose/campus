package com.campus.management.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.management.entity.SysUser;
import com.campus.management.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserService extends ServiceImpl<SysUserMapper, SysUser> {

    private final ProfileCacheService profileCacheService;
    private final DashboardCacheService dashboardCacheService;

    public UserService(ProfileCacheService profileCacheService,
                       DashboardCacheService dashboardCacheService) {
        this.profileCacheService = profileCacheService;
        this.dashboardCacheService = dashboardCacheService;
    }

    /**
     * 用户注册
     */
    public void register(SysUser user) {
        validateUsernameUnique(user.getUsername(), null);
        user.setRole("USER");
        user.setAvatar(user.getAvatar() == null ? "" : user.getAvatar());
        user.setIsDeleted(0);
        save(user);
        dashboardCacheService.evictDashboardCache();
    }

    /**
     * 用户分页查询
     */
    public Page<SysUser> pageUsers(int pageNum, int pageSize) {
        return lambdaQuery()
            .eq(SysUser::getIsDeleted, 0)
            .orderByDesc(SysUser::getCreateTime)
            .page(new Page<>(pageNum, pageSize));
    }

    /**
     * 后台创建用户
     */
    public void createUser(SysUser user) {
        validateUsernameUnique(user.getUsername(), null);
        user.setAvatar(user.getAvatar() == null ? "" : user.getAvatar());
        user.setIsDeleted(0);
        save(user);
        dashboardCacheService.evictDashboardCache();
    }

    /**
     * 更新用户信息
     */
    public void updateUser(Long id, SysUser updatedUser) {
        SysUser existingUser = getById(id);
        if (existingUser == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        validateUsernameUnique(updatedUser.getUsername(), id);
        existingUser.setUsername(updatedUser.getUsername());
        existingUser.setRealName(updatedUser.getRealName());
        existingUser.setRole(updatedUser.getRole());
        // 密码非空才更新
        if (StringUtils.hasText(updatedUser.getPassword())) {
            existingUser.setPassword(updatedUser.getPassword());
        }
        updateById(existingUser);
        profileCacheService.evictProfileCache(id);
        dashboardCacheService.evictDashboardCache();
    }

    /**
     * 删除用户
     */
    public void deleteUser(Long id) {
        removeById(id);
        profileCacheService.evictProfileCache(id);
        dashboardCacheService.evictDashboardCache();
    }

    /**
     * 根据用户名查询用户
     */
    public SysUser findByUsername(String username) {
        return lambdaQuery()
            .eq(SysUser::getUsername, username)
            .last("limit 1")
            .one();
    }

    /**
     * 用户名唯一性检验
     */
    private void validateUsernameUnique(String username, Long excludeId) {
        boolean exists = lambdaQuery()
            .eq(SysUser::getUsername, username)
            .ne(excludeId != null, SysUser::getId, excludeId)
            .count() > 0;
        if (exists) {
            throw new IllegalArgumentException("用户名已存在");
        }
    }
}

