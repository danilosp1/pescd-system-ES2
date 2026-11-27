import { expect } from '@playwright/test';

export class SecretaryOfferCreatePage {
  constructor(page) {
    this.page = page;
    this.newOfferLink = page.getByRole('link', { name: 'Nova oferta' });
    this.heading = page.getByRole('heading', { name: 'Nova oferta' });
    this.nameInput = page.locator('.form-group:has(label:text-is("Nome (opcional)")) input');
    this.semesterInput = page.locator('.form-group:has(label:text-is("Semestre *")) input');
    this.startDateInput = page.locator('.form-group:has(label:text-is("Início *")) input');
    this.endDateInput = page.locator('.form-group:has(label:text-is("Fim *")) input');
    this.professorSelect = page.locator('.form-group:has(label:text-is("Professor responsável *")) select');
    this.submitButton = page.getByRole('button', { name: 'Criar oferta' });
    this.errorAlert = page.locator('.alert-error');
  }

  async openFromOffersList() {
    await this.newOfferLink.click();
    await expect(this.heading).toBeVisible();
  }

  async fillAndSubmit({ name = '', semester, startDate, endDate, professorName }) {
    await this.nameInput.fill(name);
    await this.semesterInput.fill(semester);
    await this.startDateInput.fill(startDate);
    await this.endDateInput.fill(endDate);
    await this.professorSelect.selectOption({ label: professorName });
    await this.submitButton.click();
  }

  async expectOfferCreated(expectedName, expectedProfessor) {
    await expect(this.page).toHaveURL(/\/secretaria\/ofertas\/\d+$/);
    await expect(this.page.getByRole('heading', { name: expectedName })).toBeVisible();
    await expect(this.page.getByText(`Responsável: ${expectedProfessor}`)).toBeVisible();
  }

  async expectOfferPeriod(startDate, endDate) {
    const period = this.page.locator('.page-header p');
    await expect(period).toContainText(`${startDate} – ${endDate}`);
  }

  async expectCreationError(expectedMessage) {
    await expect(this.errorAlert).toBeVisible();
    await expect(this.errorAlert).toContainText(expectedMessage);
  }
}
