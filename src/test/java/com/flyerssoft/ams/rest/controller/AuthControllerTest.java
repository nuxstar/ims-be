package com.flyerssoft.ams.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.text.ParseException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.flyerssoft.ams.model.dto.AmsResponse;
import com.flyerssoft.ams.model.dto.EmployeeDto;
import com.flyerssoft.ams.model.dto.LoginResponse;
import com.flyerssoft.ams.model.dto.SignUpRequestDto;
import com.flyerssoft.ams.security.JwtService;
import com.flyerssoft.ams.service.AuthService;
import com.flyerssoft.ams.utility.AmsConstants;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(AuthController.class)
@MockBean({ JwtService.class })
@AutoConfigureMockMvc
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthControllerTest {

	@Autowired
	private WebApplicationContext webApplicationContext;

	private MockMvc mockMvc;

	@MockBean
	private AuthService authService;

	@MockBean
	JwtService jwtService;

	@InjectMocks
	private AuthController authenticationController;

	@Test
	public void testAuthenticate_Success() throws ParseException {
		String authCode = "validAuthCode";
		String redirectUrl = "https://example.com/signup";
		AmsResponse<?> expectedResponse = new AmsResponse<>(HttpStatus.OK.value(), true, "Success");
		when(authService.authenticate(authCode)).thenAnswer(x -> expectedResponse);
		ResponseEntity<?> response = authenticationController.authenticate(authCode);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(expectedResponse, response.getBody());
		
		 AmsResponse<?> expectedResponse1 = new AmsResponse<>(HttpStatus.TEMPORARY_REDIRECT.value(),true, "Temporary Redirect");

		  // Mock the authService.authenticate() method to return the expected response
		  when(authService.authenticate(authCode)).thenAnswer(x -> expectedResponse1);

		  ResponseEntity<?> response1 = authenticationController.authenticate(authCode);
		  assertEquals(HttpStatus.TEMPORARY_REDIRECT, response1.getStatusCode());
		  assertEquals(expectedResponse1, response1.getBody());
		  HttpHeaders headers = response1.getHeaders();
		  assertTrue(headers.containsKey(AmsConstants.REDIRECT_URL_KEY));

	}

	@Test
	public void test_employeeSignup() {
		String validAuthCode = "validAuthCode";
		SignUpRequestDto signUpRequest = new SignUpRequestDto("");
		LoginResponse expectedResponse = new LoginResponse(
				new EmployeeDto(1, validAuthCode, validAuthCode, validAuthCode, validAuthCode, validAuthCode,
						validAuthCode, validAuthCode, validAuthCode, null, null, null),
				1L, validAuthCode, null, null);
		Mockito.when(authService.signup(validAuthCode, signUpRequest)).thenReturn(expectedResponse);
		ResponseEntity<AmsResponse<LoginResponse>> response = authenticationController.signup(validAuthCode,
				signUpRequest);
		assertEquals(HttpStatus.ACCEPTED.value(), response.getStatusCodeValue());
		assertTrue(response.getBody().getResponse());
		assertEquals(expectedResponse, response.getBody().getData());
	}

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
	}

	@Test
	void superAdminLogin_success() throws Exception {
		EmployeeDto superAdminDto = new EmployeeDto(1, "Super Admin", "user", "", "", "chennai", "superAdmin",
				"super-admin", "", null, null, null);
		long expirationTime = System.currentTimeMillis() + 3600000; // 1hour
		String mockToken = "mockToken";

		var loginResponse = new LoginResponse(superAdminDto, expirationTime, mockToken, null, null);
		when(authService.login(any())).thenReturn(loginResponse);

		MvcResult result = mockMvc
				.perform(MockMvcRequestBuilders.post("/v1/login").header("Authorization", "mockAuthorization"))
				.andExpect(status().isOk()).andReturn();
	}

	@Test
	void superAdminLogin_invalidParams() throws Exception {
		MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/v1/login")).andExpect(status().isBadRequest())
				.andReturn();
	}
}
