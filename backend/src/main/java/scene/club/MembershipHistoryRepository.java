package scene.club;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipHistoryRepository extends JpaRepository<MembershipHistory, Long> {

    List<MembershipHistory> findByClubIdAndUserIdOrderByCreatedAtAsc(Long clubId, Long userId);
}
