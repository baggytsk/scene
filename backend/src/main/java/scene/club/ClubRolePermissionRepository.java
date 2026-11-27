package scene.club;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubRolePermissionRepository
        extends JpaRepository<ClubRolePermission, ClubRolePermission.Id> {

    List<ClubRolePermission> findByIdRoleId(Long roleId);
}
