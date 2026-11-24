import { expect } from '@playwright/test';

export class PublicOffersPage {
  constructor(page) {
    this.page = page;
    this.heading = page.getByRole('heading', { name: 'Ofertas públicas' });
    this.loginButton = page.getByRole('link', { name: 'Entrar' });
    this.table = page.locator('table');
    this.rows = page.locator('tbody tr');
    this.semesterCells = page.locator('tbody tr td:nth-child(2)');
  }

  async goto() {
    await this.page.goto('/ofertas');
    await expect(this.heading).toBeVisible();
  }

  async expectVisitorModeAndTableHeaders() {
    await expect(this.loginButton).toBeVisible();
    await expect(this.table.locator('th', { hasText: 'Oferta' })).toBeVisible();
    await expect(this.table.locator('th', { hasText: 'Semestre' })).toBeVisible();
    await expect(this.table.locator('th', { hasText: 'Período' })).toBeVisible();
    await expect(this.table.locator('th', { hasText: 'Responsável' })).toBeVisible();
    await expect(this.table.locator('th', { hasText: 'Inscritos' })).toBeVisible();
  }

  async expectOfferVisible(name, semester, responsibleProfessor) {
    const row = this.rows.filter({ hasText: name }).first();
    await expect(row).toBeVisible();
    await expect(row).toContainText(semester);
    await expect(row).toContainText(responsibleProfessor);
  }

  async expectOrderedBySemesterDescending() {
    await expect(this.rows.first()).toBeVisible();
    const semesters = await this.semesterCells.allInnerTexts();
    expect(semesters.length).toBeGreaterThan(1);
    const sorted = [...semesters].sort((a, b) => b.localeCompare(a));
    expect(semesters).toEqual(sorted);
  }
}
