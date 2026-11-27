import { test } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage.js';
import { SecretaryOfferCreatePage } from '../pages/SecretaryOfferCreatePage.js';

test.describe('S.01 - Criação de oferta pelo Secretário', () => {
  test.use({ timezoneId: 'America/Sao_Paulo' });
  let loginPage;
  let offerCreatePage;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
    offerCreatePage = new SecretaryOfferCreatePage(page);
    await loginPage.goto();
    await loginPage.login('secretario', '123456');
    await loginPage.expectLoginSuccessful('/secretaria/ofertas', 'SECRETARIO');
    await offerCreatePage.openFromOffersList();
  });

  test('deve criar uma nova oferta com professor responsável e datas válidas', async () => {
    const offerName = `Engenharia de Software 2 - E2E ${Date.now()}`;

    await offerCreatePage.fillAndSubmit({
      name: offerName,
      semester: '2026/2',
      startDate: '2026-08-01',
      endDate: '2026-12-15',
      professorName: 'André Endo',
    });

    await offerCreatePage.expectOfferCreated(offerName, 'André Endo');
  });

  test('deve mostrar no detalhe as mesmas datas cadastradas na oferta', async () => {
    const offerName = `Oferta Q-3 - E2E ${Date.now()}`;

    await offerCreatePage.fillAndSubmit({
      name: offerName,
      semester: '2026/2',
      startDate: '2026-12-01',
      endDate: '2026-12-02',
      professorName: 'André Endo',
    });

    await offerCreatePage.expectOfferCreated(offerName, 'André Endo');
    await offerCreatePage.expectOfferPeriod('01/12/2026', '02/12/2026');
  });

  test('deve rejeitar criação de oferta quando a data de fim não for posterior à data de início', async () => {
    await offerCreatePage.fillAndSubmit({
      name: 'Oferta Datas Inválidas',
      semester: '2026/2',
      startDate: '2026-10-10',
      endDate: '2026-09-01',
      professorName: 'André Endo',
    });

    await offerCreatePage.expectCreationError('A data de fim deve ser depois da data de início.');
  });
});
