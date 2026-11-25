import { expect } from '@playwright/test';

export class StudentEnrollmentDetailsPage {
  constructor(page) {
    this.page = page;
    this.statusBadge = page.locator('.card').first().locator('.badge');
    this.workPlanLink = page.getByRole('link', { name: /Enviar plano/ });
    this.documentationLink = page.getByRole('link', { name: /Enviar documentação/ });
    this.reportLink = page.getByRole('link', { name: /Enviar relatório/ });
  }

  async expectStatus(statusLabel) {
    await expect(this.statusBadge).toHaveText(statusLabel);
  }

  async openWorkPlanForm() {
    await this.workPlanLink.click();
    await expect(this.page.getByRole('heading', { name: 'Enviar plano de trabalho' })).toBeVisible();
  }

  async openDocumentationForm() {
    await this.documentationLink.click();
    await expect(this.page.getByRole('heading', { name: 'Enviar documentação' })).toBeVisible();
  }

  async openReportForm() {
    await this.reportLink.click();
    await expect(this.page.getByRole('heading', { name: 'Enviar relatório final' })).toBeVisible();
  }

  async expectFileCard(title, expectedText) {
    const card = this.page.locator('.card').filter({
      has: this.page.getByRole('heading', { name: title }),
    }).first();
    await expect(card).toBeVisible();
    await expect(card).toContainText(expectedText);
  }
}
