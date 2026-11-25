import { test } from '@playwright/test';
import { PublicOffersPage } from '../pages/PublicOffersPage.js';

test.describe('V.01 - Visualização pública de ofertas pelo Visitante', () => {
  let publicOffersPage;

  test.beforeEach(async ({ page }) => {
    publicOffersPage = new PublicOffersPage(page);
    await publicOffersPage.goto();
  });

  test('deve exibir a lista de ofertas para visitante não autenticado com todas as colunas exigidas', async () => {
    await publicOffersPage.expectVisitorModeAndTableHeaders();
    await publicOffersPage.expectOfferVisible(
      'Algoritmo e Estrutura de Dados I',
      '2026/1',
      'Alexandre Levada'
    );
  });

  test('deve apresentar a lista de ofertas ordenada pelo semestre de forma decrescente', async () => {
    await publicOffersPage.expectOrderedBySemesterDescending();
  });
});
