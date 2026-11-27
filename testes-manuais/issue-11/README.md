# Testes manuais do PESCD

[Issue #11](https://github.com/danilosp1/pescd-system-ES2/issues/11) · 27/11/2026 · Commit testado `c3b6b1f`

**Resultado:** 26 casos executados pela interface. 23 aprovados, 2 com defeitos reproduzidos e 1 inconclusivo.

A sessão usou o frontend React e o backend Spring Boot locais, com PostgreSQL e SeaweedFS no ambiente de testes. As ações foram executadas no Chromium. A oferta **Sessão manual J - 27-11** e o cadastro **Aluno Manual J** foram criados durante a sessão para acompanhar as mudanças de estado. A análise de documentação usou uma inscrição de teste disponível.

Os critérios vieram de `REQUISITOS.md`. A sessão aplicou partições válidas e inválidas para login e anexos, valores-limite para datas e frequência, transições de estado para aprovações e encerramento e exploração de duplicidade, busca, cancelamento e saída da sessão.

| Caso | Critério e ação | Resultado observado | Evidência |
| --- | --- | --- | --- |
| J01 | **V.01**. Como visitante, abrir a consulta de ofertas. | **Aprovado.** A tabela abre sem login e mostra oferta, semestre, período, responsável e inscritos. | [Ofertas públicas](evidencias/J01-ofertas-publicas.jpg) |
| J02 | **U.01**. Tentar entrar como secretário com senha incorreta e repetir com a senha de teste válida. | **Aprovado.** A primeira tentativa exibe erro e permanece no login. A segunda abre o painel da secretaria. | [Senha incorreta](evidencias/J02-login-invalido.jpg), [painel da secretaria](evidencias/J05-b-datas-exibidas.jpg) |
| J03 | **S.01 RN-2**. Informar início em 01/12 e fim em 30/11. | **Aprovado.** O formulário informa que o fim deve ser posterior ao início. | [Fim anterior](evidencias/J03-data-fim-anterior.jpg) |
| J04 | **S.01 RN-2**. Informar início e fim em 01/12. | **Aprovado.** Datas iguais são rejeitadas com a mesma mensagem. | [Datas iguais](evidencias/J04-datas-iguais.jpg) |
| J05 | **S.01 e S.03**. Ajustar o fim para 02/12, um dia depois do início, e salvar. Conferir as datas no detalhe. | **Defeito.** A oferta é criada, mas o período exibido é 30/11/2026 a 01/12/2026, um dia antes em cada ponta. | [Entrada](evidencias/J05-a-datas-informadas.jpg), [detalhe após salvar](evidencias/J05-b-datas-exibidas.jpg) |
| J06 | **S.02**. Inscrever um aluno novo com nome, e-mail e RA de teste. | **Aprovado.** O aluno aparece na lista com status **Não enviado** e o total passa de zero para um. | [Aluno inscrito](evidencias/J06-aluno-adicionado.jpg) |
| J07 | **S.02**. Repetir a inscrição com o mesmo e-mail na mesma oferta. | **Aprovado.** A mensagem informa que o aluno já está inscrito e o total permanece um. | [Duplicidade](evidencias/J07-inscricao-duplicada.jpg) |
| J08 | **U.01 e AL.01**. Entrar com a conta do aluno criado e abrir a oferta em **Minhas ofertas**. | **Aprovado.** A inscrição abre com status **Não enviado** e oferece as opções de plano e documentação. | [Inscrição do aluno](evidencias/J08-aluno-nova-inscricao.jpg) |
| J09 | **AL.02 RN-3**. Anexar um CSV em vez de PDF e enviar o plano. | **Inconclusivo.** A interface mostrou `Failed to fetch` e permaneceu no formulário. A captura registra a mensagem, mas a tentativa não confirmou a validação de formato. | [Tentativa com CSV](evidencias/J09-envio-csv-no-plano.jpg) |
| J10 | **AL.02**. Substituir pelo PDF de teste e enviar o plano. | **Aprovado.** **Plano enviado**, nome do PDF, supervisor e transição aparecem na inscrição. | [Plano enviado](evidencias/J10-plano-pdf-enviado.jpg) |
| J11 | **PS.02 RN-3**. Abrir o plano como supervisor e tentar aprovar com parecer vazio. | **Aprovado.** O campo obrigatório impede a ação. O plano permanece enviado. | [Parecer obrigatório](evidencias/J11-parecer-obrigatorio.jpg) |
| J12 | **PS.02 RN-4**. Informar parecer, aprovar e recarregar. | **Aprovado.** A transição para **Plano aprovado** aparece no histórico e se mantém após recarregar. | [Plano aprovado](evidencias/J12-plano-aprovado.jpg) |
| J13 | **U.01 e PR.04**. Entrar como professor, alternar para **Responsável**, abrir a oferta e buscar `ALUNO MANUAL`. | **Aprovado.** O menu mostra os dois papéis e a busca retorna o aluno independentemente da caixa. | [Papéis](evidencias/J13a-professor-dois-papeis.jpg), [resultado](evidencias/J13b-filtro-nome.jpg) |
| J14 | **PR.04**. Buscar um nome inexistente e depois apagar o termo e buscar novamente. | **Aprovado.** A tela informa que nenhum aluno foi encontrado. A busca vazia restaura a lista. | [Busca sem resultados](evidencias/J14-filtro-sem-resultados.jpg), [linha do aluno](evidencias/J13b-filtro-nome.jpg) |
| J15 | **PR.03 PC-2**. Tentar encaminhar a oferta ao secretário com o aluno ainda em **Plano aprovado**. | **Aprovado.** O envio é recusado com mensagem sobre alunos pendentes. | [Bloqueio por pendência](evidencias/J15-encerramento-com-pendencia.jpg) |
| J16 | **AL.04**. Informar frequência 101 com PDF selecionado e tentar enviar. | **Aprovado.** O campo tem máximo 100, indica valor acima do limite e impede a submissão. | [Frequência 101](evidencias/J16-frequencia-101.jpg) |
| J17 | **AL.04**. Ajustar a frequência exatamente para 100 e enviar o relatório. | **Aprovado.** A frequência de 100% é aceita e **Relatório enviado** aparece no histórico. | [Relatório enviado](evidencias/J17-relatorio-100.jpg) |
| J18 | **PS.03 RN-3**. Abrir o relatório de frequência 100% como supervisor e conferir o campo inicial. | **Defeito.** O resumo mostra 100%, mas **Frequência confirmada (%)** aparece vazia. | [Frequência vazia](evidencias/J18-frequencia-vazia-supervisor.jpg) |
| J19 | **PS.03 RN-4**. Digitar frequência 100, informar parecer e nota A e aprovar. | **Aprovado.** O sistema confirma e registra **Relatório aprovado pelo supervisor**. | [Relatório aprovado](evidencias/J19-relatorio-aprovado.jpg) |
| J20 | **PR.01**. Conferir a avaliação do supervisor e concluir com frequência 100, parecer e nota A. | **Aprovado.** A confirmação e o status **Concluído pelo responsável** aparecem no histórico. | [Relatório concluído](evidencias/J20-relatorio-concluido.jpg) |
| J21 | **PR.03**. Conferir o resumo de encerramento, informar lições aprendidas e encaminhar. | **Aprovado.** O resumo mostra uma conclusão por relatório, média de 100% e uma nota A. A oferta passa a **Aguardando encerramento do secretário**. | [Resumo](evidencias/J21-resumo-de-encerramento.jpg), [estado após encaminhar](evidencias/J22-a-confirmacao-secretaria.jpg) |
| J22 | **S.04**. Abrir a confirmação e selecionar **Cancelar**. | **Aprovado.** A confirmação tem instruções. O cancelamento fecha o painel e mantém a oferta aguardando. | [Confirmação](evidencias/J22-a-confirmacao-secretaria.jpg), [cancelamento](evidencias/J22-b-cancelamento-secretaria.jpg) |
| J23 | **S.04**. Reabrir a confirmação e confirmar o encerramento da oferta de teste. | **Aprovado.** O sistema confirma o encerramento e exibe **Concluída**. | [Oferta concluída](evidencias/J23-oferta-concluida.jpg) |
| J24 | **AD.01**. Editar o nome do aluno de teste, cancelar e reabrir o formulário. | **Aprovado.** A edição descartada dá lugar ao nome original ao reabrir. | [Rascunho](evidencias/J24-a-edicao-admin.jpg), [formulário reaberto](evidencias/J24-b-edicao-reaberta.jpg) |
| J25 | **U.01**. Sair da área de administração e usar o botão Voltar do navegador. | **Aprovado.** O navegador apresenta a tela de login e a área administrativa não reaparece. | [Login após sair](evidencias/J25-sessao-encerrada.jpg) |
| J26 | **PR.02**. Abrir documentação pendente, tentar concluir sem nota, selecionar B e enviar. | **Aprovado.** Nota vazia impede a submissão. Com nota válida, o sistema confirma a análise e registra a conclusão. | [Nota obrigatória](evidencias/J26-a-documentacao-sem-nota.jpg), [documentação concluída](evidencias/J26-b-documentacao-concluida.jpg) |

## Defeitos reproduzidos

**J05 — Datas exibidas um dia antes.** No formulário da secretaria, criar oferta com início em 01/12/2026 e fim em 02/12/2026. Após salvar, o detalhe mostra 30/11/2026 e 01/12/2026. A mensagem que aparece na captura de entrada vem da tentativa anterior com datas iguais. O reenvio com o dia seguinte foi aceito. Por inspeção, a formatação do frontend passa a data simples para `new Date` antes de formatá-la no horário local. Essa é uma hipótese para a diferença observada.

**J18 — Frequência confirmada vazia.** Enviar um relatório com frequência 100% depois da aprovação do plano e abrir **Aprovar relatório** como supervisor. O resumo mostra 100%, mas o campo editável abre vazio. O esperado em PS.03 RN-3 é iniciar com a frequência informada pelo aluno. O supervisor conseguiu aprovar depois de preencher 100 manualmente.

Os dois relatos foram registrados no GitHub com os passos e as capturas. [J05 — Issue #53](https://github.com/danilosp1/pescd-system-ES2/issues/53) · [J18 — Issue #54](https://github.com/danilosp1/pescd-system-ES2/issues/54)
