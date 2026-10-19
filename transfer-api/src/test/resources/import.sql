INSERT INTO customer (id, document_number, full_name, email, status, created_at)
VALUES ('11111111-1111-1111-1111-111111111111', 'DOC-TEST-1', 'Test Customer', 'customer@test.local', 'ACTIVE', CURRENT_TIMESTAMP);
INSERT INTO account (id, customer_id, account_number, currency, balance, status, created_at)
VALUES ('22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'TEST-ACCOUNT-1', 'PEN', 1000.00, 'ACTIVE', CURRENT_TIMESTAMP);

