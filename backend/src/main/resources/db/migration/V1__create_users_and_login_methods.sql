-- 회원 (ADR-001 · FN-1). 공개 영역 · 동아리 활동의 주체.
-- 행은 지우지 않는다. 서비스를 탈퇴하면 개인 정보를 비우고(익명화) 글 · 댓글의 작성자로만 남긴다.
CREATE TABLE users (
    id                     bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    public_id              uuid        NOT NULL DEFAULT uuidv7(),
    status                 varchar(20) NOT NULL,
    nickname               varchar(12),
    nickname_changed_at    timestamptz,
    bio                    varchar(30),
    real_name              varchar(20),
    real_name_corrected_at timestamptz,
    real_name_verified_at  timestamptz,
    created_at             timestamptz NOT NULL DEFAULT now(),
    updated_at             timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT users_public_id_key UNIQUE (public_id),
    CONSTRAINT users_public_id_v7_check CHECK (uuid_extract_version(public_id) = 7),
    CONSTRAINT users_status_check
        CHECK (status IN ('ACTIVE', 'WITHDRAWAL_PENDING', 'WITHDRAWN', 'SUSPENDED')),
    -- 탈퇴(익명화)를 마친 회원만 닉네임이 비어 있다
    CONSTRAINT users_nickname_required_check
        CHECK (status = 'WITHDRAWN' OR nickname IS NOT NULL),
    -- 탈퇴를 마치면 개인 정보가 남아 있으면 안 된다
    CONSTRAINT users_withdrawn_anonymized_check
        CHECK (status <> 'WITHDRAWN' OR (nickname IS NULL AND bio IS NULL AND real_name IS NULL)),
    CONSTRAINT users_nickname_length_check CHECK (char_length(nickname) BETWEEN 2 AND 12),
    CONSTRAINT users_real_name_length_check CHECK (char_length(real_name) BETWEEN 2 AND 20),
    -- 화면에선 같아 보이는 NFD(자모 분리) 값이 다른 닉네임 · 실명으로 들어오지 않게 한다
    CONSTRAINT users_nickname_nfc_check CHECK (nickname IS NFC NORMALIZED),
    CONSTRAINT users_real_name_nfc_check CHECK (real_name IS NFC NORMALIZED)
);

-- 대소문자만 다른 닉네임도 같은 닉네임으로 본다 (FN-1).
-- COLLATE "C": OS 로케일과 상관없이 ASCII만 접고 바이트 순으로 정렬한다 (OS를 올려도 인덱스가 안 깨짐).
-- 조회할 때도 같은 식을 써야 이 인덱스를 탄다.
-- 탈퇴 대기(30일) 동안에는 닉네임이 남아 있어 다른 사람이 쓸 수 없다.
CREATE UNIQUE INDEX users_nickname_lower_key ON users (lower(nickname COLLATE "C"));

-- 로그인 수단 (ADR-001 · 008). 회원 1 : N.
-- 회원은 (제공자, 제공자 ID)로만 찾는다. 이메일은 식별자가 아니라서 저장하지 않는다.
CREATE TABLE login_methods (
    id               bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id          bigint       NOT NULL,
    provider         varchar(10)  NOT NULL,
    provider_user_id varchar(255) NOT NULL,
    created_at       timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT login_methods_user_id_fkey FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT login_methods_provider_check CHECK (provider IN ('KAKAO', 'NAVER', 'GOOGLE', 'APPLE')),
    CONSTRAINT login_methods_provider_user_key UNIQUE (provider, provider_user_id)
);

CREATE INDEX login_methods_user_id_idx ON login_methods (user_id);
