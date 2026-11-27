import { randomUUID } from 'node:crypto';
import { expect } from '@playwright/test';
import { samplePdfFile } from './samplePdf.js';

export async function loginApi(request, username, password = '123456') {
  const response = await request.post('/api/auth/login', { data: { username, password } });
  await expect(response).toBeOK();
}

async function post(request, url, options) {
  const response = await request.post(url, options);
  await expect(response).toBeOK();
  return response.json();
}

async function prepareStudent(request, scenario, phase, suffix) {
  await loginApi(request, 'secretario');
  const student = {
    fullName: `Aluno ${scenario.story} ${suffix} ${scenario.token}`,
    email: `i3.${scenario.token}.${suffix}@example.com`,
    ra: '910007',
  };
  const detail = await post(request, `/api/secretary/offers/${scenario.offerId}/students`, { data: student });
  const enrollment = detail.students.find((item) => item.student.email === student.email);
  expect(enrollment).toBeTruthy();
  const result = { ...student, studentId: enrollment.student.id, enrollmentId: enrollment.enrollmentId };

  await loginApi(request, student.email, student.ra);
  const base = `/api/aluno/ofertas/${result.enrollmentId}`;
  if (phase === 'documentation' || phase === 'completedDocumentation') {
    await post(request, `${base}/documentacao`, {
      multipart: {
        institutionName: 'Universidade de Teste',
        disciplineName: 'Didática do Ensino Superior',
        disciplineCourse: 'Pós-graduação',
        workloadHours: '60',
        file: samplePdfFile,
      },
    });
    if (phase === 'completedDocumentation') {
      await loginApi(request, 'aendo');
      await post(request, `/api/professor/responsavel/offers/${scenario.offerId}/students/${result.studentId}/documentation/analyze`, {
        data: { parecer: 'Documentação conferida', frequencia: 100, nota: 'B' },
      });
    }
    return result;
  }

  await post(request, `${base}/plano`, {
    multipart: {
      disciplineCode: 'ES-I3',
      disciplineName: 'Engenharia de Software',
      disciplineCourse: 'Ciência da Computação',
      supervisorId: String(scenario.supervisorId),
      file: samplePdfFile,
    },
  });
  const supervisorBase = `/api/professor/supervisor/offers/${scenario.offerId}/students/${result.studentId}`;
  if (phase !== 'plan') {
    // O relatório depende da aprovação prévia do plano.
    await loginApi(request, 'alevada');
    await post(request, `${supervisorBase}/work-plan/approve`, { data: { parecer: 'Plano adequado' } });
    await loginApi(request, student.email, student.ra);
    await post(request, `${base}/relatorio`, { multipart: { frequency: '95', file: samplePdfFile } });
  }
  if (phase === 'approvedReport' || phase === 'completedReport') {
    await loginApi(request, 'alevada');
    await post(request, `${supervisorBase}/report/approve`, {
      data: { parecer: 'Atividades concluídas conforme o plano', frequencia: 95, notaSugestao: 'A' },
    });
  }
  if (phase === 'completedReport') {
    await loginApi(request, 'aendo');
    await post(request, `/api/professor/responsavel/offers/${scenario.offerId}/students/${result.studentId}/report/conclude`, {
      data: { parecer: 'Relatório conferido', frequencia: 95, nota: 'A' },
    });
  }
  return result;
}

// A API prepara as pré-condições; cada teste executa a ação principal pela interface.
export async function createProfessorScenario(request, story, phase) {
  await loginApi(request, 'secretario');
  const response = await request.get('/api/secretary/offers/professors');
  await expect(response).toBeOK();
  const professors = await response.json();
  const supervisor = professors.find((item) => item.username === 'alevada');
  const responsible = professors.find((item) => item.username === 'aendo');
  expect(supervisor).toBeTruthy();
  expect(responsible).toBeTruthy();
  // Identificadores únicos permitem repetir a suíte sem resetar o banco.
  const token = randomUUID().slice(0, 8);
  const offerName = `E2E I3 ${story} ${token}`;
  const offer = await post(request, '/api/secretary/offers', {
    data: {
      name: offerName, semester: '2026/2', startDate: '2026-08-01', endDate: '2026-12-15',
      responsibleProfessorId: responsible.id,
    },
  });
  const scenario = { story, token, offerName, offerId: offer.id, supervisorId: supervisor.id };
  scenario.student = await prepareStudent(request, scenario, phase === 'mixed' ? 'completedReport' : phase, 'relatorio');
  if (phase === 'mixed') {
    scenario.secondStudent = await prepareStudent(request, scenario, 'completedDocumentation', 'documentacao');
  }
  return scenario;
}
