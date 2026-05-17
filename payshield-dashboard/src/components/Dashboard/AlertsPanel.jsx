import { Check, Clock } from 'lucide-react';
import { formatRelativeTime, getSeverityClass } from '../../utils/formatters';

export default function AlertsPanel({ alerts, onAcknowledge }) {
  if (!alerts || alerts.length === 0) {
    return (
      <div className="card">
        <div className="card-title">Active Alerts</div>
        <div className="loading-overlay" style={{ minHeight: 120 }}>
          <p style={{ fontSize: 13 }}>No active alerts — all clear ✓</p>
        </div>
      </div>
    );
  }

  return (
    <div className="card">
      <div className="card-title">Active Alerts ({alerts.length})</div>
      <div style={{ maxHeight: 400, overflowY: 'auto' }}>
        {alerts.slice(0, 10).map((alert) => {
          const sevClass = getSeverityClass(alert.severity);
          return (
            <div key={alert.alertId} className="alert-item">
              <div className={`alert-severity ${sevClass}`} />
              <div className="alert-content">
                <div className="alert-title">
                  <span className={`badge ${sevClass}`} style={{ marginRight: 8 }}>
                    {alert.severity}
                  </span>
                  {alert.alertType?.replace(/_/g, ' ')}
                </div>
                <div className="alert-meta">
                  <span>TXN: {alert.transactionId?.substring(0, 12)}...</span>
                  {alert.userId && <span> · User: {alert.userId}</span>}
                  {alert.fraudScore != null && (
                    <span> · Score: {(alert.fraudScore * 100).toFixed(0)}%</span>
                  )}
                  <span style={{ display: 'flex', alignItems: 'center', gap: 4, marginTop: 4 }}>
                    <Clock size={11} />
                    {formatRelativeTime(alert.createdAt)}
                  </span>
                </div>
              </div>
              <div className="alert-actions">
                {onAcknowledge && (
                  <button
                    className="btn btn-ghost btn-sm"
                    onClick={() => onAcknowledge(alert.alertId)}
                    title="Acknowledge alert"
                  >
                    <Check size={14} />
                    ACK
                  </button>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
