import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage.js';
import { StudentOffersPage } from '../pages/StudentOffersPage.js';
import { StudentEnrollmentDetailsPage } from '../pages/StudentEnrollmentDetailsPage.js';
import { WorkPlanFormPage } from '../pages/WorkPlanFormPage.js';
import { samplePdfFile } from '../fixtures/samplePdf.js';
import { createStudentScenario } from '../fixtures/studentScenario.js';

test.describe('AL.02 - Envio do plano de trabalho pelo aluno', () => {
  let loginPage;
  let studentOffersPage;
  let enrollmentDetailsPage;
  let workPlanFormPage;
  let scenario;

  test.beforeEach(async ({ page, request }) => {
    scenario = await createStudentScenario(request, 'AL02');
    loginPage = new LoginPage(page);
    studentOffersPage = new StudentOffersPage(page);
    enrollmentDetailsPage = new StudentEnrollmentDetailsPage(page);
    workPlanFormPage = new WorkPlanFormPage(page);

    await loginPage.goto();
    await loginPage.login(scenario.student.email, scenario.student.ra);
    await loginPage.expectLoginSuccessful('/aluno/ofertas', 'ALUNO');
    await studentOffersPage.goto();
    await studentOffersPage.openOffer(scenario.offerName);
  });

  test('deve enviar o plano de trabalho em PDF e atualizar o status para plano enviado', async ({ page }) => {
    await enrollmentDetailsPage.expectStatus('Não enviado');
    await enrollmentDetailsPage.openWorkPlanForm();

    await workPlanFormPage.fillAndSubmit({
      disciplineCode: '1000999',
      disciplineName: 'Engenharia de Software 2',
      disciplineCourse: 'Ciência da Computação',
      supervisorName: 'Alexandre Levada',
      file: samplePdfFile,
    });

    await expect(page).toHaveURL(/\/aluno\/ofertas\/\d+$/);
    await enrollmentDetailsPage.expectStatus('Plano enviado');
    await enrollmentDetailsPage.expectFileCard('Plano de trabalho', '1000999');
    await enrollmentDetailsPage.expectFileCard('Plano de trabalho', 'documento-e2e.pdf');
  });
});
