import { test } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage.js';
import { AdminUsersPage } from '../pages/AdminUsersPage.js';

test.describe('AD.01 - Gerenciamento do cadastro de usuários pelo Administrador', () => {
  let loginPage;
  let adminUsersPage;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
    adminUsersPage = new AdminUsersPage(page);
    await loginPage.goto();
    await loginPage.login('admin', 'admin123');
    await loginPage.expectLoginSuccessful('/admin/usuarios', 'ADMIN');
  });

  test('deve permitir criar, editar e remover um usuário com perfil válido (CRUD)', async () => {
    const suffix = Date.now();
    const username = `prof_e2e_${suffix}`;
    const email = `${username}@ufscar.br`;

    await adminUsersPage.createUser({
      fullName: 'Professor E2E Criado',
      email,
      username,
      password: '123456',
      role: 'PROFESSOR',
    });
    await adminUsersPage.expectSuccessMessage('Usuário criado.');
    await adminUsersPage.expectUserInList(username, 'Professor E2E Criado', 'PROFESSOR');

    await adminUsersPage.editUser(username, {
      fullName: 'Professor E2E Editado',
      role: 'SECRETARIO',
    });
    await adminUsersPage.expectSuccessMessage('Usuário atualizado.');
    await adminUsersPage.expectUserInList(username, 'Professor E2E Editado', 'SECRETARIO');

    await adminUsersPage.deleteUser(username);
    await adminUsersPage.expectSuccessMessage('Usuário removido.');
    await adminUsersPage.expectUserNotInList(username);
  });

  test('deve impedir a remoção do próprio usuário logado e o cadastro de e-mail duplicado', async () => {
    await adminUsersPage.deleteUser('admin');
    await adminUsersPage.expectErrorMessage('Você não pode remover seu próprio usuário.');

    await adminUsersPage.createUser({
      fullName: 'Outro Admin',
      email: 'admin@pescd.com',
      username: `admin_dup_${Date.now()}`,
      password: '123456',
      role: 'ADMIN',
    });
    await adminUsersPage.expectErrorMessage('Já existe um usuário com este e-mail.');
  });
});
