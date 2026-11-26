import { expect } from '@playwright/test';

export class SecretaryOfferDetailsPage {
  constructor(page) {
    this.page = page;
    this.closeOfferButton = page.getByRole('button', { name: 'Encerrar oferta' });
    this.confirmCloseButton = page.getByRole('button', { name: 'Confirmar encerramento' });
    this.cancelCloseButton = page.getByRole('button', { name: 'Cancelar' });
    this.confirmationCard = page.getByTestId('close-offer-confirmation');
    this.successAlert = page.locator('.alert-success');
    this.statusBadge = page.locator('.card').first().locator('.badge');
  }

  async openOfferByName(offerName) {
    const row = this.page.locator('tbody tr').filter({ hasText: offerName }).first();
    await row.getByRole('link', { name: 'Detalhes' }).click();
    await expect(this.page.getByRole('heading', { name: offerName })).toBeVisible();
  }

  async expectCloseButtonVisible() {
    await expect(this.closeOfferButton).toBeVisible();
  }

  async expectCloseButtonNotVisible() {
    await expect(this.closeOfferButton).toHaveCount(0);
  }

  async openCloseConfirmation() {
    await this.closeOfferButton.click();
    await expect(this.confirmationCard).toBeVisible();
  }

  async expectInstructions() {
    await expect(this.confirmationCard).toContainText(
      'Confira os dados da oferta antes de confirmar o encerramento.'
    );
  }

  async cancelClose() {
    await this.cancelCloseButton.click();
    await expect(this.confirmationCard).toHaveCount(0);
  }

  async confirmClose() {
    await this.confirmCloseButton.click();
    await expect(this.confirmationCard).toHaveCount(0);
  }

  async expectOfferStatus(expectedStatus) {
    await expect(this.statusBadge).toHaveText(expectedStatus);
  }

  async expectSuccessMessage(expectedText) {
    await expect(this.successAlert).toBeVisible();
    await expect(this.successAlert).toContainText(expectedText);
  }
}
