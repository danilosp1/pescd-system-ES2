import { expect } from '@playwright/test';

export class StudentOffersPage {
  constructor(page) {
    this.page = page;
    this.heading = page.getByRole('heading', { name: 'Minhas ofertas' });
    this.rows = page.locator('tbody tr');
  }

  async goto() {
    await this.page.goto('/aluno/ofertas');
    await expect(this.heading).toBeVisible();
  }

  async expectOfferWithStatus(offerName, statusLabel) {
    const row = this.rows.filter({ hasText: offerName }).first();
    await expect(row).toBeVisible();
    await expect(row).toContainText(statusLabel);
  }

  async openOffer(offerName) {
    const row = this.rows.filter({ hasText: offerName }).first();
    await row.getByRole('link', { name: 'Abrir' }).click();
    await expect(this.page.getByRole('heading', { name: offerName })).toBeVisible();
  }
}
