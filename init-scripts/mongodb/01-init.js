// PayShield MongoDB Collections for Transaction Processing

// Switch to payshield database
db = db.getSiblingDB('payshield');

// Authenticate as admin
db.auth('payshield', 'payshield_secret');

// ============================================
// USER DATA RECORD - Successful Transactions
// ============================================
db.createCollection('user_data_record', {
    validator: {
        $jsonSchema: {
            bsonType: 'object',
            required: ['transactionId', 'userId', 'amount', 'status', 'processedAt'],
            properties: {
                transactionId: {
                    bsonType: 'string',
                    description: 'Unique transaction identifier (Kafka key)'
                },
                userId: {
                    bsonType: 'string',
                    description: 'User ID from master data'
                },
                merchantId: {
                    bsonType: 'string',
                    description: 'Merchant ID'
                },
                amount: {
                    bsonType: 'double',
                    description: 'Transaction amount'
                },
                currency: {
                    bsonType: 'string',
                    description: 'Currency code'
                },
                type: {
                    enum: ['PAYMENT', 'TRANSFER', 'WITHDRAWAL', 'DEPOSIT'],
                    description: 'Transaction type'
                },
                status: {
                    enum: ['COMPLETED', 'PENDING', 'REJECTED'],
                    description: 'Final transaction status'
                },
                fraudScore: {
                    bsonType: 'double',
                    description: 'AI-computed fraud score (0-1)'
                },
                fraudReason: {
                    bsonType: 'string',
                    description: 'Reason if flagged'
                },
                processedAt: {
                    bsonType: 'date',
                    description: 'Processing timestamp'
                },
                metadata: {
                    bsonType: 'object',
                    description: 'Additional transaction metadata'
                }
            }
        }
    }
});

// Indexes for user_data_record
db.user_data_record.createIndex({ 'transactionId': 1 }, { unique: true });
db.user_data_record.createIndex({ 'userId': 1 });
db.user_data_record.createIndex({ 'status': 1 });
db.user_data_record.createIndex({ 'processedAt': -1 });
db.user_data_record.createIndex({ 'userId': 1, 'processedAt': -1 });

// ============================================
// FAILED TRANSACTIONS - For Retry Scheduler
// ============================================
db.createCollection('failed_transactions', {
    validator: {
        $jsonSchema: {
            bsonType: 'object',
            required: ['transactionId', 'originalPayload', 'failureReason', 'retryCount', 'createdAt'],
            properties: {
                transactionId: {
                    bsonType: 'string',
                    description: 'Unique transaction identifier'
                },
                originalPayload: {
                    bsonType: 'object',
                    description: 'Original transaction request payload'
                },
                failureReason: {
                    enum: ['DOWNSTREAM_ERROR', 'AI_UNCERTAINTY', 'TIMEOUT', 'VALIDATION_ERROR', 'SYSTEM_ERROR'],
                    description: 'Reason for failure'
                },
                errorDetails: {
                    bsonType: 'string',
                    description: 'Detailed error message'
                },
                retryCount: {
                    bsonType: 'int',
                    description: 'Number of retry attempts'
                },
                maxRetries: {
                    bsonType: 'int',
                    description: 'Maximum allowed retries'
                },
                nextRetryAt: {
                    bsonType: 'date',
                    description: 'Scheduled time for next retry'
                },
                lastRetryAt: {
                    bsonType: 'date',
                    description: 'Last retry attempt timestamp'
                },
                status: {
                    enum: ['PENDING_RETRY', 'RETRYING', 'EXHAUSTED', 'RESOLVED'],
                    description: 'Current retry status'
                },
                createdAt: {
                    bsonType: 'date',
                    description: 'Initial failure timestamp'
                }
            }
        }
    }
});

// Indexes for failed_transactions
db.failed_transactions.createIndex({ 'transactionId': 1 }, { unique: true });
db.failed_transactions.createIndex({ 'status': 1, 'nextRetryAt': 1 });
db.failed_transactions.createIndex({ 'retryCount': 1 });
db.failed_transactions.createIndex({ 'createdAt': -1 });

// ============================================
// FRAUD ALERTS - Alert History
// ============================================
db.createCollection('fraud_alerts', {
    validator: {
        $jsonSchema: {
            bsonType: 'object',
            required: ['alertId', 'transactionId', 'alertType', 'severity', 'createdAt'],
            properties: {
                alertId: {
                    bsonType: 'string',
                    description: 'Unique alert identifier'
                },
                transactionId: {
                    bsonType: 'string',
                    description: 'Related transaction ID'
                },
                userId: {
                    bsonType: 'string',
                    description: 'User ID'
                },
                alertType: {
                    enum: ['FRAUD_DETECTED', 'RETRY_EXHAUSTED', 'USER_NOT_FOUND', 'HIGH_RISK_TRANSACTION', 'ANOMALY_DETECTED'],
                    description: 'Type of alert'
                },
                severity: {
                    enum: ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'],
                    description: 'Alert severity level'
                },
                fraudScore: {
                    bsonType: 'double',
                    description: 'Fraud score if applicable'
                },
                details: {
                    bsonType: 'object',
                    description: 'Alert details and context'
                },
                acknowledged: {
                    bsonType: 'bool',
                    description: 'Whether alert was acknowledged'
                },
                acknowledgedBy: {
                    bsonType: 'string',
                    description: 'Who acknowledged the alert'
                },
                acknowledgedAt: {
                    bsonType: 'date',
                    description: 'Acknowledgement timestamp'
                },
                createdAt: {
                    bsonType: 'date',
                    description: 'Alert creation timestamp'
                }
            }
        }
    }
});

// Indexes for fraud_alerts
db.fraud_alerts.createIndex({ 'alertId': 1 }, { unique: true });
db.fraud_alerts.createIndex({ 'transactionId': 1 });
db.fraud_alerts.createIndex({ 'severity': 1, 'acknowledged': 1 });
db.fraud_alerts.createIndex({ 'createdAt': -1 });

print('PayShield MongoDB collections initialized successfully');
