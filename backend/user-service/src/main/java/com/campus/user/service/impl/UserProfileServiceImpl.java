package com.campus.user.service.impl;

import com.campus.common.exception.BusinessException;
import com.campus.common.response.ResultCode;
import com.campus.user.dto.ProfileVO;
import com.campus.user.dto.UpdateProfileRequest;
import com.campus.user.entity.UserProfile;
import com.campus.user.mapper.UserProfileMapper;
import com.campus.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileMapper userMapper;

    @Override
    public ProfileVO getById(long userId) {
        UserProfile u = userMapper.selectById(userId);
        if (u == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return toVO(u);
    }

    @Override
    @Transactional
    public ProfileVO updateMyProfile(long userId, UpdateProfileRequest req) {
        UserProfile existing = userMapper.selectById(userId);
        if (existing == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        UserProfile patch = new UserProfile();
        patch.setId(userId);
        boolean changed = false;
        if (req.getNickname() != null) { patch.setNickname(req.getNickname()); changed = true; }
        if (req.getAvatar()   != null) { patch.setAvatar(req.getAvatar());     changed = true; }
        if (req.getPhone()    != null) { patch.setPhone(req.getPhone());       changed = true; }
        if (req.getEmail()    != null) { patch.setEmail(req.getEmail());       changed = true; }

        if (changed) {
            userMapper.updateById(patch);
            log.info("用户资料已更新: userId={}", userId);
            existing = userMapper.selectById(userId);
        }
        return toVO(existing);
    }

    private static ProfileVO toVO(UserProfile u) {
        return ProfileVO.builder()
                .id(u.getId())
                .username(u.getUsername())
                .nickname(u.getNickname())
                .avatar(u.getAvatar())
                .phone(u.getPhone())
                .email(u.getEmail())
                .build();
    }
}
