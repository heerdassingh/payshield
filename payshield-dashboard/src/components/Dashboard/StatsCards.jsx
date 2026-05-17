import {
  ArrowLeftRight,
  ShieldAlert,
  TrendingUp,
  AlertTriangle,
  Target,
  Zap,
} from 'lucide-react';
import { formatNumber } from '../../utils/formatters';

const cardConfigs = [
  {
    key: 'totalTransactions',
    label: 'Total Transactions',
    icon: ArrowLeftRight,
    accent: 'accent-primary',
    iconBg: 'rgba(99, 102, 241, 0.15)',
    iconColor: '#818cf8',
  },
  {
    key: 'activeAlerts',
    label: 'Active Alerts',
    icon: ShieldAlert,
    accent: 'accent-danger',
    iconBg: 'rgba(239, 68, 68, 0.15)',
    iconColor: '#ef4444',
  },
  {
    key: 'fraudRate',
    label: 'Fraud Rate',
    icon: Target,
    accent: 'accent-warning',
    iconBg: 'rgba(245, 158, 11, 0.15)',
    iconColor: '#f59e0b',
    suffix: '%',
  },
  {
    key: 'criticalAlerts',
    label: 'Critical Alerts',
    icon: AlertTriangle,
    accent: 'accent-critical',
    iconBg: 'rgba(168, 85, 247, 0.15)',
    iconColor: '#a855f7',
  },
  {
    key: 'fraudCount',
    label: 'Fraud Detected',
    icon: Zap,
    accent: 'accent-danger',
    iconBg: 'rgba(239, 68, 68, 0.15)',
    iconColor: '#ef4444',
  },
  {
    key: 'avgFraudScore',
    label: 'Avg Fraud Score',
    icon: TrendingUp,
    accent: 'accent-info',
    iconBg: 'rgba(59, 130, 246, 0.15)',
    iconColor: '#3b82f6',
    format: (v) => v != null ? (v * 100).toFixed(1) + '%' : '—',
  },
];

export default function StatsCards({ stats }) {
  if (!stats) {
    return (
      <div className="stats-grid">
        {cardConfigs.map((cfg) => (
          <div key={cfg.key} className={`stat-card ${cfg.accent}`}>
            <div className="stat-header">
              <span className="stat-label">{cfg.label}</span>
              <div
                className="stat-icon"
                style={{ background: cfg.iconBg }}
              >
                <cfg.icon size={20} color={cfg.iconColor} />
              </div>
            </div>
            <div className="stat-value" style={{ color: 'var(--text-muted)' }}>
              —
            </div>
          </div>
        ))}
      </div>
    );
  }

  return (
    <div className="stats-grid">
      {cardConfigs.map((cfg) => {
        const value = stats[cfg.key];
        const display = cfg.format
          ? cfg.format(value)
          : `${formatNumber(value)}${cfg.suffix || ''}`;

        return (
          <div key={cfg.key} className={`stat-card ${cfg.accent}`}>
            <div className="stat-header">
              <span className="stat-label">{cfg.label}</span>
              <div
                className="stat-icon"
                style={{ background: cfg.iconBg }}
              >
                <cfg.icon size={20} color={cfg.iconColor} />
              </div>
            </div>
            <div className="stat-value">{display}</div>
            <div className="stat-change">
              {cfg.key === 'fraudRate' && 'detection accuracy'}
              {cfg.key === 'totalTransactions' && 'processed total'}
              {cfg.key === 'activeAlerts' && 'pending review'}
              {cfg.key === 'criticalAlerts' && 'require action'}
              {cfg.key === 'fraudCount' && 'transactions blocked'}
              {cfg.key === 'avgFraudScore' && 'across all transactions'}
            </div>
          </div>
        );
      })}
    </div>
  );
}
