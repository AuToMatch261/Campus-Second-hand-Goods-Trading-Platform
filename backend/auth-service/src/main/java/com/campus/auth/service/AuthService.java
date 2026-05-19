package com.campus.auth.service;

import com.campus.auth.dto.LoginRequest;
import com.campus.auth.dto.LoginResult;
import com.campus.auth.dto.RegisterRequest;
import com.campus.auth.dto.UserVO;

public interface AuthService {

    UserVO register(RegisterRequest req);

    LoginResult login(LoginRequest req);

    UserVO getCurrentUser(long userId);
}
