export function formatDateTime(value: number | null | undefined): string {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return ''
  return d.toLocaleString()
}
