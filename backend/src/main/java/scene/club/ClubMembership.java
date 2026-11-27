package scene.club;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * 멤버십 (ADR-001). 회원 × 동아리 한 쌍에 한 행. 다시 가입하면 같은 행을 다시 쓴다.
 * 역할은 활동 부원만 갖는다 (DB CHECK). 다른 동아리의 역할은 DB 복합 외래 키가 거부한다.
 * 상태 · 역할을 바꾸는 서비스는 이 행을 먼저 {@code SELECT … FOR UPDATE}로 잡고, 같은 트랜잭션에서 이력을 쓴다.
 */
@Entity
@Table(name = "club_memberships")
public class ClubMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long clubId;

    @Column(nullable = false, updatable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MembershipStatus status;

    private Long roleId;

    @Column(length = 12)
    private String nickname;

    @Column(length = 30)
    private String bio;

    private Instant appliedAt;

    private Instant joinedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private MembershipEndReason endReason;

    private Instant kickedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    protected ClubMembership() {
    }

    private ClubMembership(Long clubId, Long userId, MembershipStatus status, Long roleId) {
        this.clubId = clubId;
        this.userId = userId;
        this.status = status;
        this.roleId = roleId;
    }

    /** 동아리를 만든 사람 (생성 3단계). 바로 활동 · 관리자 역할 */
    public static ClubMembership owner(Long clubId, Long userId, Long adminRoleId) {
        ClubMembership m = new ClubMembership(clubId, userId, MembershipStatus.ACTIVE, adminRoleId);
        m.joinedAt = Instant.now();
        return m;
    }

    /** 가입 신청 (FN-68). 승인 전이라 역할이 없다 */
    public static ClubMembership applicant(Long clubId, Long userId) {
        ClubMembership m = new ClubMembership(clubId, userId, MembershipStatus.PENDING, null);
        m.appliedAt = Instant.now();
        return m;
    }

    public Long getId() {
        return id;
    }

    public Long getClubId() {
        return clubId;
    }

    public Long getUserId() {
        return userId;
    }

    public MembershipStatus getStatus() {
        return status;
    }

    public Long getRoleId() {
        return roleId;
    }

    public String getNickname() {
        return nickname;
    }

    public String getBio() {
        return bio;
    }

    public Instant getAppliedAt() {
        return appliedAt;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public MembershipEndReason getEndReason() {
        return endReason;
    }

    public Instant getKickedAt() {
        return kickedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
