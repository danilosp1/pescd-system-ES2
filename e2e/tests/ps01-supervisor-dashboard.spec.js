import { test } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage.js';
import { SupervisorDashboardPage } from '../pages/SupervisorDashboardPage.js';

test.describe('PS.01 - Visualização das ofertas e alunos supervisionados', () => {
  let loginPage;
  let supervisorDashboardPage;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
    supervisorDashboardPage = new SupervisorDashboardPage(page);
    await loginPage.goto();
    await loginPage.login('alevada', '123456');
    await loginPage.expectLoginSuccessful('/professor/supervisor', 'PROFESSOR');
  });

  test('deve listar as ofertas e os alunos sob supervisão com seus status', async () => {
    await supervisorDashboardPage.expectLoaded();
    await supervisorDashboardPage.expectOfferVisible('Algoritmo e Estrutura de Dados I');

    await supervisorDashboardPage.expectStudentStatus(
      'Algoritmo e Estrutura de Dados I',
      'João Lima',
      'Plano enviado'
    );
  });
});
