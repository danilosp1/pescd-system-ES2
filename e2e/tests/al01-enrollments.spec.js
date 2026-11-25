import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage.js';
import { StudentOffersPage } from '../pages/StudentOffersPage.js';

test.describe('AL.01 - Visualização das ofertas do aluno', () => {
  let loginPage;
  let studentOffersPage;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
    studentOffersPage = new StudentOffersPage(page);
    await loginPage.goto();
    await loginPage.login('lferreira', '123456');
    await loginPage.expectLoginSuccessful('/aluno/ofertas', 'ALUNO');
    await studentOffersPage.goto();
  });

  test('deve listar as ofertas do aluno com seus respectivos status', async ({ page }) => {
    await expect(page.getByRole('heading', { name: 'Minhas ofertas' })).toBeVisible();

    await studentOffersPage.expectOfferWithStatus(
      'Algoritmo e Estrutura de Dados I',
      'Não enviado'
    );
    await studentOffersPage.expectOfferWithStatus(
      'Processamento Digital de Imagens',
      'Concluído pelo responsável'
    );
    await studentOffersPage.expectOfferWithStatus(
      'Desenvolvimento Web I',
      'Relatório aprovado pelo supervisor'
    );

    await studentOffersPage.openOffer('Algoritmo e Estrutura de Dados I');
    await expect(page).toHaveURL(/\/aluno\/ofertas\/\d+$/);
    await expect(page.getByRole('heading', { name: 'Histórico de status' })).toBeVisible();
  });
});
