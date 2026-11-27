package scene.club;

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
import org.hibernate.annotations.UpdateTimestamp;

/** 동아리의 역할 (ADR-002). 관리자 · 멤버 · 뷰어는 동아리를 만들 때 생기고, 이름을 바꿀 수 있다 */
@Entity
@Table(name = "club_roles")
public class ClubRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Generated
    @Column(nullable = false, insertable = false, updatable = false, unique = true)
    private UUID publicId;

    @Column(nullable = false, updatable = false)
    private Long clubId;

    @Column(nullable = false, length = 20)
    private String name;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    protected ClubRole() {
    }

    public ClubRole(Long clubId, String name) {
        this.clubId = clubId;
        this.name = name;
    }

    public void rename(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public Long getClubId() {
        return clubId;
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
