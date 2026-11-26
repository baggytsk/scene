package scene.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.text.Normalizer;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import scene.TestcontainersConfiguration;

/**
 * Flyway V1 스키마 위에서 회원 · 로그인 수단 규칙을 확인한다 (PostgreSQL 컨테이너).
 * 거부되는 경우는 어떤 제약이 막았는지까지 확인한다 (다른 이유로 실패해도 통과하지 않게).
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class UserRepositoryTest {

    @Autowired
    UserRepository users;

    @Autowired
    LoginMethodRepository loginMethods;

    @Autowired
    JdbcTemplate jdbc;

    // ── 저장 · 조회 ──────────────────────────────

    @Test
    void 가입한_회원은_DB가_만든_UUIDv7_공개_ID와_활동_상태를_가진다() {
        User user = users.saveAndFlush(User.signUp("남진"));

        assertThat(user.getId()).isNotNull();
        assertThat(user.getPublicId()).isNotNull();
        assertThat(user.getPublicId().version()).isEqualTo(7);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getCreatedAt()).isNotNull();
    }

    @Test
    void 제공자와_제공자_ID로_회원을_찾는다() {
        User user = users.save(User.signUp("남진"));
        loginMethods.saveAndFlush(new LoginMethod(user, LoginProvider.KAKAO, "1234567890"));

        assertThat(loginMethods.findByProviderAndProviderUserId(LoginProvider.KAKAO, "1234567890"))
                .map(LoginMethod::getUser)
                .map(User::getPublicId)
                .contains(user.getPublicId());
        assertThat(loginMethods.findByProviderAndProviderUserId(LoginProvider.NAVER, "1234567890")).isEmpty();
    }

    @Test
    void 한_회원은_로그인_수단을_여러_개_가질_수_있다() {
        User user = users.save(User.signUp("남진"));
        loginMethods.save(new LoginMethod(user, LoginProvider.KAKAO, "1234567890"));
        loginMethods.saveAndFlush(new LoginMethod(user, LoginProvider.GOOGLE, "1234567890"));

        assertThat(loginMethods.findByProviderAndProviderUserId(LoginProvider.GOOGLE, "1234567890"))
                .map(LoginMethod::getUser)
                .map(User::getId)
                .contains(user.getId());
    }

    @Test
    void 닉네임_확인은_대소문자를_무시하고_유일_인덱스를_탄다() {
        users.saveAndFlush(User.signUp("Baggy"));

        assertThat(users.existsByNicknameIgnoringCase("bAGGY")).isTrue();
        assertThat(users.existsByNicknameIgnoringCase("baggy2")).isFalse();

        // 행이 적으면 순차 스캔이 더 싸서 인덱스를 안 쓰므로, 순차 스캔을 끄고 계획을 본다
        jdbc.execute("set local enable_seqscan = off");
        List<String> plan = jdbc.queryForList("""
                explain select 1 from users
                where lower(nickname collate "C") = lower(cast('bAGGY' as text) collate "C")
                """, String.class);
        assertThat(String.join("\n", plan)).contains("users_nickname_lower_key");
    }

    // ── 유일 · 외래 키 ───────────────────────────

    @Test
    void 같은_제공자_계정은_두_회원에_연결할_수_없다() {
        User first = users.save(User.signUp("남진"));
        User second = users.save(User.signUp("영진"));
        loginMethods.saveAndFlush(new LoginMethod(first, LoginProvider.KAKAO, "1234567890"));

        assertViolates(() -> loginMethods.saveAndFlush(new LoginMethod(second, LoginProvider.KAKAO, "1234567890")),
                "login_methods_provider_user_key");
    }

    @Test
    void 대소문자만_다른_닉네임은_저장되지_않는다() {
        users.saveAndFlush(User.signUp("Baggy"));

        assertViolates(() -> users.saveAndFlush(User.signUp("baggy")), "users_nickname_lower_key");
    }

    @Test
    void 없는_회원에게_로그인_수단을_붙일_수_없다() {
        assertViolates(() -> jdbc.update(
                        "insert into login_methods (user_id, provider, provider_user_id) values (-1, 'KAKAO', '1')"),
                "login_methods_user_id_fkey");
    }

    // ── CHECK: 엔티티를 거치지 않는 잘못된 쓰기도 DB가 막는다 ──

    @Test
    void 공개_ID는_UUIDv7만_받는다() {
        assertViolates(() -> jdbc.update(
                        "insert into users (public_id, status, nickname) values (gen_random_uuid(), 'ACTIVE', '남진')"),
                "users_public_id_v7_check");
    }

    @Test
    void 정해진_상태만_받는다() {
        assertViolates(() -> jdbc.update("insert into users (status, nickname) values ('DELETED', '남진')"),
                "users_status_check");
    }

    @Test
    void 정해진_제공자만_받는다() {
        User user = users.saveAndFlush(User.signUp("남진"));

        assertViolates(() -> jdbc.update(
                        "insert into login_methods (user_id, provider, provider_user_id) values (?, 'FACEBOOK', '1')",
                        user.getId()),
                "login_methods_provider_check");
    }

    @Test
    void 탈퇴하지_않은_회원은_닉네임이_있어야_한다() {
        assertViolates(() -> jdbc.update("insert into users (status) values ('ACTIVE')"),
                "users_nickname_required_check");
    }

    @Test
    void 탈퇴한_회원에게_개인_정보가_남으면_안_된다() {
        assertViolates(() -> jdbc.update("insert into users (status, real_name) values ('WITHDRAWN', '남진')"),
                "users_withdrawn_anonymized_check");
    }

    @Test
    void 닉네임은_두_글자_이상이어야_한다() {
        assertViolates(() -> users.saveAndFlush(User.signUp("진")), "users_nickname_length_check");
    }

    @Test
    void NFD로_쓴_닉네임은_저장되지_않는다() {
        String nfd = Normalizer.normalize("남진", Normalizer.Form.NFD);
        assertThat(nfd).isNotEqualTo("남진"); // 화면에선 같아 보여도 다른 문자열

        assertViolates(() -> users.saveAndFlush(User.signUp(nfd)), "users_nickname_nfc_check");
    }

    @Test
    void NFD로_쓴_실명은_저장되지_않는다() {
        String nfd = Normalizer.normalize("남진", Normalizer.Form.NFD);

        assertViolates(() -> jdbc.update(
                        "insert into users (status, nickname, real_name) values ('ACTIVE', '남진', ?)", nfd),
                "users_real_name_nfc_check");
    }

    /** 쓰기가 거부되고, 그 이유가 기대한 제약인지 확인한다. */
    private static void assertViolates(ThrowingCallable write, String constraint) {
        assertThatThrownBy(write)
                .isInstanceOf(DataIntegrityViolationException.class)
                .rootCause()
                .isInstanceOfSatisfying(PSQLException.class, e ->
                        assertThat(e.getServerErrorMessage().getConstraint()).isEqualTo(constraint));
    }
}
