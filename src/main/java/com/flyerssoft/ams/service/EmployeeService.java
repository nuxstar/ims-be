package com.flyerssoft.ams.service;

import com.flyerssoft.ams.client.microsoft.dto.Profile;
import com.flyerssoft.ams.model.dto.EmployeeDto;
import com.flyerssoft.ams.model.dto.SignUpRequestDto;
import com.flyerssoft.ams.model.entity.Employee;

/**
 * The EmployeeService interface provides methods for managing employees in the
 * system.
 */
public interface EmployeeService {
  /**
   * Adds an employee using the provided profile response.
   *
   * @param profileResponse The profile response containing employee details from
   *                        microsoft graph client.
   * @return The EmployeeDto representing the added employee.
   */
  EmployeeDto addEmployee(final Profile profileResponse);

  Employee updateSignup(String accessToken, SignUpRequestDto signUpRequest);

}
