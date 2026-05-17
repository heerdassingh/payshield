import { formatCurrency, formatDateTime, getScoreClass, formatScore } from '../../utils/formatters';

function ScoreBar({ score }) {
  if (score == null) return <span style={{ color: 'var(--text-muted)' }}>—</span>;
  const cls = getScoreClass(score);
  const pct = Math.round(score * 100);

  return (
    <div className="score-bar">
      <span className={`score-text`} style={{ color: `var(--status-${cls === 'safe' ? 'safe' : cls === 'warning' ? 'warning' : cls === 'danger' ? 'danger' : 'critical'})` }}>
        {pct}%
      </span>
      <div className="bar">
        <div
          className={`bar-fill ${cls}`}
          style={{ width: `${pct}%` }}
        />
      </div>
    </div>
  );
}

function StatusBadge({ status }) {
  const cls =
    status === 'COMPLETED' ? 'safe' :
    status === 'REJECTED' ? 'danger' :
    'warning';

  return <span className={`badge ${cls}`}>{status}</span>;
}

export default function TransactionTable({ transactions }) {
  if (!transactions || transactions.length === 0) {
    return (
      <div className="card">
        <div className="card-title">Recent Transactions</div>
        <div className="loading-overlay">
          <p>No transactions yet. Submit a transaction to see data here.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="card" style={{ overflow: 'auto' }}>
      <div className="card-title">Recent Transactions</div>
      <table className="data-table">
        <thead>
          <tr>
            <th>Transaction ID</th>
            <th>User</th>
            <th>Merchant</th>
            <th>Amount</th>
            <th>Status</th>
            <th>Fraud Score</th>
            <th>Time</th>
          </tr>
        </thead>
        <tbody>
          {transactions.slice(0, 20).map((txn) => (
            <tr key={txn.transactionId || txn.id}>
              <td style={{ fontFamily: 'var(--font-mono)', fontSize: 12 }}>
                {txn.transactionId?.substring(0, 8)}...
              </td>
              <td>{txn.userId}</td>
              <td>{txn.merchantId}</td>
              <td className="td-amount">{formatCurrency(txn.amount)}</td>
              <td><StatusBadge status={txn.status} /></td>
              <td><ScoreBar score={txn.fraudScore} /></td>
              <td style={{ color: 'var(--text-muted)', fontSize: 12 }}>
                {formatDateTime(txn.processedAt)}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
