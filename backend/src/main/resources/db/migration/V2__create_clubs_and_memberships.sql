-- 학교 (FN-2). 재학 인증이 아니라 대학 동아리에 붙는 이름표이고, 같은 학교 안 동아리 이름 중복을 막는 기준이다.
CREATE TABLE schools (
    id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    public_id  uuid        NOT NULL DEFAULT uuidv7(),
    name       varchar(50) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT schools_public_id_key UNIQUE (public_id),
    CONSTRAINT schools_public_id_v7_check CHECK (uuid_extract_version(public_id) = 7),
    CONSTRAINT schools_name_key UNIQUE (name)
);

-- 동아리 · 크루 (ADR-002 · FN-2). 행은 지우지 않는다. 삭제하면 deleted_at을 채우고 소유자를 비운다.
-- 소유자 · 가입 기본 역할 외래 키는 서로 가리키는 표라서 아래에서 따로 건다.
CREATE TABLE clubs (
    id                   bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    public_id            uuid         NOT NULL DEFAULT uuidv7(),
    type                 varchar(10)  NOT NULL,
    name                 varchar(30)  NOT NULL,
    school_id            bigint,
    region               varchar(20)  NOT NULL,
    intro                varchar(500),
    recruiting           boolean      NOT NULL DEFAULT false,
    join_policy          varchar(10)  NOT NULL DEFAULT 'APPROVAL',
    default_join_role_id bigint,
    owner_user_id        bigint,
    owner_status         varchar(10),
    deleted_at           timestamptz,
    created_at           timestamptz  NOT NULL DEFAULT now(),
    updated_at           timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT clubs_public_id_key UNIQUE (public_id),
    CONSTRAINT clubs_public_id_v7_check CHECK (uuid_extract_version(public_id) = 7),
    CONSTRAINT clubs_school_id_fkey FOREIGN KEY (school_id) REFERENCES schools (id),
    CONSTRAINT clubs_type_check CHECK (type IN ('CLUB', 'CREW')),
    CONSTRAINT clubs_join_policy_check CHECK (join_policy IN ('APPROVAL', 'AUTO')),
    -- 대학 동아리는 학교가 있고, 크루는 없다
    CONSTRAINT clubs_school_by_type_check CHECK ((type = 'CLUB') = (school_id IS NOT NULL)),
    CONSTRAINT clubs_name_length_check CHECK (char_length(name) BETWEEN 2 AND 30),
    CONSTRAINT clubs_name_nfc_check CHECK (name IS NFC NORMALIZED),
    -- 소유자 외래 키에 상태까지 넣으려고 두는 열. 값은 ACTIVE 하나뿐이다
    CONSTRAINT clubs_owner_status_check CHECK (owner_status = 'ACTIVE'),
    -- 살아 있는 동아리에는 소유자가 있고, 지운 동아리에는 없다 (지운 뒤에도 소유자가 묶여 탈퇴를 못 하는 일을 막는다).
    -- 외래 키는 빈 값이 하나라도 있으면 검사하지 않으므로 여기서 함께 묶는다
    CONSTRAINT clubs_owner_by_deleted_check
        CHECK ((deleted_at IS NULL) = (owner_user_id IS NOT NULL AND owner_status IS NOT NULL))
);

-- 같은 학교에 같은 이름 동아리 금지 (FN-2). 크루는 school_id가 비어 있어 걸리지 않는다. 지운 동아리 이름은 다시 쓸 수 있다.
CREATE UNIQUE INDEX clubs_school_name_key ON clubs (school_id, lower(name COLLATE "C")) WHERE deleted_at IS NULL;
CREATE INDEX clubs_school_id_idx ON clubs (school_id);

-- 역할 (ADR-002). 관리자 · 멤버 · 뷰어는 동아리를 만들 때 생기고, 이름을 바꿀 수 있다.
CREATE TABLE club_roles (
    id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    public_id  uuid        NOT NULL DEFAULT uuidv7(),
    club_id    bigint      NOT NULL,
    name       varchar(20) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT club_roles_public_id_key UNIQUE (public_id),
    CONSTRAINT club_roles_public_id_v7_check CHECK (uuid_extract_version(public_id) = 7),
    CONSTRAINT club_roles_club_id_fkey FOREIGN KEY (club_id) REFERENCES clubs (id),
    -- 복합 외래 키 (club_id, role_id)의 대상. 다른 동아리의 역할을 가리키지 못하게 한다
    CONSTRAINT club_roles_club_id_id_key UNIQUE (club_id, id),
    CONSTRAINT club_roles_name_length_check CHECK (char_length(name) BETWEEN 1 AND 20),
    CONSTRAINT club_roles_name_nfc_check CHECK (name IS NFC NORMALIZED)
);

CREATE UNIQUE INDEX club_roles_club_name_key ON club_roles (club_id, lower(name COLLATE "C"));

-- 역할의 권한 (ADR-002). 권한 하나 = 한 행. 두 동아리의 행을 잇지 않으므로 club_id를 두지 않는다.
CREATE TABLE club_role_permissions (
    role_id    bigint      NOT NULL,
    permission varchar(30) NOT NULL,

    CONSTRAINT club_role_permissions_pkey PRIMARY KEY (role_id, permission),
    CONSTRAINT club_role_permissions_role_id_fkey
        FOREIGN KEY (role_id) REFERENCES club_roles (id) ON DELETE CASCADE,
    CONSTRAINT club_role_permissions_permission_check CHECK (permission IN (
        'POST_WRITE', 'RESERVATION_CREATE', 'NOTICE_MANAGE', 'RESERVATION_APPROVE',
        'RESOURCE_MANAGE', 'MEMBER_MANAGE', 'CLUB_MANAGE'))
);

-- 멤버십 (ADR-001). 회원 × 동아리 한 쌍에 한 행. 다시 가입하면 같은 행을 다시 쓴다.
CREATE TABLE club_memberships (
    id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    club_id    bigint      NOT NULL,
    user_id    bigint      NOT NULL,
    status     varchar(10) NOT NULL,
    role_id    bigint,
    nickname   varchar(12),
    bio        varchar(30),
    applied_at timestamptz,
    joined_at  timestamptz,
    end_reason varchar(10),
    kicked_at  timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT club_memberships_club_id_fkey FOREIGN KEY (club_id) REFERENCES clubs (id),
    CONSTRAINT club_memberships_user_id_fkey FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT club_memberships_club_user_key UNIQUE (club_id, user_id),
    -- 소유자 외래 키 (club_id, user_id, status)의 대상. 위 제약이 있어 뜻은 같다
    CONSTRAINT club_memberships_club_user_status_key UNIQUE (club_id, user_id, status),
    CONSTRAINT club_memberships_status_check CHECK (status IN ('PENDING', 'ACTIVE', 'INACTIVE')),
    -- 역할은 활동 부원만 갖는다 (대기자가 권한을 갖는 구멍을 막는다)
    CONSTRAINT club_memberships_role_by_status_check CHECK ((status = 'ACTIVE') = (role_id IS NOT NULL)),
    -- 다른 동아리의 역할을 붙이지 못한다 (동아리 간 격리)
    CONSTRAINT club_memberships_role_fkey FOREIGN KEY (club_id, role_id) REFERENCES club_roles (club_id, id),
    CONSTRAINT club_memberships_end_reason_check CHECK (
        (status = 'INACTIVE') = (end_reason IS NOT NULL)
        AND (end_reason IS NULL
             OR end_reason IN ('LEFT', 'KICKED', 'REJECTED', 'CANCELED', 'EXPIRED', 'WITHDRAWN'))),
    CONSTRAINT club_memberships_nickname_length_check CHECK (char_length(nickname) BETWEEN 2 AND 12),
    CONSTRAINT club_memberships_nickname_nfc_check CHECK (nickname IS NFC NORMALIZED)
);

CREATE INDEX club_memberships_user_id_idx ON club_memberships (user_id);
CREATE INDEX club_memberships_club_status_idx ON club_memberships (club_id, status);
-- 역할을 지울 때 외래 키 검사가 그 역할의 부원을 찾는다
CREATE INDEX club_memberships_club_role_idx ON club_memberships (club_id, role_id);

-- 멤버십 이력. 덧붙이기만 한다 (앱은 UPDATE · DELETE 하지 않음. 역할 삭제 때 SET NULL이 역할 ID만 비운다).
CREATE TABLE membership_history (
    id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    club_id        bigint       NOT NULL,
    user_id        bigint       NOT NULL,
    action         varchar(20)  NOT NULL,
    actor_user_id  bigint,
    from_role_id   bigint,
    to_role_id     bigint,
    from_role_name varchar(20),
    to_role_name   varchar(20),
    reason         varchar(200),
    created_at     timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT membership_history_membership_fkey
        FOREIGN KEY (club_id, user_id) REFERENCES club_memberships (club_id, user_id),
    CONSTRAINT membership_history_actor_user_id_fkey FOREIGN KEY (actor_user_id) REFERENCES users (id),
    -- 역할이 지워지면 그 역할 ID만 비운다 (club_id는 그대로, PostgreSQL 15+ 열 지정 문법)
    CONSTRAINT membership_history_from_role_fkey FOREIGN KEY (club_id, from_role_id)
        REFERENCES club_roles (club_id, id) ON DELETE SET NULL (from_role_id),
    CONSTRAINT membership_history_to_role_fkey FOREIGN KEY (club_id, to_role_id)
        REFERENCES club_roles (club_id, id) ON DELETE SET NULL (to_role_id),
    CONSTRAINT membership_history_action_check CHECK (action IN (
        'APPLIED', 'APPROVED', 'AUTO_JOINED', 'REJECTED', 'CANCELED', 'EXPIRED',
        'LEFT', 'KICKED', 'ROLE_CHANGED', 'OWNER_TRANSFERRED', 'WITHDRAWN'))
);

CREATE INDEX membership_history_club_user_idx ON membership_history (club_id, user_id, created_at);

-- 서로 가리키는 외래 키. 동아리를 만드는 트랜잭션 안에서는 검사를 COMMIT(또는 SET CONSTRAINTS … IMMEDIATE)까지 미룬다.
-- 소유자는 그 동아리의 활동(ACTIVE) 멤버십이어야 한다. 소유자 멤버십을 INACTIVE로 바꾸는 UPDATE도 거부된다.
ALTER TABLE clubs
    ADD CONSTRAINT clubs_owner_membership_fkey
        FOREIGN KEY (id, owner_user_id, owner_status)
        REFERENCES club_memberships (club_id, user_id, status)
        DEFERRABLE INITIALLY DEFERRED,
    ADD CONSTRAINT clubs_default_join_role_fkey
        FOREIGN KEY (id, default_join_role_id)
        REFERENCES club_roles (club_id, id)
        DEFERRABLE INITIALLY DEFERRED;
