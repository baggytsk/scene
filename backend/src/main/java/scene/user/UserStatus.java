package scene.user;

/** 회원 상태 (FN-1 · FN-8). */
public enum UserStatus {
    /** 활동 중 */
    ACTIVE,
    /** 탈퇴 신청 뒤 30일. 로그인하면 복구할 수 있고, 닉네임은 다른 사람이 쓸 수 없다 */
    WITHDRAWAL_PENDING,
    /** 탈퇴 완료. 개인 정보를 비운 상태로 작성자 기록에만 남는다 */
    WITHDRAWN,
    /** 서비스 관리자가 이용을 정지함 */
    SUSPENDED
}
