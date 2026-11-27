package scene.club;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** 지운 동아리는 찾기 · 미리보기 · 링크에서 나오지 않는다 */
public interface ClubRepository extends JpaRepository<Club, Long> {

    Optional<Club> findByPublicIdAndDeletedAtIsNull(UUID publicId);
}
