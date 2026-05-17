import { useCallback } from 'react';
import { usePolling } from '../hooks/usePolling';
import { fetchFraudTrends, fetchRecentTransactions, fetchDashboardStats } from '../services/api';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  LineChart,
  Line,
  Legend,
} from 'recharts';

const COLORS = ['#10b981', '#f59e0b', '#ef4444', '#a855f7'];

const CustomTooltip = ({ active, payload, label }) => {
  if (!active || !payload?.length) return null;
  return (
    <div
      style={{
        background: 'rgba(17, 24, 39, 0.95)',
        border: '1px solid rgba(255,255,255,0.1)',
        borderRadius: 12,
        padding: '12px 16px',
      }}
    >
      <p style={{ color: '#94a3b8', fontSize: 12, marginBottom: 6 }}>{label}</p>
      {payload.map((entry, i) => (
        <p key={i} style={{ color: entry.color, fontSize: 13, fontWeight: 600 }}>
          {entry.name}: {entry.value}
        </p>
      ))}
    </div>
  );
};

export default function AnalyticsPage() {
  const { data: trends } = usePolling(fetchFraudTrends, 15000);
  const { data: transactions } = usePolling(
    useCallback(() => fetchRecentTransactions(100), []),
    10000
  );
  const { data: stats } = usePolling(fetchDashboardStats, 10000);

  // Build status distribution for pie chart
  const statusDist = (transactions || []).reduce((acc, txn) => {
    const status = txn.status || 'UNKNOWN';
    acc[status] = (acc[status] || 0) + 1;
    return acc;
  }, {});

  const pieData = Object.entries(statusDist).map(([name, value]) => ({
    name,
    value,
  }));

  // Build score distribution for bar chart
  const scoreRanges = [
    { range: '0-10%', min: 0, max: 0.1 },
    { range: '10-20%', min: 0.1, max: 0.2 },
    { range: '20-30%', min: 0.2, max: 0.3 },
    { range: '30-40%', min: 0.3, max: 0.4 },
    { range: '40-50%', min: 0.4, max: 0.5 },
    { range: '50-60%', min: 0.5, max: 0.6 },
    { range: '60-70%', min: 0.6, max: 0.7 },
    { range: '70-80%', min: 0.7, max: 0.8 },
    { range: '80-90%', min: 0.8, max: 0.9 },
    { range: '90-100%', min: 0.9, max: 1.01 },
  ];

  const scoreDist = scoreRanges.map(({ range, min, max }) => ({
    range,
    count: (transactions || []).filter(
      (t) => t.fraudScore != null && t.fraudScore >= min && t.fraudScore < max
    ).length,
  }));

  return (
    <div className="page-container">
      {/* Top row: Avg Score Line Chart */}
      <div className="card" style={{ marginBottom: 24 }}>
        <div className="card-title">Average Fraud Score Over Time</div>
        <div className="chart-container">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={trends || []} margin={{ top: 10, right: 10, left: -10, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.04)" />
              <XAxis dataKey="hour" stroke="#64748b" fontSize={11} tickLine={false} />
              <YAxis stroke="#64748b" fontSize={11} tickLine={false} domain={[0, 1]} />
              <Tooltip content={<CustomTooltip />} />
              <Legend wrapperStyle={{ fontSize: 12 }} />
              <Line
                type="monotone"
                dataKey="avgFraudScore"
                name="Avg Fraud Score"
                stroke="#f59e0b"
                strokeWidth={2}
                dot={{ fill: '#f59e0b', r: 3 }}
                activeDot={{ r: 5 }}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Bottom row */}
      <div className="dashboard-grid equal">
        {/* Score Distribution */}
        <div className="card">
          <div className="card-title">Fraud Score Distribution</div>
          <div className="chart-container">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={scoreDist} margin={{ top: 10, right: 10, left: -10, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.04)" />
                <XAxis dataKey="range" stroke="#64748b" fontSize={10} tickLine={false} angle={-45} textAnchor="end" height={60} />
                <YAxis stroke="#64748b" fontSize={11} tickLine={false} />
                <Tooltip content={<CustomTooltip />} />
                <Bar dataKey="count" name="Transactions" fill="#6366f1" radius={[4, 4, 0, 0]}>
                  {scoreDist.map((entry, i) => (
                    <Cell
                      key={i}
                      fill={
                        i >= 7 ? '#ef4444' :
                        i >= 5 ? '#f59e0b' :
                        '#6366f1'
                      }
                    />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Status Distribution */}
        <div className="card">
          <div className="card-title">Transaction Status Distribution</div>
          <div className="chart-container" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            {pieData.length === 0 ? (
              <p style={{ color: 'var(--text-muted)' }}>No data yet</p>
            ) : (
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={pieData}
                    cx="50%"
                    cy="50%"
                    innerRadius={70}
                    outerRadius={110}
                    paddingAngle={3}
                    dataKey="value"
                    label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                    labelLine={false}
                  >
                    {pieData.map((entry, i) => (
                      <Cell
                        key={i}
                        fill={
                          entry.name === 'COMPLETED' ? '#10b981' :
                          entry.name === 'REJECTED' ? '#ef4444' :
                          entry.name === 'PENDING' ? '#f59e0b' :
                          COLORS[i % COLORS.length]
                        }
                      />
                    ))}
                  </Pie>
                  <Tooltip content={<CustomTooltip />} />
                </PieChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>
      </div>

      {/* Summary Stats */}
      <div className="card" style={{ marginTop: 24 }}>
        <div className="card-title">Quick Summary</div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 16 }}>
          {[
            { label: 'Total Transactions', value: stats?.totalTransactions?.toLocaleString() || '0' },
            { label: 'Fraud Detected', value: stats?.fraudCount?.toLocaleString() || '0' },
            { label: 'Fraud Rate', value: stats?.fraudRate != null ? `${stats.fraudRate}%` : '0%' },
            { label: 'Avg Fraud Score', value: stats?.avgFraudScore != null ? `${(stats.avgFraudScore * 100).toFixed(1)}%` : '0%' },
            { label: 'Active Alerts', value: stats?.activeAlerts?.toLocaleString() || '0' },
            { label: 'Critical Alerts', value: stats?.criticalAlerts?.toLocaleString() || '0' },
          ].map((item) => (
            <div
              key={item.label}
              style={{
                padding: 16,
                borderRadius: 10,
                background: 'var(--bg-glass)',
                border: '1px solid var(--bg-glass-border)',
              }}
            >
              <div style={{ fontSize: 12, color: 'var(--text-muted)', marginBottom: 4 }}>{item.label}</div>
              <div style={{ fontSize: 22, fontWeight: 700 }}>{item.value}</div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
