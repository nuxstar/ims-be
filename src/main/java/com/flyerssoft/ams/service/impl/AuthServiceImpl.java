package com.flyerssoft.ams.service.impl;

import com.flyerssoft.ams.client.microsoft.auth.MicrosoftAuthClient;
import com.flyerssoft.ams.client.microsoft.dto.Profile;
import com.flyerssoft.ams.client.microsoft.dto.TokenResponse;
import com.flyerssoft.ams.client.microsoft.graph.MicrosoftGraphClient;
import com.flyerssoft.ams.exception.BadRequestException;
import com.flyerssoft.ams.exception.NotFoundException;
import com.flyerssoft.ams.mapper.EmployeeMapper;
import com.flyerssoft.ams.mapper.EntitlementMapper;
import com.flyerssoft.ams.model.dto.AmsResponse;
import com.flyerssoft.ams.model.dto.EmployeeDto;
import com.flyerssoft.ams.model.dto.EntitlementDto;
import com.flyerssoft.ams.model.dto.LoginResponse;
import com.flyerssoft.ams.model.dto.SignUpRequestDto;
import com.flyerssoft.ams.model.entity.Employee;
import com.flyerssoft.ams.model.entity.Entitlement;
import com.flyerssoft.ams.model.entity.GroupPermission;
import com.flyerssoft.ams.model.repository.EmployeeRepository;
import com.flyerssoft.ams.model.repository.EntitlementRepository;
import com.flyerssoft.ams.model.repository.UserGroupPermissionRepository;
import com.flyerssoft.ams.security.JwtService;
import com.flyerssoft.ams.security.User;
import com.flyerssoft.ams.service.AuthService;
import com.flyerssoft.ams.service.EmployeeService;
import com.flyerssoft.ams.utility.AmsConstants;
import java.text.ParseException;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Implementation class for the AuthService interface.
 */
@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

  /**
   * Holds the end points Microsoft Authentication client.
   */
  @Autowired
  private MicrosoftAuthClient microsoftAuthClient;

  @Autowired
  private EntitlementRepository entitlementRepository;

  @Autowired
  private UserGroupPermissionRepository userGroupPermissionRepository;

  @Autowired
  private EntitlementMapper entitlementMapper;

  /**
   * Entity to Dto conversions.
   */
  @Autowired
  private EmployeeMapper employeeMapper;

  /**
   * Service which is responsible to generate properties in jwt.
   */
  @Autowired
  private JwtService jwtService;

  /**
   * Service which has all the employee related implementations.
   */
  @Autowired
  private EmployeeService employeeService;

  @Autowired
  BCryptPasswordEncoder passwordEncoder;

  /**
   * Microsoft Tenant I'd which is present in application properties.
   */
  @Value("${api.microsoft.sso.tenant.id}")
  private String tenantId;

  /**
   * Microsoft Client I'd which is present in application properties.
   */
  @Value("${api.microsoft.sso.client.id}")
  private String clientId;

  /**
   * Microsoft Client secret which is present in application properties.
   */
  @Value("${api.microsoft.sso.client.secret}")
  private String clientSecret;

  /**
   * Microsoft Redirect URL which is present in application properties.
   */
  @Value("${api.microsoft.sso.redirectUri}")
  private String redirectUri;

  /**
   * Microsoft Scopes for our api to consume which is present in application
   * properties.
   */
  @Value("${api.microsoft.sso.scopes}")
  private String scopes;

  /**
   * Grant type holds the flow in which we want to achieve microsoft
   * authentication.
   */
  @Value("${api.microsoft.sso.grantType}")
  private String grantType;

  /**
   * repository to save the all of crud operations.
   */
  @Autowired
  private EmployeeRepository empRepository;

  @Value("${super-admin.password}")
  private String superAdminPassword;

  /**
   * Holds the end points Microsoft Graph client.
   */
  @Autowired
  private MicrosoftGraphClient microsoftGraphClient;

  private String parseCredentials(String authorizationHeader) {
    if (authorizationHeader == null || !authorizationHeader.startsWith("Basic ")) {
      throw new BadRequestException("Invalid Credentials");
    }
    String encodedCredentials = authorizationHeader.substring(6).trim();
    byte[] decodedBytes = Base64.getDecoder().decode(encodedCredentials);
    return new String(decodedBytes);
  }

  private Map<String, Object> getClaimsForSuperAdmin(
      Employee superAdmin,
      List<GroupPermission> groupPermissions,
      List<Entitlement> entitlements) {
    Map<String, Object> customClaims = new HashMap<>();
    customClaims.put("iss", "Flyerssoft_AMS");
    customClaims.put("sub", superAdmin.getFlyerssoftId());
    customClaims.put("name", superAdmin.getEmployeeName());
    customClaims.put("email", superAdmin.getEmployeeEmail());

    Set<String> allEntitlements = entitlements.stream().map(Entitlement::getName).collect(
        Collectors.toSet());
    customClaims.put("user_permissions", allEntitlements);
    List<String> allGroups = groupPermissions.stream().map(GroupPermission::getGroupName)
        .toList();
    customClaims.put("groups", allGroups);
    return customClaims;
  }

  private LoginResponse getTokenResponse(Employee employee) {
    User employeeUser = employeeMapper.toUser(employee);
    var customClaims = this.getEmployeeClaims(employee);
    String customToken = jwtService.generateToken(customClaims, employeeUser);
    Date expirationTime = jwtService.extractExpiration(customToken);
    EmployeeDto employeeDto = employeeMapper.toDto(employee);
    Set<Entitlement> entitlements = employee.getGroupPermissions()
        .stream()
        .map((GroupPermission::getEntitlements))
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
    Set<EntitlementDto> entitlementDtos = entitlementMapper.toDto(entitlements);
    return new LoginResponse(
        employeeDto,
        expirationTime.getTime(),
        customToken,
        (List<String>) customClaims.get("groups"),
        entitlementDtos
    );
  }

  private Map<String, Object> getEmployeeClaims(Employee employee) {
    Map<String, Object> customClaims = new HashMap<>();
    customClaims.put("iss", "Flyerssoft_AMS");
    customClaims.put("name", employee.getEmployeeName());
    customClaims.put("email", employee.getEmployeeEmail());
    Set<String> allEntitlements = employee.getGroupPermissions().stream()
        .flatMap(userGroup -> userGroup.getEntitlements().stream())
        .map(Entitlement::getName)
        .collect(Collectors.toSet());
    customClaims.put("user_permissions", allEntitlements);
    List<String> allGroups = employee.getGroupPermissions().stream()
        .map(GroupPermission::getGroupName)
        .toList();
    customClaims.put("groups", allGroups);
    return customClaims;
  }

  /**
   * Authenticates the user based on the provided authorization code.
   *
   * @param authCode The authorization code obtained from the authentication
   *                 process.
   * @return A LoginResponseDto object containing the profile information and
   *         access token.
   * @throws ParseException If there is an error parsing the response.
   */
  @Override
  public AmsResponse<?> authenticate(final String authCode) throws ParseException {
    Map<String, String> urlEncodedFormData = new HashMap<>();
    urlEncodedFormData.put(AmsConstants.MS_CODE, authCode);
    urlEncodedFormData.put(AmsConstants.MS_CLIENTID, clientId);
    urlEncodedFormData.put(AmsConstants.MS_REDIRECTURI, redirectUri);
    urlEncodedFormData.put(AmsConstants.GRANT_TYPE, grantType);
    urlEncodedFormData.put(AmsConstants.CLIENT_SECRET, clientSecret);

    TokenResponse tokenResponse = microsoftAuthClient.getAccessToken(tenantId, urlEncodedFormData);
    String bearerToken = AmsConstants.BEARER + tokenResponse.accessToken();
    Profile profileResponse = microsoftGraphClient.getUserById(bearerToken);

    Employee employee = this.checkEmployeeInDatabase(profileResponse.mail());
    if (ObjectUtils.isNotEmpty(employee)) {
      User employeeUser = employeeMapper.toUser(employee);
      var customClaims = this.getEmployeeClaims(employee);
      String customToken = jwtService.generateToken(customClaims, employeeUser);
      Date expirationTime = jwtService.extractExpiration(customToken);
      List<String> userGroups = employee.getGroupPermissions().stream()
          .map(GroupPermission::getGroupName)
          .collect(Collectors.toList());
      Set<EntitlementDto> entitlements = employee.getGroupPermissions().stream()
          .flatMap(userGroupPermission -> userGroupPermission.getEntitlements().stream())
          .map(entitlement -> entitlementMapper.toDto(entitlement))
          .collect(Collectors.toSet());
      LoginResponse loginResponse = new LoginResponse(
          employeeMapper.toDto(employee),
          expirationTime.getTime(),
          customToken,
          userGroups,
          entitlements
      );

      return new AmsResponse<>(HttpStatus.OK.value(), true, loginResponse);
    } else {
      // save to ams db
      EmployeeDto savedEmployee = employeeService.addEmployee(
          profileResponse);
      log.info("Employee saved to DB and redirecting to signup :{}", savedEmployee.employeeId());
      return new AmsResponse<>(
          HttpStatus.TEMPORARY_REDIRECT.value(),
          true,
          new LoginResponse(
              savedEmployee,
              tokenResponse.expiresIn(),
              tokenResponse.accessToken(),
              null,
              null
          )
      );
    }
  }

  /**
   * Method to check whether the employee is exists with ams database.
   *
   * @param email to check by property
   * @return boolean value
   */
  public Employee checkEmployeeInDatabase(String email) {
    Employee existEmp = empRepository.findByEmployeeEmail(email);
    if (ObjectUtils.isNotEmpty(existEmp)) {
      return existEmp;
    } else {
      return null;
    }
  }

  /**
   * Implementation method to handle the signup.
   *
   * @param accessToken accessToken
   *
   * @param signUpRequest signUpRequest
   * @return custom login response object
   */
  @Override
  public LoginResponse signup(String accessToken, SignUpRequestDto signUpRequest) {
    Employee employee = employeeService.updateSignup(accessToken, signUpRequest);
    return getTokenResponse(employee);
  }

  /**
   * generate login token for super admin.
   *
   * @param authorizationHeader authorization header contains basic auth.
   * @return LoginResponseDto contains token and it's expiry time.
   * @throws ParseException Parse Exception
   */
  @Override
  public LoginResponse login(String authorizationHeader) throws ParseException {
    String decodedCredentials = parseCredentials(authorizationHeader);

    // Split the decoded credentials into username and password
    String[] credentials = decodedCredentials.split(":", 2);
    String userEmail = credentials[0];
    String password = credentials[1];

    Employee superAdmin = empRepository
        .findById(1)
        .orElseThrow(
            () -> new NotFoundException(
                String.format(
                    NotFoundException.EMPLOYEE_NOT_FOUND,
                    1L
                )
            )
        );
    var superAdminUser = employeeMapper.toUser(superAdmin);

    if (!superAdmin.getEmployeeEmail().equals(userEmail)) {
      throw new BadRequestException("Invalid Credentials");
    }

    if (!passwordEncoder.matches(password, superAdminPassword)) {
      throw new BadRequestException("Invalid Credentials");
    }

    List<GroupPermission> groupPermissions = userGroupPermissionRepository.findAll();
    List<Entitlement> entitlements = entitlementRepository.findAll();

    Map<String, Object> customClaims = getClaimsForSuperAdmin(
        superAdmin,
        groupPermissions,
        entitlements);

    String token = jwtService.generateToken(customClaims, superAdminUser);
    Date expirationTime = jwtService.extractExpiration(token);

    List<EntitlementDto> entitlementDtos = entitlementMapper.toDto(entitlements);
    EmployeeDto superAdminDto = employeeMapper.toDto(superAdmin);
    return new LoginResponse(
        superAdminDto,
        expirationTime.getTime(),
        token,
        (List<String>) customClaims.get("groups"),
        new HashSet<>(entitlementDtos)
    );
  }

}
