import { expect } from '@playwright/test';

export class ResponsibleOffersPage {
  constructor(page) {
    this.page = page;
    this.heading = page.getByRole('heading', { name: 'Professor responsável', exact: true });
    this.searchInput = page.getByPlaceholder('Buscar aluno pelo nome');
    this.searchButton = page.getByRole('button', { name: 'Buscar', exact: true });
    this.rows = page.locator('tbody tr');
  }

  async goto() {
    await this.page.goto('/professor/responsavel');
    await expect(this.heading).toBeVisible();
  }

  offerCard(name) {
    return this.page.locator('.card').filter({ has: this.page.getByRole('heading', { name, exact: true }) });
  }

  async openStudent(offerName, studentName, action) {
    await this.offerCard(offerName).locator('tbody tr').filter({ hasText: studentName })
      .getByRole('link', { name: action, exact: true }).click();
    await expect(this.page.getByRole('heading', { name: studentName, exact: true })).toBeVisible();
  }

  async openOffer(name) {
    await this.offerCard(name).getByRole('link', { name: 'Ver oferta', exact: true }).click();
    await expect(this.page.getByRole('heading', { name, exact: true })).toBeVisible();
  }

  async openClosure(name) {
    await this.offerCard(name).getByRole('link', { name: 'Encerrar oferta', exact: true }).click();
    await expect(this.page.getByRole('heading', { name: 'Encerrar oferta', exact: true })).toBeVisible();
  }

  async search(name) {
    await this.searchInput.fill(name);
    await this.searchButton.click();
  }

  async expectStudent(name, status) {
    const row = this.rows.filter({ hasText: name });
    await expect(row).toBeVisible();
    await expect(row).toContainText(status);
  }
}
