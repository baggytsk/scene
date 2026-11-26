package scene.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByPublicId(UUID publicId);

    /**
     * 대소문자를 무시하고 닉네임이 쓰이는지 본다. 넘기는 값은 NFC로 정규화한 값이어야 한다.
     * 유일 인덱스와 같은 식 lower(nickname COLLATE "C")로 비교해야 인덱스를 탄다 (JPQL로는 COLLATE를 못 써서 네이티브 쿼리).
     * 화면 안내용이고, 동시에 가입하는 경우는 유일 인덱스가 막는다.
     */
    @Query(value = """
            select exists (
                select 1 from users
                where lower(nickname collate "C") = lower(cast(:nickname as text) collate "C")
            )
            """, nativeQuery = true)
    boolean existsByNicknameIgnoringCase(@Param("nickname") String nickname);
}
