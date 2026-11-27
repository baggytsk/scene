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
import org.hibernate.annotations.Immutable;

/**
 * 멤버십 이력. 덧붙이기만 한다 ({@link Immutable}: Hibernate가 UPDATE를 보내지 않음).
 * 역할이 지워지면 DB가 역할 ID만 비우고, 역할 이름 복사본은 남는다.
 */
@Entity
@Immutable
@Table(name = "membership_history")
public class MembershipHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long clubId;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MembershipAction action;

    /** 한 사람. 자동 만료처럼 시스템이 한 일은 비어 있다 */
    private Long actorUserId;

    private Long fromRoleId;

    private Long toRoleId;

    @Column(length = 20)
    private String fromRoleName;

    @Column(length = 20)
    private String toRoleName;

    @Column(length = 200)
    private String reason;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected MembershipHistory() {
    }

    public MembershipHistory(Long clubId, Long userId, MembershipAction action, Long actorUserId,
                             ClubRole fromRole, ClubRole toRole, String reason) {
        this.clubId = clubId;
        this.userId = userId;
        this.action = action;
        this.actorUserId = actorUserId;
        if (fromRole != null) {
            this.fromRoleId = fromRole.getId();
            this.fromRoleName = fromRole.getName();
        }
        if (toRole != null) {
            this.toRoleId = toRole.getId();
            this.toRoleName = toRole.getName();
        }
        this.reason = reason;
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

    public MembershipAction getAction() {
        return action;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public Long getFromRoleId() {
        return fromRoleId;
    }

    public Long getToRoleId() {
        return toRoleId;
    }

    public String getFromRoleName() {
        return fromRoleName;
    }

    public String getToRoleName() {
        return toRoleName;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
