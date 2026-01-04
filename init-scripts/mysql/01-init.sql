-- PayShield MySQL Master User Database Schema

-- Users table - Master user data for validation
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    status ENUM('ACTIVE', 'INACTIVE', 'BLOCKED', 'PENDING') DEFAULT 'ACTIVE',
    role ENUM('USER', 'MERCHANT', 'ADMIN') DEFAULT 'USER',
    max_transaction_limit DECIMAL(19,4) DEFAULT 10000.00,
    daily_limit DECIMAL(19,4) DEFAULT 50000.00,
    monthly_limit DECIMAL(19,4) DEFAULT 500000.00,
    risk_score DECIMAL(5,4) DEFAULT 0.0000,
    verified_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_status (status),
    INDEX idx_risk_score (risk_score)
);

-- Merchants table - Validated merchant destinations
CREATE TABLE IF NOT EXISTS merchants (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(100) NOT NULL,
    mcc_code VARCHAR(10),
    status ENUM('ACTIVE', 'INACTIVE', 'BLOCKED', 'UNDER_REVIEW') DEFAULT 'ACTIVE',
    risk_level ENUM('LOW', 'MEDIUM', 'HIGH') DEFAULT 'LOW',
    daily_volume_limit DECIMAL(19,4) DEFAULT 1000000.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_category (category)
);

-- Insert sample users for testing
INSERT INTO users (id, email, name, phone, status, role, max_transaction_limit, daily_limit) VALUES
('user-001', 'john.doe@example.com', 'John Doe', '+1234567890', 'ACTIVE', 'USER', 10000.00, 50000.00),
('user-002', 'jane.smith@example.com', 'Jane Smith', '+1234567891', 'ACTIVE', 'USER', 15000.00, 75000.00),
('user-003', 'blocked@example.com', 'Blocked User', '+1234567892', 'BLOCKED', 'USER', 0.00, 0.00),
('user-004', 'merchant@example.com', 'Merchant Admin', '+1234567893', 'ACTIVE', 'MERCHANT', 100000.00, 500000.00),
('user-005', 'inactive@example.com', 'Inactive User', '+1234567894', 'INACTIVE', 'USER', 10000.00, 50000.00);

-- Insert sample merchants
INSERT INTO merchants (id, name, category, mcc_code, status, risk_level) VALUES
('merch-001', 'Amazon', 'E-Commerce', '5411', 'ACTIVE', 'LOW'),
('merch-002', 'Netflix', 'Streaming', '4899', 'ACTIVE', 'LOW'),
('merch-003', 'Risky Casino', 'Gambling', '7995', 'ACTIVE', 'HIGH'),
('merch-004', 'Local Store', 'Retail', '5411', 'ACTIVE', 'LOW'),
('merch-005', 'Blocked Merchant', 'Unknown', '0000', 'BLOCKED', 'HIGH');
