package scene.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginMethodRepository extends JpaRepository<LoginMethod, Long> {

    /** 소셜 로그인 뒤 회원을 찾는다. 회원도 한 번의 조회로 같이 가져온다 */
    @EntityGraph(attributePaths = "user")
    Optional<LoginMethod> findByProviderAndProviderUserId(LoginProvider provider, String providerUserId);
}
