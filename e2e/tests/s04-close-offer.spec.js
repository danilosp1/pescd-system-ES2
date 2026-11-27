import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage.js';
import { SecretaryOfferDetailsPage } from '../pages/SecretaryOfferDetailsPage.js';

test.describe('S.04 - Encerramento de oferta pelo Secretário', () => {
  let loginPage;
  let offerDetailsPage;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
    offerDetailsPage = new SecretaryOfferDetailsPage(page);
    await loginPage.goto();
    await loginPage.login('secretario', '123456');
    await loginPage.expectLoginSuccessful('/secretaria/ofertas', 'SECRETARIO');
  });

  test('deve ocultar a ação em oferta em andamento e exigir confirmação na oferta aguardando encerramento', async ({ page }) => {
    await offerDetailsPage.openOfferByName('Algoritmo e Estrutura de Dados I');
    await offerDetailsPage.expectCloseButtonNotVisible();

    await page.getByRole('link', { name: 'Voltar' }).click();
    await expect(page.getByRole('heading', { name: 'Ofertas' })).toBeVisible();

    await offerDetailsPage.openOfferByName('Oferta em encerramento (E2E)');
    await offerDetailsPage.expectCloseButtonVisible();

    await offerDetailsPage.openCloseConfirmation();
    await offerDetailsPage.expectInstructions();
    await offerDetailsPage.cancelClose();
    await offerDetailsPage.expectCloseButtonVisible();

    await offerDetailsPage.openCloseConfirmation();
    await offerDetailsPage.confirmClose();

    await offerDetailsPage.expectOfferStatus('Concluída');
    await offerDetailsPage.expectSuccessMessage('Oferta encerrada com sucesso.');
  });
});
