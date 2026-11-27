import { useEffect, useRef, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { api, openAuthenticatedFile } from '../../api/client.js';
import { ErrorMessage, Loading } from '../../components/Feedback.jsx';
import StatusHistory from '../../components/StatusHistory.jsx';

// AL.04 - Envio do relatório final (frequência 0-100, PDF <= 5MB), com leitura
// do plano de trabalho e do histórico de status.
export default function ReportPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const fileRef = useRef(null);
  const [data, setData] = useState(null);
  const [frequency, setFrequency] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const base = `/api/aluno/ofertas/${id}`;

  useEffect(() => {
    api.get(base).then(setData).catch((e) => setError(e.message));
  }, [id]);

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      const file = fileRef.current.files[0];
      if (!file) throw new Error('Selecione o arquivo PDF do relatório.');
      const form = new FormData();
      form.append('frequency', frequency);
      form.append('file', file);
      await api.postForm(`${base}/relatorio`, form);
      navigate(`/aluno/ofertas/${id}`);
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  if (!data && !error) return <Loading />;
  if (!data) return <ErrorMessage>{error}</ErrorMessage>;

  const { offer, enrollment, workPlan, report, statusLogs } = data;
  const resubmission = enrollment.status === 'RELATORIO_REPROVADO';
  const canSubmit = offer.status === 'EM_ANDAMENTO' && (resubmission || enrollment.status === 'PLANO_APROVADO');

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>{resubmission ? 'Reenviar relatório final' : 'Enviar relatório final'}</h1>
          <p>AL.04 — frequência (0–100) e PDF de até 5 MB.</p>
        </div>
        <Link className="btn btn-ghost" to={`/aluno/ofertas/${id}`}>Voltar</Link>
      </div>

      {workPlan && (
        <div className="card">
          <div className="row-between">
            <h2 style={{ margin: 0, fontSize: 18 }}>Plano de trabalho aprovado</h2>
            <button className="btn btn-ghost btn-sm" onClick={() => openAuthenticatedFile(`${base}/plano/arquivo`)}>
              Abrir PDF
            </button>
          </div>
          <p className="muted" style={{ marginBottom: 0 }}>
            {workPlan.disciplineCode} — {workPlan.disciplineName} · {workPlan.disciplineCourse}
          </p>
        </div>
      )}

      {resubmission && report && (
        <div className="card">
          <h2 style={{ marginTop: 0, fontSize: 18 }}>Parecer da reprovação</h2>
          <p style={{ whiteSpace: 'pre-wrap' }}>{report.supervisorParecer}</p>
        </div>
      )}

      {canSubmit && <div className="card" style={{ maxWidth: 620 }}>
        <form onSubmit={handleSubmit}>
          <ErrorMessage>{error}</ErrorMessage>
          <div className="form-group">
            <label htmlFor="report-frequency">Frequência (%) *</label>
            <input
              id="report-frequency"
              type="number"
              min="0"
              max="100"
              value={frequency}
              onChange={(e) => setFrequency(e.target.value)}
              required
            />
          </div>
          <div className="form-group">
            <label htmlFor="report-file">Arquivo (PDF) *</label>
            <input id="report-file" type="file" accept="application/pdf,.pdf" ref={fileRef} required />
          </div>
          <div className="form-actions">
            <button className="btn" type="submit" disabled={submitting}>
              {submitting ? 'Enviando…' : resubmission ? 'Reenviar relatório' : 'Enviar relatório'}
            </button>
          </div>
        </form>
      </div>}
      {!canSubmit && <ErrorMessage>O envio do relatório está indisponível nesta inscrição.</ErrorMessage>}

      <StatusHistory logs={statusLogs} />
    </div>
  );
}
