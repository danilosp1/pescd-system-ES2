import { expect } from '@playwright/test';

export class ResponsibleCloseOfferPage {
  constructor(page) {
    this.page = page;
    this.lessonsInput = page.getByRole('textbox');
    this.submitButton = page.getByRole('button', { name: 'Encerrar oferta', exact: true });
    this.errorAlert = page.locator('.alert-error');
  }

  async expectStatistic(label, value) {
    const term = this.page.locator('dt').filter({ hasText: new RegExp(`^${label}$`) });
    await expect(term.locator('xpath=following-sibling::dd[1]')).toHaveText(value);
  }

  async expectStudentResult(name, source, frequency, grade) {
    const cells = this.page.locator('tbody tr').filter({ hasText: name }).getByRole('cell');
    await expect(cells.nth(1)).toHaveText('Concluído pelo responsável');
    await expect(cells.nth(2)).toHaveText(source);
    await expect(cells.nth(3)).toHaveText(`${frequency}%`);
    await expect(cells.nth(4)).toHaveText(grade);
  }

  async requestClosure(lessons) {
    await this.lessonsInput.fill(lessons);
    await this.submitButton.click();
  }

  async goBack() {
    await this.page.getByRole('link', { name: 'Voltar', exact: true }).click();
    await expect(this.page).toHaveURL(/\/professor\/responsavel$/);
  }
}
