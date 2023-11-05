package com.flyerssoft.ams.model.repository;

import com.flyerssoft.ams.model.entity.GroupPermission;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * UserGroupPermission table - Repository interface.
 */
public interface UserGroupPermissionRepository extends JpaRepository<GroupPermission, Long> {
  List<GroupPermission> findByIsDefault(boolean b);
}
