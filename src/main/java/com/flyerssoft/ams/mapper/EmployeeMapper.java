package com.flyerssoft.ams.mapper;

import com.flyerssoft.ams.model.dto.EmployeeDto;
import com.flyerssoft.ams.model.entity.Employee;
import com.flyerssoft.ams.security.User;
import org.mapstruct.Mapper;

/**
 * The EmployeeMapper interface is responsible for mapping Employee objects to
 * EmployeeDto objects and Profile objects to Employee objects.
 */
@Mapper(componentModel = "spring")
public interface EmployeeMapper {

  Employee dtoToEntity(EmployeeDto employee1);

  EmployeeDto toDto(Employee employee);

  User toUser(Employee employee);
}
