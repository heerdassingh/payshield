import { useCallback } from 'react';
import { usePolling } from '../hooks/usePolling';
import {
  fetchDashboardStats,
  fetchRecentTransactions,
  fetchFraudTrends,
  fetchActiveAlerts,
  fetchAIStatus,
  acknowledgeAlert,
} from '../services/api';
import StatsCards from '../components/Dashboard/StatsCards';
import FraudTrendChart from '../components/Dashboard/FraudTrendChart';
import TransactionTable from '../components/Dashboard/TransactionTable';
import AlertsPanel from '../components/Dashboard/AlertsPanel';
import AIStatusCard from '../components/Dashboard/AIStatusCard';

export default function DashboardPage() {
  const { data: stats, refetch: refetchStats } = usePolling(fetchDashboardStats, 5000);
  const { data: transactions } = usePolling(
    useCallback(() => fetchRecentTransactions(20), []),
    5000
  );
  const { data: trends } = usePolling(fetchFraudTrends, 15000);
  const { data: alerts, refetch: refetchAlerts } = usePolling(fetchActiveAlerts, 5000);
  const { data: aiStatus } = usePolling(fetchAIStatus, 10000);

  const handleAcknowledge = async (alertId) => {
    try {
      await acknowledgeAlert(alertId);
      refetchAlerts();
      refetchStats();
    } catch (err) {
      console.error('Failed to acknowledge alert:', err);
    }
  };

  return (
    <div className="page-container">
      <StatsCards stats={stats} />

      <div className="dashboard-grid">
        <FraudTrendChart data={trends} />
        <AlertsPanel alerts={alerts} onAcknowledge={handleAcknowledge} />
      </div>

      <TransactionTable transactions={transactions} />

      <div style={{ marginTop: 24 }}>
        <AIStatusCard aiStatus={aiStatus} />
      </div>
    </div>
  );
}
