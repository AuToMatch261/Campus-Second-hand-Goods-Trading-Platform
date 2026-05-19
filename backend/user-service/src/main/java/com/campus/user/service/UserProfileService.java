package com.campus.user.service;

import com.campus.user.dto.ProfileVO;
import com.campus.user.dto.UpdateProfileRequest;

public interface UserProfileService {

    ProfileVO getById(long userId);

    ProfileVO updateMyProfile(long userId, UpdateProfileRequest req);
}
