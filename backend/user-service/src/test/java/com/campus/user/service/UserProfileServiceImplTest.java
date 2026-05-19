package com.campus.user.service;

import com.campus.common.exception.BusinessException;
import com.campus.common.response.ResultCode;
import com.campus.user.dto.ProfileVO;
import com.campus.user.dto.UpdateProfileRequest;
import com.campus.user.entity.UserProfile;
import com.campus.user.mapper.UserProfileMapper;
import com.campus.user.service.impl.UserProfileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

    @Mock
    UserProfileMapper mapper;

    UserProfileServiceImpl service;

    @BeforeEach
    void setup() {
        service = new UserProfileServiceImpl(mapper);
    }

    private static UserProfile stub() {
        UserProfile u = new UserProfile();
        u.setId(1L);
        u.setUsername("alice");
        u.setNickname("Alice");
        u.setStatus(1);
        return u;
    }

    @Test
    void getById_returns_vo() {
        when(mapper.selectById(1L)).thenReturn(stub());
        ProfileVO vo = service.getById(1L);
        assertThat(vo.getId()).isEqualTo(1L);
        assertThat(vo.getUsername()).isEqualTo("alice");
    }

    @Test
    void getById_not_found() {
        when(mapper.selectById(99L)).thenReturn(null);
        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.USER_NOT_FOUND.getCode());
    }

    @Test
    void updateMyProfile_only_updates_non_null_fields() {
        when(mapper.selectById(1L)).thenReturn(stub(), stub());

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setNickname("Alice Updated");
        req.setEmail("alice@example.com");
        // phone / avatar 留空 → 不应写入

        service.updateMyProfile(1L, req);

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(mapper).updateById(captor.capture());
        UserProfile patch = captor.getValue();
        assertThat(patch.getId()).isEqualTo(1L);
        assertThat(patch.getNickname()).isEqualTo("Alice Updated");
        assertThat(patch.getEmail()).isEqualTo("alice@example.com");
        assertThat(patch.getPhone()).isNull();
        assertThat(patch.getAvatar()).isNull();
        // username/password 永远不该被这个接口改
        assertThat(patch.getUsername()).isNull();
    }

    @Test
    void updateMyProfile_no_change_skips_update() {
        when(mapper.selectById(1L)).thenReturn(stub());
        UpdateProfileRequest req = new UpdateProfileRequest();
        service.updateMyProfile(1L, req);
        verify(mapper, never()).updateById(any(UserProfile.class));
    }

    @Test
    void updateMyProfile_user_not_found() {
        when(mapper.selectById(99L)).thenReturn(null);
        assertThatThrownBy(() -> service.updateMyProfile(99L, new UpdateProfileRequest()))
                .isInstanceOf(BusinessException.class);
    }
}
