import { expect } from '@playwright/test';

export class ReportFormPage {
  constructor(page) {
    this.page = page;
    this.frequencyInput = page.locator('.form-group:has(label:text-is("Frequência (%) *")) input');
    this.fileInput = page.locator('input[type="file"]');
    this.submitButton = page.getByRole('button', { name: 'Enviar relatório' });
  }

  async fillAndSubmit({ frequency, file }) {
    await this.frequencyInput.fill(String(frequency));
    await this.fileInput.setInputFiles(file);
    await this.submitButton.click();
  }

  async expectError(message) {
    const alert = this.page.locator('.alert-error');
    await expect(alert).toBeVisible();
    await expect(alert).toContainText(message);
  }
}
