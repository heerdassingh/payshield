import { NavLink, useLocation } from 'react-router-dom';
import {
  LayoutDashboard,
  ArrowLeftRight,
  ShieldAlert,
  BarChart3,
  Activity,
  Settings,
  Shield,
} from 'lucide-react';

const navItems = [
  { to: '/', icon: LayoutDashboard, label: 'Dashboard' },
  { to: '/transactions', icon: ArrowLeftRight, label: 'Transactions' },
  { to: '/alerts', icon: ShieldAlert, label: 'Fraud Alerts' },
  { to: '/analytics', icon: BarChart3, label: 'Analytics' },
];

export default function Sidebar() {
  return (
    <aside className="sidebar">
      <div className="sidebar-logo">
        <div className="logo-icon">
          <Shield size={20} />
        </div>
        <div>
          <div className="logo-text">PayShield</div>
        </div>
        <span className="logo-badge">AI</span>
      </div>

      <nav className="sidebar-nav">
        <div className="sidebar-section-label">Main</div>
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) =>
              `sidebar-nav-item ${isActive ? 'active' : ''}`
            }
            end={item.to === '/'}
          >
            <item.icon className="nav-icon" size={18} />
            <span>{item.label}</span>
          </NavLink>
        ))}

        <div className="sidebar-section-label" style={{ marginTop: 'auto' }}>
          System
        </div>
        <div className="sidebar-nav-item" style={{ cursor: 'default' }}>
          <Activity className="nav-icon" size={18} />
          <span>System Health</span>
          <span
            style={{
              marginLeft: 'auto',
              width: 8,
              height: 8,
              borderRadius: '50%',
              background: 'var(--status-safe)',
            }}
          />
        </div>
      </nav>
    </aside>
  );
}
