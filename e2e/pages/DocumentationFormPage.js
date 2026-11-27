import { expect } from '@playwright/test';

export class DocumentationFormPage {
  constructor(page) {
    this.page = page;
    this.institutionInput = page.locator('.form-group:has(label:text-is("Instituição *")) input');
    this.disciplineNameInput = page.locator('.form-group:has(label:text-is("Nome da disciplina *")) input');
    this.disciplineCourseInput = page.locator('.form-group:has(label:text-is("Curso da disciplina *")) input');
    this.workloadInput = page.locator('.form-group:has(label:text-is("Carga horária (h) *")) input');
    this.fileInput = page.locator('input[type="file"]');
    this.submitButton = page.getByRole('button', { name: 'Enviar documentação' });
  }

  async fillAndSubmit({ institutionName, disciplineName, disciplineCourse, workloadHours, file }) {
    await this.institutionInput.fill(institutionName);
    await this.disciplineNameInput.fill(disciplineName);
    await this.disciplineCourseInput.fill(disciplineCourse);
    await this.workloadInput.fill(String(workloadHours));
    await this.fileInput.setInputFiles(file);
    await this.submitButton.click();
  }

  async expectError(message) {
    const alert = this.page.locator('.alert-error');
    await expect(alert).toBeVisible();
    await expect(alert).toContainText(message);
  }
}
