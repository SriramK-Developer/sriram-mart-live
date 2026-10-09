-- Handy read-only queries for checking what the website stored. Works on H2, MySQL and SQL Server.
-- (On MySQL add LIMIT 20 instead of the FETCH clause.)

-- Registered accounts (passwords are stored only as BCrypt hashes)
SELECT id, username, email, role, enabled, approved, created_at FROM users ORDER BY created_at DESC;

-- Latest orders and who placed them
SELECT o.order_no, u.username, o.status, o.payment_method, o.payment_status, o.total, o.created_at
FROM orders o JOIN users u ON u.id = o.user_id
ORDER BY o.created_at DESC OFFSET 0 ROWS FETCH NEXT 20 ROWS ONLY;

-- What each order contains
SELECT o.order_no, i.product_name, i.quantity, i.unit_price, i.line_total, i.status
FROM order_items i JOIN orders o ON o.id = i.order_id
ORDER BY o.created_at DESC OFFSET 0 ROWS FETCH NEXT 50 ROWS ONLY;

-- Live stock and units sold (stock drops automatically when an order is placed)
SELECT sku, name, stock, sold_count FROM products ORDER BY sold_count DESC OFFSET 0 ROWS FETCH NEXT 20 ROWS ONLY;

-- Products that are running low
SELECT sku, name, stock FROM products WHERE stock <= 10 AND active = 1 ORDER BY stock;
