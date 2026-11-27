import { expect } from '@playwright/test';

export class WorkPlanFormPage {
  constructor(page) {
    this.page = page;
    this.disciplineCodeInput = page.locator('.form-group:has(label:text-is("Código da disciplina *")) input');
    this.disciplineNameInput = page.locator('.form-group:has(label:text-is("Nome da disciplina *")) input');
    this.disciplineCourseInput = page.locator('.form-group:has(label:text-is("Curso da disciplina *")) input');
    this.supervisorSelect = page.locator('.form-group:has(label:text-is("Professor supervisor *")) select');
    this.fileInput = page.locator('input[type="file"]');
    this.submitButton = page.getByRole('button', { name: 'Enviar plano' });
  }

  async fillAndSubmit({ disciplineCode, disciplineName, disciplineCourse, supervisorName, file }) {
    await this.disciplineCodeInput.fill(disciplineCode);
    await this.disciplineNameInput.fill(disciplineName);
    await this.disciplineCourseInput.fill(disciplineCourse);
    await this.supervisorSelect.selectOption({ label: supervisorName });
    await this.fileInput.setInputFiles(file);
    await this.submitButton.click();
  }

  async expectError(message) {
    const alert = this.page.locator('.alert-error');
    await expect(alert).toBeVisible();
    await expect(alert).toContainText(message);
  }
}
