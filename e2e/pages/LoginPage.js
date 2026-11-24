import { expect } from '@playwright/test';

export class LoginPage {
  constructor(page) {
    this.page = page;
    this.usernameInput = page.locator('#username');
    this.passwordInput = page.locator('#password');
    this.loginButton = page.getByRole('button', { name: 'Entrar' });
    this.errorAlert = page.locator('.alert-error');
    this.userChip = page.locator('.user-chip');
    this.logoutButton = page.getByRole('button', { name: 'Sair' });
    this.navigation = page.locator('header .nav');
  }

  async goto() {
    await this.page.goto('/login');
    await expect(this.usernameInput).toBeVisible();
    await expect(this.passwordInput).toBeVisible();
    await expect(this.loginButton).toBeVisible();
  }

  async login(username, password) {
    await this.usernameInput.fill(username);
    await this.passwordInput.fill(password);
    await this.loginButton.click();
  }

  async logout() {
    await this.logoutButton.click();
    await expect(this.page).toHaveURL(/\/login$/);
  }

  async expectLoginSuccessful(expectedPath, expectedRole) {
    await expect(this.page).toHaveURL(new RegExp(expectedPath));
    await expect(this.userChip).toContainText(expectedRole);
    await expect(this.logoutButton).toBeVisible();
  }

  async expectProfessorDualRoleMenu() {
    await expect(this.navigation.getByRole('link', { name: 'Supervisor' })).toBeVisible();
    await expect(this.navigation.getByRole('link', { name: 'Responsável' })).toBeVisible();
  }

  async expectLoginFailed(expectedMessage) {
    await expect(this.page).toHaveURL(/\/login$/);
    await expect(this.errorAlert).toBeVisible();
    await expect(this.errorAlert).toContainText(expectedMessage);
  }
}
