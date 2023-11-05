package com.flyerssoft.ams.mapper;

import com.flyerssoft.ams.model.dto.UserGroupPermissionDto;
import com.flyerssoft.ams.model.entity.GroupPermission;
import java.util.List;
import org.mapstruct.Mapper;

/**
 * Mapper class which holded all the mapper interface and its
 * implementations of the user group permissions.
 */
@Mapper(componentModel = "spring")
public interface UserGroupPermissionMapper {

  UserGroupPermissionDto toDto(GroupPermission groupPermission);

  List<UserGroupPermissionDto> toDtoList(List<GroupPermission> groupPermissionList);
}
