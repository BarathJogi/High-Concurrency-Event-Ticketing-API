CREATE TABLE users
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR(255)        NOT NULL,
    email         VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255)        NOT NULL,
    role          VARCHAR(50)         NOT NULL,

    CONSTRAINT chk_user_role CHECK (role IN ('USER', 'ADMIN'))
);

CREATE TABLE events
(
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(255)             NOT NULL,
    venue       VARCHAR(255)             NOT NULL,
    event_date  TIMESTAMP WITH TIME ZONE NOT NULL, -- Use one timestamp instead of split date/time
    total_seats INTEGER                  NOT NULL
);

CREATE TABLE seats
(
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id        BIGINT      NOT NULL REFERENCES events (id),
    seat_number     VARCHAR(50) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    hold_expires_at TIMESTAMP WITH TIME ZONE,
    version         INTEGER     NOT NULL DEFAULT 0, -- MANDATORY for JPA @Version Optimistic Locking

    CONSTRAINT chk_seat_status CHECK (status IN ('AVAILABLE', 'HELD', 'BOOKED')),
    UNIQUE (event_id, seat_number)                  -- Prevents creating duplicate seat numbers for the same event
);

CREATE TABLE bookings
(
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         BIGINT      NOT NULL REFERENCES users (id),
    seat_id         BIGINT      NOT NULL REFERENCES seats (id),
    status          VARCHAR(20) NOT NULL,
    idempotency_key VARCHAR(255) UNIQUE, -- Crucial for retries
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_booking_status CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'EXPIRED'))
);





