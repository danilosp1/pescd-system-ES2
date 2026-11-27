export const samplePdfFile = {
  name: 'documento-e2e.pdf',
  mimeType: 'application/pdf',
  buffer: Buffer.from(
    '%PDF-1.4\n% Documento de teste E2E do PESCD\n1 0 obj\n<< /Type /Catalog >>\nendobj\ntrailer\n<< /Root 1 0 R >>\n%%EOF\n',
    'utf-8'
  ),
};
