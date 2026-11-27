package scene.club;

/** 멤버십 상태 (ADR-001). 활동 아님의 이유는 {@link MembershipEndReason} */
public enum MembershipStatus {
    PENDING,
    ACTIVE,
    INACTIVE
}
