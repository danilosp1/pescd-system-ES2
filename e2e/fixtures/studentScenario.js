import { randomUUID } from 'node:crypto';
import { expect } from '@playwright/test';
import { samplePdfFile } from './samplePdf.js';
import { loginApi } from './professorScenario.js';

async function post(request, url, options) {
  const response = await request.post(url, options);
  await expect(response).toBeOK();
  return response.json();
}

export async function createStudentScenario(request, story, phase = 'none') {
  await loginApi(request, 'secretario');

  const professorsResponse = await request.get('/api/secretary/offers/professors');
  await expect(professorsResponse).toBeOK();
  const professors = await professorsResponse.json();
  const supervisor = professors.find((item) => item.username === 'alevada');
  const responsible = professors.find((item) => item.username === 'aendo');
  expect(supervisor).toBeTruthy();
  expect(responsible).toBeTruthy();

  const token = randomUUID().slice(0, 8);
  const offerName = `E2E ${story} ${token}`;
  const offer = await post(request, '/api/secretary/offers', {
    data: {
      name: offerName,
      semester: '2026/2',
      startDate: '2026-08-01',
      endDate: '2026-12-15',
      responsibleProfessorId: responsible.id,
    },
  });

  const student = {
    fullName: `Aluno ${story} ${token}`,
    email: `${story.toLowerCase()}.${token}@example.com`,
    ra: '910008',
  };
  const detail = await post(request, `/api/secretary/offers/${offer.id}/students`, { data: student });
  const enrollment = detail.students.find((item) => item.student.email === student.email);
  expect(enrollment).toBeTruthy();

  const scenario = {
    story,
    token,
    offerName,
    offerId: offer.id,
    supervisorId: supervisor.id,
    student,
    studentId: enrollment.student.id,
    enrollmentId: enrollment.enrollmentId,
  };

  if (phase === 'approvedPlan') {
    await loginApi(request, student.email, student.ra);
    await post(request, `/api/aluno/ofertas/${scenario.enrollmentId}/plano`, {
      multipart: {
        disciplineCode: 'ES-I2',
        disciplineName: 'Engenharia de Software 2',
        disciplineCourse: 'Ciência da Computação',
        supervisorId: String(supervisor.id),
        file: samplePdfFile,
      },
    });

    await loginApi(request, 'alevada');
    await post(
      request,
      `/api/professor/supervisor/offers/${offer.id}/students/${scenario.studentId}/work-plan/approve`,
      { data: { parecer: 'Plano adequado para o estágio' } }
    );
  }

  return scenario;
}
