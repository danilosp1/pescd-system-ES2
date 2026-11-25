import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage.js';
import { StudentOffersPage } from '../pages/StudentOffersPage.js';
import { StudentEnrollmentDetailsPage } from '../pages/StudentEnrollmentDetailsPage.js';
import { ReportFormPage } from '../pages/ReportFormPage.js';
import { samplePdfFile } from '../fixtures/samplePdf.js';

test.describe('AL.04 - Envio do relatório final pelo aluno', () => {
  let loginPage;
  let studentOffersPage;
  let enrollmentDetailsPage;
  let reportFormPage;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
    studentOffersPage = new StudentOffersPage(page);
    enrollmentDetailsPage = new StudentEnrollmentDetailsPage(page);
    reportFormPage = new ReportFormPage(page);

    await loginPage.goto();
    await loginPage.login('alunorelatorioe2e', '123456');
    await loginPage.expectLoginSuccessful('/aluno/ofertas', 'ALUNO');
    await studentOffersPage.goto();
    await studentOffersPage.openOffer('Algoritmo e Estrutura de Dados I');
  });

  test('deve enviar o relatório com frequência e atualizar o status para relatório enviado', async ({ page }) => {
    await enrollmentDetailsPage.expectStatus('Plano aprovado');
    await enrollmentDetailsPage.openReportForm();

    await reportFormPage.fillAndSubmit({
      frequency: 90,
      file: samplePdfFile,
    });

    await expect(page).toHaveURL(/\/aluno\/ofertas\/\d+$/);
    await enrollmentDetailsPage.expectStatus('Relatório enviado');
    await enrollmentDetailsPage.expectFileCard('Relatório final', '90%');
  });
});
