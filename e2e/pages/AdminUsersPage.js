import { expect } from '@playwright/test';

export class AdminUsersPage {
  constructor(page) {
    this.page = page;
    this.heading = page.getByRole('heading', { name: 'Usuários' });
    this.fullNameInput = page.locator('.form-group:has(label:text-is("Nome completo")) input');
    this.emailInput = page.locator('.form-group:has(label:text-is("E-mail")) input');
    this.usernameInput = page.locator('.form-group:has(label:text-is("Usuário")) input');
    this.passwordInput = page.locator('.form-group:has(label:has-text("Senha")) input');
    this.roleSelect = page.locator('.form-group:has(label:text-is("Perfil")) select');
    this.createButton = page.getByRole('button', { name: 'Criar' });
    this.saveButton = page.getByRole('button', { name: 'Salvar' });
    this.successAlert = page.locator('.alert-success');
    this.errorAlert = page.locator('.alert-error');
    this.rows = page.locator('tbody tr');
  }

  userRow(username) {
    return this.rows.filter({ hasText: username }).first();
  }

  async createUser({ fullName, email, username, password, role }) {
    await this.fullNameInput.fill(fullName);
    await this.emailInput.fill(email);
    await this.usernameInput.fill(username);
    await this.passwordInput.fill(password);
    await this.roleSelect.selectOption(role);
    await this.createButton.click();
  }

  async editUser(username, { fullName, role }) {
    const row = this.userRow(username);
    await row.getByRole('button', { name: 'Editar' }).click();
    if (fullName) {
      await this.fullNameInput.fill(fullName);
    }
    if (role) {
      await this.roleSelect.selectOption(role);
    }
    await this.saveButton.click();
  }

  async deleteUser(username) {
    this.page.once('dialog', (dialog) => dialog.accept());
    const row = this.userRow(username);
    await row.getByRole('button', { name: 'Remover' }).click();
  }

  async expectSuccessMessage(text) {
    await expect(this.successAlert).toBeVisible();
    await expect(this.successAlert).toContainText(text);
  }

  async expectErrorMessage(text) {
    await expect(this.errorAlert).toBeVisible();
    await expect(this.errorAlert).toContainText(text);
  }

  async expectUserInList(username, fullName, role) {
    const row = this.userRow(username);
    await expect(row).toBeVisible();
    await expect(row).toContainText(fullName);
    await expect(row).toContainText(role);
  }

  async expectUserNotInList(username) {
    await expect(this.rows.filter({ hasText: username })).toHaveCount(0);
  }
}
