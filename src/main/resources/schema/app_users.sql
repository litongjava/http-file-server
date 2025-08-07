CREATE TABLE app_users (
    id TEXT PRIMARY KEY,                      -- 用户唯一标识
    username TEXT UNIQUE,                        -- 用户邮箱
    email TEXT UNIQUE,                        -- 用户邮箱
    phone TEXT UNIQUE,                        -- 用户电话
    email_verified INTEGER NOT NULL DEFAULT 0,-- 邮箱是否已验证 (0=false, 1=true)
    updated_profile INTEGER NOT NULL DEFAULT 0,-- 是否更新过资料
    display_name TEXT,                        -- 显示名称
    bio TEXT,                                 -- 个人简介
    photo_url TEXT,                           -- 头像 URL
    background_url TEXT,                      -- 背景图 URL
    phone_number TEXT,                        -- 电话号码
    disabled INTEGER NOT NULL DEFAULT 0,      -- 是否禁用 (0=false,1=true)
    birthday DATETIME,                        -- 生日
    coin INTEGER NOT NULL DEFAULT 0,          -- 金币数量
    invited_by_user_id TEXT,                  -- 邀请人 ID
    of TEXT,                                  -- 注册的系统
    platform TEXT,                            -- 注册来源平台
    third_platform_url TEXT,                  -- 第三方平台 URL
    school_id INTEGER,                        -- 学校 ID
    user_type INTEGER NOT NULL DEFAULT 0,     -- 用户类型
    password_salt TEXT,                       -- 密码盐
    password_hash TEXT,                       -- 密码哈希
    provider_data TEXT NOT NULL DEFAULT '[]', -- JSON 存储
    mfa_info TEXT NOT NULL DEFAULT '[]',      -- JSON 存储
    metadata TEXT NOT NULL DEFAULT '{}',      -- JSON 存储
    user_info TEXT NOT NULL DEFAULT '{}',     -- JSON 存储
    google_id TEXT,
    google_info TEXT NOT NULL DEFAULT '{}',
    facebook_id TEXT,
    facebook_info TEXT NOT NULL DEFAULT '{}',
    twitter_id TEXT,
    twitter_info TEXT NOT NULL DEFAULT '{}',
    github_id TEXT,
    github_info TEXT NOT NULL DEFAULT '{}',
    wechat_id TEXT,
    wechat_info TEXT NOT NULL DEFAULT '{}',
    qq_id TEXT,
    qq_info TEXT NOT NULL DEFAULT '{}',
    weibo_id TEXT,
    weibo_info TEXT NOT NULL DEFAULT '{}',
    remark TEXT,                              -- 备注
    creator TEXT NOT NULL DEFAULT '',         -- 创建人
    create_time DATETIME NOT NULL DEFAULT (CURRENT_TIMESTAMP), -- 创建时间
    updater TEXT NOT NULL DEFAULT '',         -- 更新人
    update_time DATETIME NOT NULL DEFAULT (CURRENT_TIMESTAMP), -- 更新时间
    deleted INTEGER NOT NULL DEFAULT 0,       -- 逻辑删除标志
    tenant_id INTEGER NOT NULL DEFAULT 0      -- 租户 ID
);

-- 索引（仅对 deleted = 0 的行生效）
CREATE UNIQUE INDEX idx_app_users_username
    ON app_users(username)
    WHERE deleted = 0;
    
CREATE UNIQUE INDEX idx_app_users_email
    ON app_users(email)
    WHERE deleted = 0;

CREATE INDEX idx_app_users_tenant_id
    ON app_users(tenant_id)
    WHERE deleted = 0;

CREATE INDEX idx_app_users_user_type
    ON app_users(user_type)
    WHERE deleted = 0;

CREATE INDEX idx_app_users_invited_by
    ON app_users(invited_by_user_id)
    WHERE deleted = 0;

CREATE INDEX idx_app_users_school_id
    ON app_users(school_id)
    WHERE deleted = 0;

CREATE INDEX idx_app_users_platform
    ON app_users(platform)
    WHERE deleted = 0;

CREATE INDEX idx_app_users_create_time
    ON app_users(create_time)
    WHERE deleted = 0;

CREATE INDEX idx_app_users_update_time
    ON app_users(update_time)
    WHERE deleted = 0;

CREATE INDEX idx_app_users_tenant_user_type
    ON app_users(tenant_id, user_type)
    WHERE deleted = 0;

CREATE INDEX idx_app_users_coin
    ON app_users(coin)
    WHERE deleted = 0;