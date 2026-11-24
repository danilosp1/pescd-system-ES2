import { test } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage.js';

test.describe('U.01 - Realização de login com controle por perfil', () => {
  let loginPage;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
    await loginPage.goto();
  });

  test('deve autenticar usuários válidos e redirecionar para a tela específica de cada perfil', async () => {
    await loginPage.login('admin', 'admin123');
    await loginPage.expectLoginSuccessful('/admin/usuarios', 'ADMIN');
    await loginPage.logout();

    await loginPage.login('secretario', '123456');
    await loginPage.expectLoginSuccessful('/secretaria/ofertas', 'SECRETARIO');
    await loginPage.logout();

    await loginPage.login('lferreira', '123456');
    await loginPage.expectLoginSuccessful('/aluno/ofertas', 'ALUNO');
    await loginPage.logout();

    await loginPage.login('alevada', '123456');
    await loginPage.expectLoginSuccessful('/professor/supervisor', 'PROFESSOR');
    await loginPage.expectProfessorDualRoleMenu();
  });

  test('deve exibir mensagem de erro e permanecer na página de login com credenciais inválidas', async () => {
    await loginPage.login('admin', 'senhaIncorreta');
    await loginPage.expectLoginFailed('Nome de usuário ou senha inválidos.');
  });
});
