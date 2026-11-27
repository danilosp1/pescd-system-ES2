import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { api, openAuthenticatedFile } from '../../api/client.js';
import { formatDateTime, GRADE_OPTIONS } from '../../utils/format.js';
import { ErrorMessage, Loading, SuccessMessage, StatusBadge } from '../../components/Feedback.jsx';
import { InfoGrid, StatusTimeline, SectionTitle } from '../../components/DataGrid.jsx';

// PS.02 (aprovar plano) e PS.03 (avaliar relatório) do professor supervisor.
// Espelha as telas approve-plan.html e approve-report.html.
export default function SupervisorEnrollmentPage() {
  const { offerId, studentId } = useParams();
  const base = `/api/professor/supervisor/offers/${offerId}/students/${studentId}`;

  const [detail, setDetail] = useState(null);
  const [workPlan, setWorkPlan] = useState(null);
  const [report, setReport] = useState(null);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [planParecer, setPlanParecer] = useState('');
  const [reportForm, setReportForm] = useState({ parecer: '', frequencia: null, notaSugestao: '' });
  const [rejectionReason, setRejectionReason] = useState('');
  const [busy, setBusy] = useState(false);

  // Uma edição vazia deve continuar vazia para a validação do formulário.
  const confirmedFrequency = reportForm.frequencia ?? report?.frequency ?? '';

  async function load() {
    try {
      const d = await api.get(base);
      setDetail(d);
      if (d.enrollment.hasWorkPlan) {
        setWorkPlan(await api.get(`${base}/work-plan`).catch(() => null));
      }
      if (d.enrollment.hasReport) {
        setReport(await api.get(`${base}/report`).catch(() => null));
      }
    } catch (e) {
      setError(e.message);
    }
  }
  useEffect(() => { load(); }, [offerId, studentId]);

  async function approvePlan(event) {
    event.preventDefault();
    setError(''); setMessage(''); setBusy(true);
    try {
      await api.post(`${base}/work-plan/approve`, { parecer: planParecer });
      setMessage('Plano de trabalho aprovado.');
      setPlanParecer('');
      await load();
    } catch (err) { setError(err.message); } finally { setBusy(false); }
  }

  async function rejectPlan() {
    if (!planParecer.trim()) {
      setError('O parecer é obrigatório.');
      return;
    }
    setError(''); setMessage(''); setBusy(true);
    try {
      await api.post(`${base}/work-plan/reject`, { parecer: planParecer });
      setMessage('Plano de trabalho reprovado. O aluno poderá reenviar o plano.');
      setPlanParecer('');
      await load();
    } catch (err) { setError(err.message); } finally { setBusy(false); }
  }

  async function approveReport(event) {
    event.preventDefault();
    setError(''); setMessage(''); setBusy(true);
    try {
      await api.post(`${base}/report/approve`, {
        parecer: reportForm.parecer,
        frequencia: confirmedFrequency === '' ? null : Number(confirmedFrequency),
        notaSugestao: reportForm.notaSugestao,
      });
      setMessage('Relatório aprovado.');
      await load();
    } catch (err) { setError(err.message); } finally { setBusy(false); }
  }

  async function rejectReport(event) {
    event.preventDefault();
    setError(''); setMessage(''); setBusy(true);
    try {
      await api.post(`${base}/report/reject`, { parecer: rejectionReason.trim() });
      setMessage('Relatório reprovado. O aluno pode reenviar o relatório.');
      setRejectionReason('');
      await load();
    } catch (err) { setError(err.message); } finally { setBusy(false); }
  }

  if (!detail && !error) return <Loading />;
  if (!detail) return <ErrorMessage>{error}</ErrorMessage>;

  const { offer, enrollment } = detail;
  const status = enrollment.status;

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>{enrollment.student.fullName}</h1>
          <p>{offer.name} · <StatusBadge label={enrollment.statusLabel} status={status} /></p>
        </div>
        <Link className="btn btn-ghost" to="/professor/supervisor">Voltar</Link>
      </div>

      <ErrorMessage>{error}</ErrorMessage>
      <SuccessMessage>{message}</SuccessMessage>

      {/* Dados do plano de trabalho */}
      {workPlan && (
        <div className="card">
          <SectionTitle>Plano de trabalho (enviado pelo aluno)</SectionTitle>
          <InfoGrid items={[
            { label: 'Código da disciplina', value: workPlan.disciplineCode },
            { label: 'Nome da disciplina', value: workPlan.disciplineName },
            { label: 'Curso', value: workPlan.disciplineCourse, span2: true },
            { label: 'Enviado em', value: formatDateTime(workPlan.submittedAt) },
          ]} />
          <button className="btn btn-ghost btn-sm" style={{ marginTop: 12 }}
            onClick={() => openAuthenticatedFile(`${base}/work-plan/download`)}>
            📄 Visualizar PDF do Plano
          </button>
          {workPlan.supervisorRejectedAt && (
            <p className="alert alert-error" style={{ marginTop: 12, marginBottom: 0 }}>
              Plano reprovado em {formatDateTime(workPlan.supervisorRejectedAt)} — Parecer: {workPlan.supervisorParecer}
            </p>
          )}
        </div>
      )}

      {/* Dados do relatório */}
      {report && (
        <div className="card">
          <SectionTitle>Relatório final (enviado pelo aluno)</SectionTitle>
          <InfoGrid items={[
            { label: 'Frequência indicada pelo aluno', value: `${report.frequency}%` },
            { label: 'Enviado em', value: formatDateTime(report.submittedAt) },
            ...(status === 'RELATORIO_REPROVADO'
              ? [{ label: 'Parecer da reprovação', value: report.supervisorParecer, span2: true }]
              : []),
          ]} />
          <button className="btn btn-ghost btn-sm" style={{ marginTop: 12 }}
            onClick={() => openAuthenticatedFile(`${base}/report/download`)}>
            📄 Visualizar PDF do Relatório
          </button>
        </div>
      )}

      {/* PS.02 — aprovar plano */}
      {status === 'PLANO_ENVIADO' && (
        <div className="card" style={{ maxWidth: 720 }}>
          <SectionTitle>Parecer do supervisor (PS.02 — aprovar plano)</SectionTitle>
          <form onSubmit={approvePlan}>
            <div className="form-group">
              <label>Parecer *</label>
              <textarea rows={4} value={planParecer} onChange={(e) => setPlanParecer(e.target.value)}
                placeholder="Descreva seu parecer sobre o plano de trabalho…" required />
            </div>
            <div className="form-actions">
              <button className="btn" type="submit" disabled={busy}>✓ Aprovar plano</button>
              <button className="btn btn-ghost" type="button" onClick={rejectPlan} disabled={busy}>
                Reprovar plano
              </button>
            </div>
          </form>
        </div>
      )}

      {/* PS.03 — aprovar relatório */}
      {status === 'RELATORIO_ENVIADO' && (
        <div className="card" style={{ maxWidth: 720 }}>
          <SectionTitle>Parecer do supervisor (PS.03 — aprovar relatório)</SectionTitle>
          <form onSubmit={approveReport}>
            <div className="form-group">
              <label>Parecer *</label>
              <textarea rows={4} value={reportForm.parecer}
                onChange={(e) => setReportForm((p) => ({ ...p, parecer: e.target.value }))}
                placeholder="Descreva seu parecer sobre o relatório final…" required />
            </div>
            <div className="form-row">
              <div className="form-group">
                <label>Frequência confirmada (%) *</label>
                <input type="number" min="0" max="100" value={confirmedFrequency}
                  onChange={(e) => setReportForm((p) => ({ ...p, frequencia: e.target.value }))} required />
              </div>
              <div className="form-group">
                <label>Sugestão de nota *</label>
                <select value={reportForm.notaSugestao}
                  onChange={(e) => setReportForm((p) => ({ ...p, notaSugestao: e.target.value }))} required>
                  <option value="">— Selecione —</option>
                  {GRADE_OPTIONS.map((g) => <option key={g.value} value={g.value}>{g.label}</option>)}
                </select>
              </div>
            </div>
            <button className="btn" type="submit" disabled={busy}>✓ Aprovar relatório</button>
          </form>
        </div>
      )}

      {status === 'RELATORIO_ENVIADO' && offer.status === 'EM_ANDAMENTO' && report && (
        <div className="card" style={{ maxWidth: 720 }}>
          <SectionTitle>Reprovação do relatório</SectionTitle>
          <form onSubmit={rejectReport}>
            <div className="form-group">
              <label htmlFor="rejection-reason">Parecer da reprovação *</label>
              <textarea id="rejection-reason" value={rejectionReason} rows={4} required
                onChange={(event) => setRejectionReason(event.target.value)} />
            </div>
            <button className="btn btn-ghost" type="submit" disabled={busy || !rejectionReason.trim()}>
              Reprovar relatório
            </button>
          </form>
        </div>
      )}

      <div className="card">
        <SectionTitle>Histórico de status</SectionTitle>
        <StatusTimeline logs={detail.statusLogs} />
      </div>
    </div>
  );
}
