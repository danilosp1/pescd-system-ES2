import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage.js';
import { StudentOffersPage } from '../pages/StudentOffersPage.js';
import { StudentEnrollmentDetailsPage } from '../pages/StudentEnrollmentDetailsPage.js';
import { DocumentationFormPage } from '../pages/DocumentationFormPage.js';
import { samplePdfFile } from '../fixtures/samplePdf.js';

test.describe('AL.03 - Envio da documentação comprobatória pelo aluno', () => {
  let loginPage;
  let studentOffersPage;
  let enrollmentDetailsPage;
  let documentationFormPage;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
    studentOffersPage = new StudentOffersPage(page);
    enrollmentDetailsPage = new StudentEnrollmentDetailsPage(page);
    documentationFormPage = new DocumentationFormPage(page);

    await loginPage.goto();
    await loginPage.login('alunodocumentacaoe2e', '123456');
    await loginPage.expectLoginSuccessful('/aluno/ofertas', 'ALUNO');
    await studentOffersPage.goto();
    await studentOffersPage.openOffer('Algoritmo e Estrutura de Dados I');
  });

  test('deve enviar a documentação em PDF e atualizar o status para documentação enviada', async ({ page }) => {
    await enrollmentDetailsPage.expectStatus('Não enviado');
    await enrollmentDetailsPage.openDocumentationForm();

    await documentationFormPage.fillAndSubmit({
      institutionName: 'Universidade Federal de São Carlos',
      disciplineName: 'Engenharia de Software 2',
      disciplineCourse: 'Ciência da Computação',
      workloadHours: 60,
      file: samplePdfFile,
    });

    await expect(page).toHaveURL(/\/aluno\/ofertas\/\d+$/);
    await enrollmentDetailsPage.expectStatus('Documentação enviada');
    await enrollmentDetailsPage.expectFileCard(
      'Documentação comprobatória',
      'Universidade Federal de São Carlos'
    );
  });
});
