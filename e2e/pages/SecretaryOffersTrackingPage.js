import { expect } from '@playwright/test';

export class SecretaryOffersTrackingPage {
  constructor(page) {
    this.page = page;
    this.offersHeading = page.getByRole('heading', { name: 'Ofertas' });
    this.tableRows = page.locator('tbody tr');
    this.studentDetailsHeading = page.getByRole('heading', { name: 'Detalhes do aluno' });
    this.studentInfoHeading = page.getByRole('heading', { name: 'Informações do aluno' });
    this.statusHistoryHeading = page.getByRole('heading', { name: 'Histórico de status' });
  }

  async expectOffersListWithStatusAndEnrolledCount(offerName, expectedSemester) {
    await expect(this.offersHeading).toBeVisible();
    const row = this.tableRows.filter({ hasText: offerName }).first();
    await expect(row).toBeVisible();
    await expect(row).toContainText(expectedSemester);
    await expect(row.locator('.badge')).toBeVisible();
    await expect(row.getByRole('link', { name: 'Detalhes' })).toBeVisible();
  }

  async openOfferDetails(offerName) {
    const row = this.tableRows.filter({ hasText: offerName }).first();
    await row.getByRole('link', { name: 'Detalhes' }).click();
    await expect(this.page.getByRole('heading', { name: offerName })).toBeVisible();
  }

  async expectEnrolledStudentsWithStatusVisible(studentName, expectedStatusLabel) {
    const row = this.tableRows.filter({ hasText: studentName }).first();
    await expect(row).toBeVisible();
    await expect(row.locator('.badge')).toContainText(expectedStatusLabel);
    await expect(row.getByRole('link', { name: 'Ver detalhes' })).toBeVisible();
  }

  async openStudentDetails(studentName) {
    const row = this.tableRows.filter({ hasText: studentName }).first();
    await row.getByRole('link', { name: 'Ver detalhes' }).click();
    await expect(this.studentDetailsHeading).toBeVisible();
  }

  async expectStudentDetailsAndHistoryVisible(studentName, studentEmail) {
    await expect(this.studentInfoHeading).toBeVisible();
    await expect(this.statusHistoryHeading).toBeVisible();
    await expect(this.page.locator('.detail-grid')).toContainText(studentName);
    await expect(this.page.locator('.detail-grid')).toContainText(studentEmail);
  }
}
