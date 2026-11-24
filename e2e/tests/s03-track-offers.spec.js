import { test } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage.js';
import { SecretaryOffersTrackingPage } from '../pages/SecretaryOffersTrackingPage.js';

test.describe('S.03 - Acompanhamento de ofertas pelo Secretário', () => {
  let loginPage;
  let trackingPage;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
    trackingPage = new SecretaryOffersTrackingPage(page);
    await loginPage.goto();
    await loginPage.login('secretario', '123456');
    await loginPage.expectLoginSuccessful('/secretaria/ofertas', 'SECRETARIO');
  });

  test('deve listar as ofertas com status e exibir os alunos inscritos e seus status ao abrir detalhes', async () => {
    await trackingPage.expectOffersListWithStatusAndEnrolledCount(
      'Algoritmo e Estrutura de Dados I',
      '2026/1'
    );

    await trackingPage.openOfferDetails('Algoritmo e Estrutura de Dados I');
    await trackingPage.expectEnrolledStudentsWithStatusVisible('Lucas Ferreira', 'Não enviado');
    await trackingPage.expectEnrolledStudentsWithStatusVisible('João Lima', 'Plano enviado');
  });

  test('deve exibir todas as informações e o histórico de status ao clicar em Ver detalhes de um aluno', async () => {
    await trackingPage.openOfferDetails('Algoritmo e Estrutura de Dados I');
    await trackingPage.openStudentDetails('João Lima');
    await trackingPage.expectStudentDetailsAndHistoryVisible(
      'João Lima',
      'joao.lima@estudante.ufscar.br'
    );
  });
});
