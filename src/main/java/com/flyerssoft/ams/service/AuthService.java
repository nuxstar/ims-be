package com.flyerssoft.ams.service;

import com.flyerssoft.ams.model.dto.AmsResponse;
import com.flyerssoft.ams.model.dto.LoginResponse;
import com.flyerssoft.ams.model.dto.SignUpRequestDto;
import java.text.ParseException;

/**
 * The auth service.
 */
public interface AuthService {

  /**
   * login response.
   *
   * @param authCode authCode
   * @return login response dto
   * @throws ParseException parse exception
   */
  AmsResponse<?> authenticate(String authCode) throws ParseException;


  LoginResponse signup(String accessToken, SignUpRequestDto signUpRequest);

  LoginResponse login(String authorizationHeader) throws ParseException;
}
