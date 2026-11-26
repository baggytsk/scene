package scene.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import org.hibernate.annotations.CreationTimestamp;

/**
 * 회원의 로그인 수단 하나 (ADR-001). 회원 1 : N.
 * (제공자, 제공자 ID)가 유일하고, 이메일이 같아도 다른 제공자 계정을 합치지 않는다.
 */
@Entity
@Table(name = "login_methods")
public class LoginMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 10)
    private LoginProvider provider;

    /** 제공자가 주는 회원 번호 (카카오 회원번호 · 구글 sub 등) */
    @Column(nullable = false, updatable = false)
    private String providerUserId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected LoginMethod() {
    }

    public LoginMethod(User user, LoginProvider provider, String providerUserId) {
        this.user = Objects.requireNonNull(user, "user");
        this.provider = Objects.requireNonNull(provider, "provider");
        this.providerUserId = Objects.requireNonNull(providerUserId, "providerUserId");
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public LoginProvider getProvider() {
        return provider;
    }

    public String getProviderUserId() {
        return providerUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
