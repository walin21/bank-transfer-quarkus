-- Database bootstrap order follows foreign-key dependencies:
-- extension -> root entities -> dependent entities -> indexes.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Root entity: customers own accounts, beneficiaries and transfers.
CREATE TABLE IF NOT EXISTS customer (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_number VARCHAR(32) NOT NULL UNIQUE,
    full_name VARCHAR(160) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'BLOCKED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Account depends on customer and is referenced by transfers.
CREATE TABLE IF NOT EXISTS account (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES customer(id),
    account_number VARCHAR(32) NOT NULL UNIQUE,
    currency CHAR(3) NOT NULL CHECK (currency IN ('PEN', 'USD')),
    balance NUMERIC(19, 4) NOT NULL DEFAULT 0 CHECK (balance >= 0),
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'BLOCKED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Beneficiary depends on customer and is referenced by transfers.
CREATE TABLE IF NOT EXISTS beneficiary (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES customer(id),
    alias VARCHAR(80) NOT NULL,
    bank_name VARCHAR(120) NOT NULL,
    account_number VARCHAR(32) NOT NULL,
    currency CHAR(3) NOT NULL CHECK (currency IN ('PEN', 'USD')),
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'BLOCKED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (customer_id, alias)
);

-- Transfer depends on customer, account and beneficiary.
CREATE TABLE IF NOT EXISTS transfer (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customer(id),
    source_account_id UUID NOT NULL REFERENCES account(id),
    beneficiary_id UUID NOT NULL REFERENCES beneficiary(id),
    amount NUMERIC(19, 4) NOT NULL CHECK (amount > 0),
    source_currency CHAR(3) NOT NULL CHECK (source_currency IN ('PEN', 'USD')),
    target_currency CHAR(3) NOT NULL CHECK (target_currency IN ('PEN', 'USD')),
    exchange_rate NUMERIC(19, 10),
    converted_amount NUMERIC(19, 4) NOT NULL CHECK (converted_amount > 0),
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'COMPLETED', 'REJECTED')),
    failure_reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at TIMESTAMPTZ
);

-- History depends on transfer and stores every status transition.
CREATE TABLE IF NOT EXISTS transfer_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transfer_id UUID NOT NULL REFERENCES transfer(id),
    previous_status VARCHAR(16) CHECK (previous_status IN ('PENDING', 'COMPLETED', 'REJECTED')),
    new_status VARCHAR(16) NOT NULL CHECK (new_status IN ('PENDING', 'COMPLETED', 'REJECTED')),
    reason VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Foreign-key lookup indexes used by the application queries.
CREATE INDEX IF NOT EXISTS idx_account_customer ON account(customer_id);
CREATE INDEX IF NOT EXISTS idx_beneficiary_customer ON beneficiary(customer_id);
CREATE INDEX IF NOT EXISTS idx_transfer_customer_created ON transfer(customer_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_transfer_history_transfer ON transfer_history(transfer_id, created_at);
