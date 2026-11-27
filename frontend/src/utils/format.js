export function formatDate(value) {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  // Datas sem horário são interpretadas como UTC pelo JavaScript.
  const isCalendarDate = typeof value === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(value);
  return isCalendarDate
    ? date.toLocaleDateString('pt-BR', { timeZone: 'UTC' })
    : date.toLocaleDateString('pt-BR');
}

export function formatDateTime(value) {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString('pt-BR');
}

// Rótulos das notas (espelha o enum GradeOption do back-end).
export const GRADE_OPTIONS = [
  { value: 'A', label: 'A — Excelente' },
  { value: 'B', label: 'B — Bom' },
  { value: 'C', label: 'C — Regular' },
  { value: 'D', label: 'D — Insuficiente' },
  { value: 'E', label: 'E — Reprovado' },
];
