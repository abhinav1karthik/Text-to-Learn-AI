const STATUS_STYLES = {
  READY: 'bg-green-500',
  GENERATED: 'bg-green-500',
  SUCCEEDED: 'bg-green-500',
  GENERATING: 'animate-pulse bg-blue-500',
  QUEUED: 'animate-pulse bg-blue-500',
  RUNNING: 'animate-pulse bg-blue-500',
  PLAN: 'bg-blue-500',
  PLANNED: 'bg-blue-500',
  DRAFT: 'bg-amber-500',
  FAILED: 'bg-red-500',
};

export default function StatusBadge({ status = 'PLAN', className = '' }) {
  const normalizedStatus = String(status || 'PLAN').toUpperCase();
  const label = statusLabel(normalizedStatus);
  const classes = [
    'pointer-events-none absolute inset-y-0 right-0 w-1.5',
    STATUS_STYLES[normalizedStatus] ?? STATUS_STYLES.PLAN,
    className,
  ].join(' ');

  return <span aria-label={label} className={classes} role="img" title={label} />;
}

function statusLabel(status) {
  if (status === 'GENERATED' || status === 'READY' || status === 'SUCCEEDED') {
    return 'Lesson ready';
  }

  if (status === 'GENERATING' || status === 'QUEUED' || status === 'RUNNING') {
    return 'Lesson generation in progress';
  }

  if (status === 'FAILED') {
    return 'Lesson generation failed';
  }

  return 'Lesson planned';
}
