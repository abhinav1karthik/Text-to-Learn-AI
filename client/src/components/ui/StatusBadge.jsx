const STATUS_STYLES = {
  READY: 'bg-green-500',
  GENERATED: 'bg-green-500',
  PLAN: 'bg-blue-500',
  PLANNED: 'bg-blue-500',
  DRAFT: 'bg-amber-500',
  FAILED: 'bg-red-500',
};

export default function StatusBadge({ status = 'PLAN', className = '' }) {
  const normalizedStatus = String(status || 'PLAN').toUpperCase();
  const label = normalizedStatus === 'GENERATED' || normalizedStatus === 'READY' ? 'Lesson ready' : 'Lesson planned';
  const classes = [
    'pointer-events-none absolute inset-y-0 right-0 w-1.5',
    STATUS_STYLES[normalizedStatus] ?? STATUS_STYLES.PLAN,
    className,
  ].join(' ');

  return <span aria-label={label} className={classes} role="img" title={label} />;
}
