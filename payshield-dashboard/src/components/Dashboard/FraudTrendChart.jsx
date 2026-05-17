import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from 'recharts';

const CustomTooltip = ({ active, payload, label }) => {
  if (!active || !payload?.length) return null;

  return (
    <div
      style={{
        background: 'rgba(17, 24, 39, 0.95)',
        border: '1px solid rgba(255,255,255,0.1)',
        borderRadius: 12,
        padding: '12px 16px',
        backdropFilter: 'blur(8px)',
      }}
    >
      <p style={{ color: '#94a3b8', fontSize: 12, marginBottom: 8 }}>{label}</p>
      {payload.map((entry, i) => (
        <p key={i} style={{ color: entry.color, fontSize: 13, fontWeight: 600 }}>
          {entry.name}: {entry.value}
        </p>
      ))}
    </div>
  );
};

export default function FraudTrendChart({ data }) {
  if (!data || data.length === 0) {
    return (
      <div className="card">
        <div className="card-title">Fraud Trends — Last 24 Hours</div>
        <div className="loading-overlay">
          <p>No trend data available yet</p>
        </div>
      </div>
    );
  }

  return (
    <div className="card">
      <div className="card-title">Fraud Trends — Last 24 Hours</div>
      <div className="chart-container">
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart data={data} margin={{ top: 10, right: 10, left: -10, bottom: 0 }}>
            <defs>
              <linearGradient id="gradTransactions" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#6366f1" stopOpacity={0.3} />
                <stop offset="95%" stopColor="#6366f1" stopOpacity={0} />
              </linearGradient>
              <linearGradient id="gradFraud" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#ef4444" stopOpacity={0.4} />
                <stop offset="95%" stopColor="#ef4444" stopOpacity={0} />
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.04)" />
            <XAxis
              dataKey="hour"
              stroke="#64748b"
              fontSize={11}
              tickLine={false}
              axisLine={false}
            />
            <YAxis
              stroke="#64748b"
              fontSize={11}
              tickLine={false}
              axisLine={false}
            />
            <Tooltip content={<CustomTooltip />} />
            <Legend
              wrapperStyle={{ fontSize: 12, paddingTop: 8 }}
            />
            <Area
              type="monotone"
              dataKey="totalTransactions"
              name="Transactions"
              stroke="#6366f1"
              strokeWidth={2}
              fill="url(#gradTransactions)"
            />
            <Area
              type="monotone"
              dataKey="fraudulent"
              name="Fraudulent"
              stroke="#ef4444"
              strokeWidth={2}
              fill="url(#gradFraud)"
            />
          </AreaChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
