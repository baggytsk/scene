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
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * 동아리 · 크루 (ADR-002 · FN-2). 행은 지우지 않는다. 삭제하면 {@link #deletedAt}을 채우고 소유자를 비운다.
 *
 * <p>외래 키 열은 연관이 아니라 {@code Long}으로 둔다.
 * 소유자 · 가입 기본 역할 외래 키는 지연(DEFERRABLE) 검사라, 이 값을 바꾸는 메서드는
 * flush 뒤 {@code SET CONSTRAINTS … IMMEDIATE}로 끝낸다.
 */
@Entity
@Table(name = "clubs")
public class Club {

    /** 소유자 외래 키에 상태까지 넣으려고 두는 값. 소유자 멤버십은 항상 활동 중이다 */
    private static final String OWNER_STATUS_ACTIVE = "ACTIVE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Generated
    @Column(nullable = false, insertable = false, updatable = false, unique = true)
    private UUID publicId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ClubType type;

    @Column(nullable = false, length = 30)
    private String name;

    private Long schoolId;

    @Column(nullable = false, length = 20)
    private String region;

    @Column(length = 500)
    private String intro;

    @Column(nullable = false)
    private boolean recruiting;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private JoinPolicy joinPolicy;

    /** 동아리를 만드는 트랜잭션 안에서만 비어 있다 */
    private Long defaultJoinRoleId;

    private Long ownerUserId;

    @Column(length = 10)
    private String ownerStatus;

    private Instant deletedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    protected Club() {
    }

    private Club(ClubType type, String name, Long schoolId, String region, Long ownerUserId) {
        this.type = type;
        this.name = name;
        this.schoolId = schoolId;
        this.region = region;
        this.recruiting = false;
        this.joinPolicy = JoinPolicy.APPROVAL;
        this.ownerUserId = ownerUserId;
        this.ownerStatus = OWNER_STATUS_ACTIVE;
    }

    /** 생성 1단계. 역할 · 소유자 멤버십 · 가입 기본 역할은 같은 트랜잭션에서 이어서 만든다 */
    public static Club create(ClubType type, String name, Long schoolId, String region, Long ownerUserId) {
        return new Club(type, name, schoolId, region, ownerUserId);
    }

    /** 생성 4단계 · 가입 기본 역할 바꾸기 */
    public void changeDefaultJoinRole(Long roleId) {
        this.defaultJoinRoleId = roleId;
    }

    public Long getId() {
        return id;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public ClubType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public Long getSchoolId() {
        return schoolId;
    }

    public String getRegion() {
        return region;
    }

    public String getIntro() {
        return intro;
    }

    public boolean isRecruiting() {
        return recruiting;
    }

    public JoinPolicy getJoinPolicy() {
        return joinPolicy;
    }

    public Long getDefaultJoinRoleId() {
        return defaultJoinRoleId;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
