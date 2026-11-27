package scene.club;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import scene.school.School;
import scene.school.SchoolRepository;
import scene.user.User;
import scene.user.UserRepository;

/**
 * Flyway V2 스키마 위에서 동아리 간 격리 · 소유자 규칙을 확인한다 (PostgreSQL 컨테이너).
 * 소유자 · 가입 기본 역할 외래 키는 지연 검사라서, 서비스와 같은 방식으로 flush 뒤 SET CONSTRAINTS … IMMEDIATE로 확인한다.
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class ClubRepositoryTest {

    private static final String DEFERRED_FKS =
            "SET CONSTRAINTS clubs_owner_membership_fkey, clubs_default_join_role_fkey IMMEDIATE";

    /** IMMEDIATE는 트랜잭션 끝까지 남는다. 검사한 뒤 되돌리지 않으면 같은 트랜잭션의 다음 동아리 만들기가 1단계에서 거부된다 */
    private static final String DEFER_AGAIN =
            "SET CONSTRAINTS clubs_owner_membership_fkey, clubs_default_join_role_fkey DEFERRED";

    @Autowired
    UserRepository users;

    @Autowired
    ClubRepository clubs;

    @Autowired
    ClubRoleRepository roles;

    @Autowired
    ClubMembershipRepository memberships;

    @Autowired
    SchoolRepository schools;

    @Autowired
    ClubRolePermissionRepository permissions;

    @Autowired
    MembershipHistoryRepository history;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void 동아리_만들기_5단계를_지키면_소유자_멤버십과_가입_기본_역할이_맞게_저장된다() {
        CreatedClub created = createCrew("소유자", "크루A");

        assertThat(created.club().getPublicId().version()).isEqualTo(7);
        assertThat(created.club().getDefaultJoinRoleId()).isEqualTo(created.member().getId());
        assertThat(memberships.findByClubIdAndUserId(created.club().getId(), created.owner().getId()))
                .map(ClubMembership::getStatus)
                .contains(MembershipStatus.ACTIVE);
    }

    @Test
    void 소유자_멤버십_없이_만든_동아리는_거부된다() {
        User owner = users.save(User.signUp("소유자"));
        clubs.saveAndFlush(Club.create(ClubType.CREW, "크루A", null, "서울", owner.getId()));

        assertViolates(() -> jdbc.execute(DEFERRED_FKS), "clubs_owner_membership_fkey");
    }

    @Test
    void 소유자_멤버십을_활동_아님으로_바꾸면_거부된다() {
        CreatedClub created = createCrew("소유자", "크루A");
        jdbc.update("""
                UPDATE club_memberships SET status = 'INACTIVE', role_id = NULL, end_reason = 'LEFT'
                WHERE club_id = ? AND user_id = ?""", created.club().getId(), created.owner().getId());

        assertViolates(() -> jdbc.execute(DEFERRED_FKS), "clubs_owner_membership_fkey");
    }

    @Test
    void 다른_동아리의_역할은_붙일_수_없다() {
        CreatedClub a = createCrew("소유자A", "크루A");
        CreatedClub b = createCrew("소유자B", "크루B");

        assertViolates(() -> jdbc.update(
                "UPDATE club_memberships SET role_id = ? WHERE club_id = ? AND user_id = ?",
                b.admin().getId(), a.club().getId(), a.owner().getId()),
                "club_memberships_role_fkey");
    }

    // ── 이름 · 학교 ──────────────────────────────

    @Test
    void 같은_학교에_대소문자만_다른_같은_이름_동아리는_거부된다() {
        School school = schools.save(new School("테스트대학교"));
        createClub(ClubType.CLUB, school.getId(), "소유자A", "Hype");
        User other = users.save(User.signUp("소유자B"));

        assertViolates(() -> clubs.saveAndFlush(
                        Club.create(ClubType.CLUB, "hype", school.getId(), "서울", other.getId())),
                "clubs_school_name_key");
    }

    @Test
    void 학교가_다르면_같은_이름_동아리를_만들_수_있다() {
        School a = schools.save(new School("가대학교"));
        School b = schools.save(new School("나대학교"));

        createClub(ClubType.CLUB, a.getId(), "소유자A", "Hype");
        createClub(ClubType.CLUB, b.getId(), "소유자B", "Hype");

        assertThat(clubs.count()).isEqualTo(2);
    }

    @Test
    void 크루는_이름이_같아도_만들_수_있다() {
        createCrew("소유자A", "크루A");
        createCrew("소유자B", "크루A");

        assertThat(clubs.count()).isEqualTo(2);
    }

    @Test
    void 지운_동아리의_이름은_같은_학교에서_다시_쓸_수_있다() {
        School school = schools.save(new School("테스트대학교"));
        CreatedClub old = createClub(ClubType.CLUB, school.getId(), "소유자A", "Hype");
        deleteClub(old.club().getId());

        createClub(ClubType.CLUB, school.getId(), "소유자B", "Hype");

        assertThat(clubs.count()).isEqualTo(2);
    }

    @Test
    void 학교_없는_대학_동아리는_거부된다() {
        User owner = users.save(User.signUp("소유자"));

        assertViolates(() -> clubs.saveAndFlush(Club.create(ClubType.CLUB, "Hype", null, "서울", owner.getId())),
                "clubs_school_by_type_check");
    }

    @Test
    void 학교_달린_크루는_거부된다() {
        School school = schools.save(new School("테스트대학교"));
        User owner = users.save(User.signUp("소유자"));

        assertViolates(() -> clubs.saveAndFlush(
                        Club.create(ClubType.CREW, "크루A", school.getId(), "서울", owner.getId())),
                "clubs_school_by_type_check");
    }

    // ── 지운 동아리 ──────────────────────────────

    @Test
    void 지운_동아리에_소유자가_남아_있으면_거부된다() {
        CreatedClub created = createCrew("소유자", "크루A");

        assertViolates(() -> jdbc.update("UPDATE clubs SET deleted_at = now() WHERE id = ?", created.club().getId()),
                "clubs_owner_by_deleted_check");
    }

    @Test
    void 지운_동아리의_소유자였던_사람은_멤버십을_끝낼_수_있다() {
        CreatedClub created = createCrew("소유자", "크루A");
        deleteClub(created.club().getId());

        jdbc.update("""
                UPDATE club_memberships SET status = 'INACTIVE', role_id = NULL, end_reason = 'WITHDRAWN'
                WHERE club_id = ? AND user_id = ?""", created.club().getId(), created.owner().getId());
        jdbc.execute(DEFERRED_FKS);

        assertThat(jdbc.queryForObject(
                "SELECT end_reason FROM club_memberships WHERE club_id = ? AND user_id = ?",
                String.class, created.club().getId(), created.owner().getId()))
                .isEqualTo("WITHDRAWN");
    }

    // ── 멤버십 상태 ──────────────────────────────

    @Test
    void 역할_없는_활동_부원은_거부된다() {
        CreatedClub created = createCrew("소유자", "크루A");
        User member = users.save(User.signUp("부원"));
        memberships.saveAndFlush(ClubMembership.applicant(created.club().getId(), member.getId()));

        assertViolates(() -> jdbc.update(
                        "UPDATE club_memberships SET status = 'ACTIVE' WHERE club_id = ? AND user_id = ?",
                        created.club().getId(), member.getId()),
                "club_memberships_role_by_status_check");
    }

    @Test
    void 이유_없이_활동_아님이_된_멤버십은_거부된다() {
        CreatedClub created = createCrew("소유자", "크루A");
        User member = users.save(User.signUp("부원"));
        memberships.saveAndFlush(ClubMembership.applicant(created.club().getId(), member.getId()));

        assertViolates(() -> jdbc.update(
                        "UPDATE club_memberships SET status = 'INACTIVE' WHERE club_id = ? AND user_id = ?",
                        created.club().getId(), member.getId()),
                "club_memberships_end_reason_check");
    }

    @Test
    void 한_사람은_한_동아리에_멤버십을_하나만_가진다() {
        CreatedClub created = createCrew("소유자", "크루A");

        assertViolates(() -> memberships.saveAndFlush(
                        ClubMembership.applicant(created.club().getId(), created.owner().getId())),
                "club_memberships_club_user_key");
    }

    // ── 역할 · 권한 · 이력 ──────────────────────────

    @Test
    void 한_동아리에_대소문자만_다른_같은_이름_역할은_거부된다() {
        CreatedClub created = createCrew("소유자", "크루A");
        roles.saveAndFlush(new ClubRole(created.club().getId(), "OB"));

        assertViolates(() -> roles.saveAndFlush(new ClubRole(created.club().getId(), "ob")),
                "club_roles_club_name_key");
    }

    @Test
    void 가입_기본_역할은_지울_수_없다() {
        CreatedClub created = createCrew("소유자", "크루A");
        roles.delete(created.member());
        roles.flush();

        assertViolates(() -> jdbc.execute(DEFERRED_FKS), "clubs_default_join_role_fkey");
    }

    @Test
    void 권한은_7개_값만_받는다() {
        CreatedClub created = createCrew("소유자", "크루A");
        ClubRole ob = roles.save(new ClubRole(created.club().getId(), "OB"));
        permissions.saveAndFlush(new ClubRolePermission(ob.getId(), ClubPermission.POST_WRITE));

        assertThat(permissions.findByIdRoleId(ob.getId()))
                .extracting(p -> p.getId().getPermission())
                .containsExactly(ClubPermission.POST_WRITE);
        assertViolates(() -> jdbc.update(
                        "INSERT INTO club_role_permissions (role_id, permission) VALUES (?, 'ADMIN')", ob.getId()),
                "club_role_permissions_permission_check");
    }

    @Test
    void 지운_역할의_권한은_함께_지워진다() {
        CreatedClub created = createCrew("소유자", "크루A");
        ClubRole ob = roles.save(new ClubRole(created.club().getId(), "OB"));
        permissions.saveAndFlush(new ClubRolePermission(ob.getId(), ClubPermission.POST_WRITE));

        jdbc.update("DELETE FROM club_roles WHERE id = ?", ob.getId());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM club_role_permissions WHERE role_id = ?",
                Integer.class, ob.getId())).isZero();
    }

    @Test
    void 역할을_지우면_이력의_역할_ID만_비고_이름은_남는다() {
        CreatedClub created = createCrew("소유자", "크루A");
        ClubRole ob = roles.save(new ClubRole(created.club().getId(), "OB"));
        history.saveAndFlush(new MembershipHistory(created.club().getId(), created.owner().getId(),
                MembershipAction.ROLE_CHANGED, created.owner().getId(), created.admin(), ob, null));

        jdbc.update("DELETE FROM club_roles WHERE id = ?", ob.getId());

        var row = jdbc.queryForMap("SELECT club_id, to_role_id, to_role_name, from_role_id FROM membership_history");
        assertThat(row.get("to_role_id")).isNull();
        assertThat(row.get("to_role_name")).isEqualTo("OB");
        assertThat(row.get("club_id")).isEqualTo(created.club().getId());
        assertThat(row.get("from_role_id")).isEqualTo(created.admin().getId());
    }

    @Test
    void 이력은_다른_동아리의_역할을_가리킬_수_없다() {
        CreatedClub a = createCrew("소유자A", "크루A");
        CreatedClub b = createCrew("소유자B", "크루B");

        assertViolates(() -> history.saveAndFlush(new MembershipHistory(a.club().getId(), a.owner().getId(),
                        MembershipAction.ROLE_CHANGED, a.owner().getId(), null, b.admin(), null)),
                "membership_history_to_role_fkey");
    }

    private void deleteClub(Long clubId) {
        jdbc.update("UPDATE clubs SET deleted_at = now(), owner_user_id = NULL, owner_status = NULL WHERE id = ?",
                clubId);
    }

    private CreatedClub createCrew(String ownerNickname, String name) {
        return createClub(ClubType.CREW, null, ownerNickname, name);
    }

    /** 동아리 만들기는 한 트랜잭션 1~5단계 (서비스가 생기면 그쪽으로 옮긴다) */
    private CreatedClub createClub(ClubType type, Long schoolId, String ownerNickname, String name) {
        User owner = users.save(User.signUp(ownerNickname));
        Club club = clubs.save(Club.create(type, name, schoolId, "서울", owner.getId()));
        List<ClubRole> created = roles.saveAll(List.of(
                new ClubRole(club.getId(), "관리자"),
                new ClubRole(club.getId(), "멤버"),
                new ClubRole(club.getId(), "뷰어")));
        memberships.save(ClubMembership.owner(club.getId(), owner.getId(), created.get(0).getId()));
        club.changeDefaultJoinRole(created.get(1).getId());
        clubs.flush();
        jdbc.execute(DEFERRED_FKS);
        jdbc.execute(DEFER_AGAIN);
        return new CreatedClub(owner, club, created.get(0), created.get(1));
    }

    private record CreatedClub(User owner, Club club, ClubRole admin, ClubRole member) {
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
