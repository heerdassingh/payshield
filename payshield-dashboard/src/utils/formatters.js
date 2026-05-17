/**
 * Format a number with commas
 */
export function formatNumber(num) {
  if (num == null) return '—';
  return num.toLocaleString('en-US');
}

/**
 * Format currency
 */
export function formatCurrency(amount, currency = 'USD') {
  if (amount == null) return '—';
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency,
    minimumFractionDigits: 2,
  }).format(amount);
}

/**
 * Format a fraud score (0-1) as percentage
 */
export function formatScore(score) {
  if (score == null) return '—';
  return (score * 100).toFixed(1) + '%';
}

/**
 * Format an ISO timestamp to a short datetime
 */
export function formatDateTime(isoString) {
  if (!isoString) return '—';
  const date = new Date(isoString);
  return date.toLocaleString('en-US', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

/**
 * Format relative time (e.g., "2 min ago")
 */
export function formatRelativeTime(isoString) {
  if (!isoString) return '—';
  const diff = Date.now() - new Date(isoString).getTime();
  const minutes = Math.floor(diff / 60000);
  if (minutes < 1) return 'just now';
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  return `${days}d ago`;
}

/**
 * Get score classification
 */
export function getScoreClass(score) {
  if (score == null) return 'safe';
  if (score >= 0.9) return 'critical';
  if (score >= 0.7) return 'danger';
  if (score >= 0.5) return 'warning';
  return 'safe';
}

/**
 * Get severity badge class
 */
export function getSeverityClass(severity) {
  switch (severity?.toUpperCase()) {
    case 'CRITICAL': return 'critical';
    case 'HIGH': return 'danger';
    case 'MEDIUM': return 'warning';
    case 'LOW': return 'info';
    default: return 'info';
  }
}
