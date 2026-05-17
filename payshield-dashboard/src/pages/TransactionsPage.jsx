import { useState, useCallback } from 'react';
import { usePolling } from '../hooks/usePolling';
import { fetchRecentTransactions } from '../services/api';
import { formatCurrency, formatDateTime, getScoreClass } from '../utils/formatters';
import { Search, Filter } from 'lucide-react';

export default function TransactionsPage() {
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');

  const { data: transactions, loading } = usePolling(
    useCallback(() => fetchRecentTransactions(100), []),
    5000
  );

  const filtered = (transactions || []).filter((txn) => {
    const matchSearch =
      !search ||
      txn.transactionId?.toLowerCase().includes(search.toLowerCase()) ||
      txn.userId?.toLowerCase().includes(search.toLowerCase()) ||
      txn.merchantId?.toLowerCase().includes(search.toLowerCase());

    const matchStatus = statusFilter === 'ALL' || txn.status === statusFilter;

    return matchSearch && matchStatus;
  });

  return (
    <div className="page-container">
      {/* Filters */}
      <div className="card" style={{ marginBottom: 24, display: 'flex', gap: 16, alignItems: 'center', flexWrap: 'wrap' }}>
        <div style={{ position: 'relative', flex: 1, minWidth: 200 }}>
          <Search
            size={16}
            style={{
              position: 'absolute',
              left: 12,
              top: '50%',
              transform: 'translateY(-50%)',
              color: 'var(--text-muted)',
            }}
          />
          <input
            type="text"
            placeholder="Search by Transaction ID, User ID, or Merchant..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            style={{
              width: '100%',
              padding: '10px 12px 10px 36px',
              background: 'var(--bg-glass)',
              border: '1px solid var(--bg-glass-border)',
              borderRadius: 8,
              color: 'var(--text-primary)',
              fontSize: 13,
              fontFamily: 'var(--font-family)',
              outline: 'none',
            }}
          />
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          {['ALL', 'COMPLETED', 'REJECTED', 'PENDING'].map((status) => (
            <button
              key={status}
              className={`btn btn-sm ${statusFilter === status ? 'btn-primary' : 'btn-ghost'}`}
              onClick={() => setStatusFilter(status)}
            >
              {status}
            </button>
          ))}
        </div>
      </div>

      {/* Table */}
      <div className="card" style={{ overflow: 'auto' }}>
        <div className="card-title">
          Transactions ({filtered.length})
        </div>
        {loading && !transactions ? (
          <div className="loading-overlay">
            <div className="spinner" />
            <p>Loading transactions...</p>
          </div>
        ) : filtered.length === 0 ? (
          <div className="loading-overlay">
            <p>No transactions match your filters</p>
          </div>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>Transaction ID</th>
                <th>User ID</th>
                <th>Merchant</th>
                <th>Amount</th>
                <th>Currency</th>
                <th>Type</th>
                <th>Status</th>
                <th>Fraud Score</th>
                <th>Reason</th>
                <th>Processed At</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((txn) => {
                const scoreClass = getScoreClass(txn.fraudScore);
                return (
                  <tr key={txn.transactionId || txn.id}>
                    <td style={{ fontFamily: 'var(--font-mono)', fontSize: 11 }}>
                      {txn.transactionId?.substring(0, 12)}...
                    </td>
                    <td>{txn.userId}</td>
                    <td>{txn.merchantId}</td>
                    <td className="td-amount">{formatCurrency(txn.amount)}</td>
                    <td>{txn.currency || 'USD'}</td>
                    <td>
                      <span className="badge info">{txn.type || 'PAYMENT'}</span>
                    </td>
                    <td>
                      <span
                        className={`badge ${
                          txn.status === 'COMPLETED' ? 'safe' :
                          txn.status === 'REJECTED' ? 'danger' : 'warning'
                        }`}
                      >
                        {txn.status}
                      </span>
                    </td>
                    <td>
                      {txn.fraudScore != null ? (
                        <div className="score-bar">
                          <span
                            className="score-text"
                            style={{
                              color: `var(--status-${scoreClass === 'safe' ? 'safe' : scoreClass === 'warning' ? 'warning' : scoreClass === 'danger' ? 'danger' : 'critical'})`,
                            }}
                          >
                            {(txn.fraudScore * 100).toFixed(0)}%
                          </span>
                          <div className="bar">
                            <div
                              className={`bar-fill ${scoreClass}`}
                              style={{ width: `${txn.fraudScore * 100}%` }}
                            />
                          </div>
                        </div>
                      ) : (
                        '—'
                      )}
                    </td>
                    <td
                      style={{
                        maxWidth: 200,
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                        fontSize: 11,
                      }}
                      title={txn.fraudReason}
                    >
                      {txn.fraudReason || '—'}
                    </td>
                    <td style={{ fontSize: 12, color: 'var(--text-muted)' }}>
                      {formatDateTime(txn.processedAt)}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
