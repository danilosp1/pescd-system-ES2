import { expect } from '@playwright/test';

export class SecretaryOfferStudentsPage {
  constructor(page) {
    this.page = page;
    this.offerRows = page.locator('tbody tr');
    this.fullNameInput = page.locator('.form-group:has(label:text-is("Nome completo")) input');
    this.emailInput = page.locator('.form-group:has(label:text-is("E-mail")) input');
    this.raInput = page.locator('.form-group:has(label:text-is("RA")) input');
    this.addButton = page.getByRole('button', { name: 'Adicionar' });
    this.csvFileInput = page.locator('input[type="file"][accept=".csv"]');
    this.importButton = page.getByRole('button', { name: 'Importar' });
    this.successAlert = page.locator('.alert-success');
    this.studentsTableRows = page.locator('tbody tr');
  }

  async openOfferByName(offerName) {
    const row = this.offerRows.filter({ hasText: offerName }).first();
    await row.getByRole('link', { name: 'Detalhes' }).click();
    await expect(this.page.getByRole('heading', { name: offerName })).toBeVisible();
  }

  async addStudentManually({ fullName, email, ra }) {
    await this.fullNameInput.fill(fullName);
    await this.emailInput.fill(email);
    await this.raInput.fill(ra);
    await this.addButton.click();
  }

  async importStudentsCsv(csvContent) {
    await this.csvFileInput.setInputFiles({
      name: 'alunos.csv',
      mimeType: 'text/csv',
      buffer: Buffer.from(csvContent, 'utf-8'),
    });
    await this.importButton.click();
  }

  async expectStudentEnrolled(fullName, email) {
    const row = this.studentsTableRows.filter({ hasText: email }).first();
    await expect(row).toBeVisible();
    await expect(row).toContainText(fullName);
    await expect(row).toContainText(email);
  }

  async expectSuccessMessage(expectedText) {
    await expect(this.successAlert).toBeVisible();
    await expect(this.successAlert).toContainText(expectedText);
  }
}
