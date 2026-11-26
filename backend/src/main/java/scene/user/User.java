package scene.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * 서비스 회원 (ADR-001). 공개 영역 · 동아리 활동의 주체.
 * 밖으로 내보내는 ID는 {@link #publicId}이고, {@link #id}는 DB 안에서만 쓴다.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 공개 ID (UUIDv7). DB 기본값 uuidv7()이 만들고, 저장 직후 Hibernate가 읽어 온다 */
    @Generated
    @Column(nullable = false, insertable = false, updatable = false, unique = true)
    private UUID publicId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column(length = 12)
    private String nickname;

    private Instant nicknameChangedAt;

    @Column(length = 30)
    private String bio;

    @Column(length = 20)
    private String realName;

    private Instant realNameCorrectedAt;

    private Instant realNameVerifiedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    /** JPA용. 코드에서는 {@link #signUp}을 쓴다 */
    protected User() {
    }

    private User(String nickname) {
        this.status = UserStatus.ACTIVE;
        this.nickname = Objects.requireNonNull(nickname, "nickname");
    }

    /** 가입 화면(프로필 입력)을 마친 회원. 닉네임 규칙 검사는 가입 서비스에서 한다 */
    public static User signUp(String nickname) {
        return new User(nickname);
    }

    public Long getId() {
        return id;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public UserStatus getStatus() {
        return status;
    }

    public String getNickname() {
        return nickname;
    }

    public Instant getNicknameChangedAt() {
        return nicknameChangedAt;
    }

    public String getBio() {
        return bio;
    }

    public String getRealName() {
        return realName;
    }

    public Instant getRealNameCorrectedAt() {
        return realNameCorrectedAt;
    }

    public Instant getRealNameVerifiedAt() {
        return realNameVerifiedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
