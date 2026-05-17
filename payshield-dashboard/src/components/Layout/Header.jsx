import { useLocation } from 'react-router-dom';
import { Bell, RefreshCw } from 'lucide-react';

const pageTitles = {
  '/': 'Dashboard',
  '/transactions': 'Transactions',
  '/alerts': 'Fraud Alerts',
  '/analytics': 'Analytics',
};

export default function Header({ onRefresh }) {
  const location = useLocation();
  const title = pageTitles[location.pathname] || 'PayShield';

  return (
    <header className="header">
      <h1 className="header-title">{title}</h1>
      <div className="header-right">
        <div className="header-status">
          <span className="status-dot" />
          <span>Live Monitoring</span>
        </div>
        {onRefresh && (
          <button className="btn btn-ghost btn-sm" onClick={onRefresh}>
            <RefreshCw size={14} />
            Refresh
          </button>
        )}
      </div>
    </header>
  );
}
