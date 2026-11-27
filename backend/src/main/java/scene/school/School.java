package scene.school;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Generated;

/**
 * 학교 (FN-2). 재학 인증이 아니라 대학 동아리에 붙는 이름표이고, 같은 학교 안 동아리 이름 중복을 막는 기준이다.
 * 학교는 서비스 관리자만 넣는다.
 */
@Entity
@Table(name = "schools")
public class School {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Generated
    @Column(nullable = false, insertable = false, updatable = false, unique = true)
    private UUID publicId;

    @Column(nullable = false, length = 50, unique = true)
    private String name;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected School() {
    }

    public School(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
