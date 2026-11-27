package scene.club;

/** 역할에 붙는 권한 7개 (ADR-002). 보기는 활동 멤버십이면 누구나라서 여기에 없다 */
public enum ClubPermission {
    POST_WRITE,
    RESERVATION_CREATE,
    NOTICE_MANAGE,
    RESERVATION_APPROVE,
    RESOURCE_MANAGE,
    MEMBER_MANAGE,
    CLUB_MANAGE
}
