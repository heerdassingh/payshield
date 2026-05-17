import { Brain, Cpu, Database, CheckCircle, XCircle, RefreshCw } from 'lucide-react';

export default function AIStatusCard({ aiStatus }) {
  const isOnline = aiStatus?.serviceReachable === true;
  const isTrained = aiStatus?.is_trained === true;

  return (
    <div className="card ai-status-card">
      <div className="card-title">AI Model Status</div>

      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
        <div
          style={{
            width: 48,
            height: 48,
            borderRadius: 12,
            background: isOnline
              ? 'rgba(16, 185, 129, 0.15)'
              : 'rgba(239, 68, 68, 0.15)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          <Brain size={24} color={isOnline ? '#10b981' : '#ef4444'} />
        </div>
        <div>
          <div style={{ fontWeight: 700, fontSize: 16 }}>
            {aiStatus?.model_name || 'Isolation Forest'}
          </div>
          <div
            className={`ai-status-indicator ${isOnline ? 'online' : 'offline'}`}
            style={{ marginTop: 4 }}
          >
            {isOnline ? (
              <>
                <CheckCircle size={14} /> Online
              </>
            ) : (
              <>
                <XCircle size={14} /> Offline
              </>
            )}
          </div>
        </div>
      </div>

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: '1fr 1fr',
          gap: 12,
          fontSize: 13,
        }}
      >
        <div
          style={{
            padding: 12,
            borderRadius: 8,
            background: 'var(--bg-glass)',
            border: '1px solid var(--bg-glass-border)',
          }}
        >
          <div style={{ color: 'var(--text-muted)', fontSize: 11, marginBottom: 4 }}>
            Model Version
          </div>
          <div style={{ fontWeight: 600 }}>
            v{aiStatus?.model_version || '1.0.0'}
          </div>
        </div>

        <div
          style={{
            padding: 12,
            borderRadius: 8,
            background: 'var(--bg-glass)',
            border: '1px solid var(--bg-glass-border)',
          }}
        >
          <div style={{ color: 'var(--text-muted)', fontSize: 11, marginBottom: 4 }}>
            Training Data
          </div>
          <div style={{ fontWeight: 600 }}>
            {aiStatus?.training_samples?.toLocaleString() || '—'} samples
          </div>
        </div>

        <div
          style={{
            padding: 12,
            borderRadius: 8,
            background: 'var(--bg-glass)',
            border: '1px solid var(--bg-glass-border)',
          }}
        >
          <div style={{ color: 'var(--text-muted)', fontSize: 11, marginBottom: 4 }}>
            Fraud Threshold
          </div>
          <div style={{ fontWeight: 600 }}>
            {aiStatus?.fraud_threshold != null
              ? `${(aiStatus.fraud_threshold * 100).toFixed(0)}%`
              : '70%'}
          </div>
        </div>

        <div
          style={{
            padding: 12,
            borderRadius: 8,
            background: 'var(--bg-glass)',
            border: '1px solid var(--bg-glass-border)',
          }}
        >
          <div style={{ color: 'var(--text-muted)', fontSize: 11, marginBottom: 4 }}>
            RAG Documents
          </div>
          <div style={{ fontWeight: 600 }}>
            {aiStatus?.rag_documents_loaded ?? '—'} loaded
          </div>
        </div>
      </div>
    </div>
  );
}
