import { formatDateTime } from '../utils/format.js';

const changedAt = (log) => formatDateTime(log.changedAt);
const previousStatus = (log) => log.previousStatusLabel || '—';
const newStatus = (log) => log.newStatusLabel;
const description = (log) => log.description;

const studentView = {
  emptyMessage: 'Sem alterações registradas.',
  columns: [
    ['Quando', changedAt],
    ['De', previousStatus],
    ['Para', newStatus],
    ['Descrição', description],
  ],
};

const secretaryView = {
  emptyMessage: 'Nenhuma alteração de status registrada.',
  columns: [
    ['Status anterior', previousStatus],
    ['Novo status', newStatus],
    ['Descrição', description],
    ['Data', changedAt],
  ],
};

export default function StatusHistory({ logs, variant = 'student' }) {
  const { columns, emptyMessage } = variant === 'secretary' ? secretaryView : studentView;

  return (
    <div className="card">
      <h2 style={{ marginTop: 0, fontSize: 18 }}>Histórico de status</h2>
      {logs.length === 0 ? (
        <p className="muted">{emptyMessage}</p>
      ) : (
        <table>
          <thead>
            <tr>
              {columns.map(([heading]) => <th key={heading}>{heading}</th>)}
            </tr>
          </thead>
          <tbody>
            {logs.map((log) => (
              <tr key={log.id}>
                {columns.map(([heading, value]) => <td key={heading}>{value(log)}</td>)}
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
