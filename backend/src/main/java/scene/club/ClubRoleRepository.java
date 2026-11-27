package scene.club;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 동아리 안 표라서 조회는 항상 동아리를 함께 받는다.
 * 동아리 서비스에서 findById를 쓰지 않는다 — ArchUnit 검사는 첫 동아리 API와 함께 넣는다.
 */
public interface ClubRoleRepository extends JpaRepository<ClubRole, Long> {

    Optional<ClubRole> findByClubIdAndPublicId(Long clubId, UUID publicId);

    List<ClubRole> findByClubId(Long clubId);
}
