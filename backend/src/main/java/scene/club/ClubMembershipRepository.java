package scene.club;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 동아리 안 표라서 조회는 항상 동아리를 함께 받는다 */
public interface ClubMembershipRepository extends JpaRepository<ClubMembership, Long> {

    Optional<ClubMembership> findByClubIdAndUserId(Long clubId, Long userId);

    List<ClubMembership> findByClubIdAndStatus(Long clubId, MembershipStatus status);

    /** 내 동아리 목록 */
    List<ClubMembership> findByUserIdAndStatus(Long userId, MembershipStatus status);
}
