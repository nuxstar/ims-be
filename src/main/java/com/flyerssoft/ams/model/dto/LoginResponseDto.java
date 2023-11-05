package com.flyerssoft.ams.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.flyerssoft.ams.client.microsoft.dto.Profile;
import com.flyerssoft.ams.model.entity.Employee;

/**
 * Custom object response which we can hold employee data along with
 * the microsoft access token.
 *
 * @param profile - custom object which hold microsoft graph api data.
 * @param employeeDto - object used to store the employee details in the database.
 * @param expiresIn - access token expiration time.
 * @param accessToken - access token property.
 * @param amsToken - custom ams jwt token.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponseDto(
    Profile profile, EmployeeDto employeeDto,
    long expiresIn, String accessToken, String amsToken) {}
