-- Seed order follows the schema dependencies: customers -> accounts -> beneficiaries.

-- Root customers used by the demo and administrator flows.
INSERT INTO customer (id, document_number, full_name, email)
VALUES ('11111111-1111-1111-1111-111111111111', '00000001', 'Cliente Demo', 'demo@bank-transfer.local'),
    ('11111111-1111-1111-9999-111111111111', '00000009', 'Admin Demo', 'admin@bank-transfer.local'),
    ('44444444-4444-4444-4444-444444444444', '00000002', 'Cliente Destino Demo', 'destination@bank-transfer.local')
ON CONFLICT (id) DO NOTHING;

-- Accounts depend on the customers above.
INSERT INTO account (id, customer_id, account_number, currency, balance)
VALUES
    ('22222222-2222-2222-2222-222222222221', '11111111-1111-1111-1111-111111111111', '001-000000001', 'PEN', 10000.00),
    ('22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', '001-000000002', 'USD', 2500.00)
ON CONFLICT (id) DO NOTHING;

-- Cuenta interna de otro cliente para probar transferencias entre cuentas.
INSERT INTO account (id, customer_id, account_number, currency, balance)
VALUES ('44444444-4444-4444-4444-444444444441', '44444444-4444-4444-4444-444444444444', '002-000000099', 'USD', 100.00)
ON CONFLICT (id) DO NOTHING;

-- Beneficiary depends on the source customer and points to the destination account.
INSERT INTO beneficiary (id, customer_id, alias, bank_name, account_number, currency)
VALUES ('33333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111111', 'Beneficiario Demo', 'Banco Demo', '002-000000099', 'USD')
ON CONFLICT (id) DO UPDATE SET
    account_number = EXCLUDED.account_number,
    currency = EXCLUDED.currency;
