import { usePolling } from '../hooks/usePolling';
import { fetchActiveAlerts, acknowledgeAlert } from '../services/api';
import { formatRelativeTime, getSeverityClass } from '../utils/formatters';
import { Check, Clock, ShieldAlert, AlertTriangle, ShieldOff, Eye } from 'lucide-react';

const alertTypeIcons = {
  FRAUD_DETECTED: ShieldAlert,
  RETRY_EXHAUSTED: AlertTriangle,
  USER_NOT_FOUND: ShieldOff,
  HIGH_RISK_TRANSACTION: Eye,
  ANOMALY_DETECTED: AlertTriangle,
};

export default function AlertsPage() {
  const { data: alerts, refetch } = usePolling(fetchActiveAlerts, 5000);

  const handleAcknowledge = async (alertId) => {
    try {
      await acknowledgeAlert(alertId);
      refetch();
    } catch (err) {
      console.error('Failed to acknowledge:', err);
    }
  };

  return (
    <div className="page-container">
      {/* Stats row */}
      <div className="stats-grid" style={{ gridTemplateColumns: 'repeat(4, 1fr)', marginBottom: 24 }}>
        {['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'].map((severity) => {
          const count = (alerts || []).filter((a) => a.severity === severity).length;
          const cls = getSeverityClass(severity);
          return (
            <div key={severity} className={`stat-card accent-${cls === 'info' ? 'info' : cls === 'warning' ? 'warning' : cls === 'danger' ? 'danger' : 'critical'}`}>
              <div className="stat-label">{severity} Alerts</div>
              <div className="stat-value" style={{ color: `var(--status-${cls === 'info' ? 'info' : cls === 'danger' ? 'danger' : cls === 'warning' ? 'warning' : 'critical'})` }}>
                {count}
              </div>
            </div>
          );
        })}
      </div>

      {/* Alerts list */}
      <div className="card">
        <div className="card-title">All Active Alerts ({(alerts || []).length})</div>

        {!alerts || alerts.length === 0 ? (
          <div className="loading-overlay" style={{ minHeight: 200 }}>
            <ShieldAlert size={48} color="var(--status-safe)" />
            <p style={{ fontSize: 15, fontWeight: 600, color: 'var(--text-primary)' }}>
              All Clear
            </p>
            <p>No unacknowledged alerts at this time.</p>
          </div>
        ) : (
          <div>
            {alerts.map((alert) => {
              const sevClass = getSeverityClass(alert.severity);
              const Icon = alertTypeIcons[alert.alertType] || AlertTriangle;
              const details = alert.details || {};

              return (
                <div key={alert.alertId} className="alert-item" style={{ marginBottom: 12 }}>
                  <div className={`alert-severity ${sevClass}`} />

                  <div
                    style={{
                      width: 40,
                      height: 40,
                      borderRadius: 10,
                      background: `var(--status-${sevClass}-bg, rgba(255,255,255,0.05))`,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      flexShrink: 0,
                    }}
                  >
                    <Icon size={18} color={`var(--status-${sevClass === 'info' ? 'info' : sevClass})`} />
                  </div>

                  <div className="alert-content" style={{ flex: 1 }}>
                    <div className="alert-title" style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      <span className={`badge ${sevClass}`}>{alert.severity}</span>
                      {alert.alertType?.replace(/_/g, ' ')}
                    </div>
                    <div className="alert-meta" style={{ marginTop: 4 }}>
                      <div>Transaction: <span style={{ fontFamily: 'var(--font-mono)' }}>{alert.transactionId?.substring(0, 16)}...</span></div>
                      {alert.userId && <div>User: {alert.userId}</div>}
                      {alert.fraudScore != null && (
                        <div>Fraud Score: <strong>{(alert.fraudScore * 100).toFixed(1)}%</strong></div>
                      )}
                      {details.amount && <div>Amount: ${details.amount}</div>}
                      {details.reason && (
                        <div style={{ marginTop: 4, color: 'var(--text-secondary)', fontSize: 12 }}>
                          Reason: {details.reason}
                        </div>
                      )}
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 4, marginTop: 6, fontSize: 11, color: 'var(--text-muted)' }}>
                      <Clock size={11} />
                      {formatRelativeTime(alert.createdAt)}
                    </div>
                  </div>

                  <div className="alert-actions">
                    <button
                      className="btn btn-ghost btn-sm"
                      onClick={() => handleAcknowledge(alert.alertId)}
                    >
                      <Check size={14} />
                      Acknowledge
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
