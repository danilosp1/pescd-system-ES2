import { expect } from '@playwright/test';

export class SupervisorDashboardPage {
  constructor(page) {
    this.page = page;
    this.heading = page.getByRole('heading', { name: 'Professor supervisor' });
    this.offerCards = page.locator('.card');
  }

  async expectLoaded() {
    await expect(this.heading).toBeVisible();
    await expect(this.page.getByText('Ofertas e alunos que você supervisiona.')).toBeVisible();
  }

  async expectOfferVisible(offerName) {
    const card = this.offerCards.filter({
      has: this.page.getByRole('heading', { name: offerName }),
    }).first();
    await expect(card).toBeVisible();
  }

  async expectStudentStatus(offerName, studentName, statusLabel) {
    const card = this.offerCards.filter({
      has: this.page.getByRole('heading', { name: offerName }),
    }).first();
    const row = card.locator('tbody tr').filter({ hasText: studentName }).first();
    await expect(row).toBeVisible();
    await expect(row).toContainText(statusLabel);
  }

  async openStudent(offerName, studentName, action) {
    const card = this.offerCards.filter({
      has: this.page.getByRole('heading', { name: offerName, exact: true }),
    });
    await card.locator('tbody tr').filter({ hasText: studentName })
      .getByRole('link', { name: action, exact: true }).click();
    await expect(this.page.getByRole('heading', { name: studentName, exact: true })).toBeVisible();
  }
}
