import { test } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage.js';
import { SecretaryOfferStudentsPage } from '../pages/SecretaryOfferStudentsPage.js';

test.describe('S.02 - Adição de alunos a uma oferta pelo Secretário', () => {
  let loginPage;
  let offerStudentsPage;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
    offerStudentsPage = new SecretaryOfferStudentsPage(page);
    await loginPage.goto();
    await loginPage.login('secretario', '123456');
    await loginPage.expectLoginSuccessful('/secretaria/ofertas', 'SECRETARIO');
    await offerStudentsPage.openOfferByName('Algoritmo e Estrutura de Dados I');
  });

  test('deve adicionar um aluno manualmente à oferta selecionada', async () => {
    const suffix = Date.now();
    const fullName = `Aluno Manual E2E ${suffix}`;
    const email = `aluno.manual.${suffix}@estudante.ufscar.br`;
    const ra = `${suffix}`.slice(-6);

    await offerStudentsPage.addStudentManually({ fullName, email, ra });

    await offerStudentsPage.expectSuccessMessage('Aluno adicionado à oferta.');
    await offerStudentsPage.expectStudentEnrolled(fullName, email);
  });

  test('deve importar alunos para a oferta a partir de um arquivo CSV', async () => {
    const suffix = Date.now();
    const ra = `${suffix}`.slice(-6);
    const fullName = `Aluno CSV E2E ${suffix}`;
    const email = `aluno.csv.${suffix}@estudante.ufscar.br`;
    const csvContent = `RA,NOME_COMPLETO,EMAIL\n${ra},${fullName},${email}\n`;

    await offerStudentsPage.importStudentsCsv(csvContent);

    await offerStudentsPage.expectSuccessMessage('1 aluno(s) importado(s) do CSV.');
    await offerStudentsPage.expectStudentEnrolled(fullName, email);
  });
});
