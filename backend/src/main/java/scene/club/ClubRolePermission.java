package scene.club;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.Objects;
import org.springframework.data.domain.Persistable;

/**
 * 역할이 가진 권한 하나 (ADR-002). 권한을 바꿀 때는 차이만 반영한다 (빠진 것만 지우고 새것만 넣기).
 * Hibernate는 INSERT를 DELETE보다 먼저 보내서 "모두 지우고 다시 넣기"는 PK 위반이 난다.
 * {@link Persistable}을 구현해 save()가 merge(추가 SELECT)로 가지 않게 한다.
 */
@Entity
@Table(name = "club_role_permissions")
public class ClubRolePermission implements Persistable<ClubRolePermission.Id> {

    @EmbeddedId
    private Id id;

    @Transient
    private boolean isNew = true;

    protected ClubRolePermission() {
    }

    public ClubRolePermission(Long roleId, ClubPermission permission) {
        this.id = new Id(roleId, permission);
    }

    @Override
    public Id getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    @Embeddable
    public static class Id {

        @Column(nullable = false)
        private Long roleId;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 30)
        private ClubPermission permission;

        protected Id() {
        }

        public Id(Long roleId, ClubPermission permission) {
            this.roleId = roleId;
            this.permission = permission;
        }

        public Long getRoleId() {
            return roleId;
        }

        public ClubPermission getPermission() {
            return permission;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Id other
                    && Objects.equals(roleId, other.roleId)
                    && permission == other.permission;
        }

        @Override
        public int hashCode() {
            return Objects.hash(roleId, permission);
        }
    }
}
