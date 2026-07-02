INSERT INTO categories (id, name, slug, image_url, active) VALUES
  (1, 'Aata', 'aata', '/images/categories/aata.png', true),
  (2, 'Dal', 'dal', '/images/categories/dal.png', true),
  (3, 'Rice', 'rice', '/images/categories/rice.png', true)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO products (id, name, slug, description, price, original_price, unit, image_url, featured, active, category_id) VALUES
  (1, 'Sharbati Aata', 'sharbati-aata', 'Fresh chakki atta for soft rotis', 420.00, 460.00, '10 kg', '/images/products/aata.png', true, true, 1),
  (2, 'Toor Dal', 'toor-dal', 'Daily cooking premium dal', 165.00, 185.00, '1 kg', '/images/products/dal.png', true, true, 2),
  (3, 'Basmati Rice', 'basmati-rice', 'Long grain basmati rice', 520.00, 575.00, '5 kg', '/images/products/rice.png', false, true, 3)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO users (id, name, email, password, phone, email_verified, blocked, role, delivery_status) VALUES
  (1, 'Demo Customer', 'customer@akstore.com', '$2a$10$X6te5oKlgRkr4weFSXZomOudxzUIBpp4F3YbCI82eGT67vK9SlIOu', '9999999999', true, false, 'CUSTOMER', NULL),
  (2, 'Store Owner', 'owner@akstore.com', '$2a$10$C/8sUuER6pNBYfWME5soi.q1HhXvpvKlF9/ryz8mV0yDkBsRObtOW', '8888888888', true, false, 'ADMIN', NULL),
  (3, 'Delivery Partner', 'delivery@akstore.com', '$2a$10$n1WVsTNMA.2BQxm6TnxAxOIT5ib9fKYheTSzRSClGI.h71dhwDH9y', '9876543210', true, false, 'DELIVERY', 'AT_SHOP')
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  email = VALUES(email),
  password = VALUES(password),
  phone = VALUES(phone),
  email_verified = VALUES(email_verified),
  blocked = VALUES(blocked),
  role = VALUES(role),
  delivery_status = VALUES(delivery_status);

INSERT INTO coupons (id, code, discount_type, discount_value, minimum_order_amount, max_uses_per_user, max_total_uses, expiry_date, first_order_only, active) VALUES
  (1, 'AK50', 'FLAT', 50.00, 499.00, 2, 500, '2027-12-31', false, true),
  (2, 'FIRST10', 'PERCENT', 10.00, 0.00, 1, 1000, '2027-12-31', true, true)
ON DUPLICATE KEY UPDATE
  code = VALUES(code),
  discount_type = VALUES(discount_type),
  discount_value = VALUES(discount_value),
  minimum_order_amount = VALUES(minimum_order_amount),
  max_uses_per_user = VALUES(max_uses_per_user),
  max_total_uses = VALUES(max_total_uses),
  expiry_date = VALUES(expiry_date),
  first_order_only = VALUES(first_order_only),
  active = VALUES(active);

INSERT IGNORE INTO store_settings (id, setting_key, setting_value) VALUES
  (1, 'store_name', 'AK General Store'),
  (2, 'support_phone', '9483989109'),
  (3, 'support_email', 'support@akgeneralstore.com'),
  (4, 'free_delivery_threshold', '499'),
  (5, 'delivery_charge', '40'),
  (6, 'enabled_payments', 'COD,UPI,RAZORPAY'),
  (7, 'service_radius_km', '25'),
  (8, 'store_locations', 'AK General Store Main|28.0162|74.9642|25|https://maps.app.goo.gl/YY4f8NfB9sTfRQrH7'),
  (9, 'upi_merchant_name', 'AK General Store'),
  (10, 'upi_id', 'support@akgeneralstore'),
  (11, 'delivery_base_payout_amount', '20'),
  (12, 'delivery_additional_payout_amount', '10');

UPDATE store_settings
SET setting_value = '499'
WHERE setting_key = 'free_delivery_threshold'
  AND setting_value = '299';
