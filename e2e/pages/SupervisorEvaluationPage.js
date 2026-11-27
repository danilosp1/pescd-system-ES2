import { expect } from '@playwright/test';

export class SupervisorEvaluationPage {
  constructor(page) {
    this.page = page;
    this.planForm = page.locator('form').filter({ has: page.getByRole('button', { name: '✓ Aprovar plano', exact: true }) });
    this.reportForm = page.locator('form').filter({ has: page.getByRole('button', { name: '✓ Aprovar relatório', exact: true }) });
    this.status = page.locator('.page-header .badge');
    this.history = page.locator('.timeline');
  }

  async expectPlanDetails() {
    const card = this.page.locator('.card').filter({ hasText: 'Plano de trabalho (enviado pelo aluno)' });
    await expect(card).toContainText('ES-I3');
    await expect(card).toContainText('Engenharia de Software');
    await expect(card).toContainText('Ciência da Computação');
    await expect(card.getByRole('button', { name: /Visualizar PDF do Plano/ })).toBeVisible();
    await expect(card.locator('input, textarea, select')).toHaveCount(0);
  }

  async approvePlan(parecer) {
    await this.planForm.getByRole('textbox').fill(parecer);
    await this.planForm.getByRole('button').click();
  }

  async approveReport({ parecer, frequency, grade }) {
    await this.reportForm.getByRole('textbox').fill(parecer);
    await this.reportForm.getByRole('spinbutton').fill(String(frequency));
    await this.reportForm.getByRole('combobox').selectOption(grade);
    await this.reportForm.getByRole('button').click();
  }

  async expectStatus(status) {
    await expect(this.status).toHaveText(status);
    const entry = this.history.locator('li').filter({ has: this.page.getByText(status, { exact: true }) });
    await expect(entry).toBeVisible();
    await expect(entry.locator('.log-date')).toHaveText(/\d{2}\/\d{2}\/\d{4}/);
  }

  async goBack() {
    await this.page.getByRole('link', { name: 'Voltar', exact: true }).click();
    await expect(this.page).toHaveURL(/\/professor\/supervisor$/);
  }
}
