import { expect } from '@playwright/test';

export class ResponsibleEvaluationPage {
  constructor(page) {
    this.page = page;
    this.form = page.locator('form');
    this.parecerInput = this.form.getByRole('textbox');
    this.frequencyInput = this.form.getByRole('spinbutton');
    this.gradeSelect = this.form.getByRole('combobox');
    this.submitButton = this.form.getByRole('button');
    this.status = page.locator('.page-header .badge');
  }

  async conclude({ parecer, frequency, grade }) {
    await this.parecerInput.fill(parecer);
    await this.frequencyInput.fill(String(frequency));
    await this.gradeSelect.selectOption(grade);
    await this.submitButton.click();
  }

  async expectConcluded() {
    await expect(this.status).toHaveText('Concluído pelo responsável');
    const log = this.page.locator('.timeline li').filter({
      has: this.page.getByText('Concluído pelo responsável', { exact: true }),
    });
    await expect(log).toBeVisible();
    await expect(log.locator('.log-date')).toHaveText(/\d{2}\/\d{2}\/\d{4}/);
    await expect(this.form).toHaveCount(0);
  }

  async goBack() {
    await this.page.getByRole('link', { name: 'Voltar', exact: true }).click();
    await expect(this.page).toHaveURL(/\/professor\/responsavel$/);
  }
}
