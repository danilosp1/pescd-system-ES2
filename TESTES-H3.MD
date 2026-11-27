# Testes de unidade de Aluno e Professores

A issue [H-3](https://github.com/danilosp1/pescd-system-ES2/issues/10) pede testes com JUnit 5 e Mockito e pelo menos 80% de cobertura de decisões para os serviços e controllers de Aluno e Professores.

As classes citadas como StudentEnrollmentService e ProfessorSupervisorService na issue correspondem a StudentOfferService e SupervisorProfessorService no código atual.

## Executar

Na raiz do repositório, com JDK 17 ou 21, execute o Maven Wrapper.

```powershell
.\mvnw.cmd -Pcoverage-h3 clean verify
```

No Linux, use o mesmo perfil.

```sh
./mvnw -Pcoverage-h3 clean verify
```

O perfil usa JUnit Jupiter 5.13.4 e Mockito, executa os testes de unidade existentes e os novos e gera o relatório JaCoCo em `target/site/jacoco/index.html`. Os formatos XML e CSV ficam no mesmo diretório. A verificação exige 80% de branch coverage no conjunto das nove classes do escopo H-3.

O teste existente PescdApplicationTests é um teste de contexto Spring que depende da infraestrutura externa. Ele fica excluído deste perfil de unidade, seguindo a seleção já usada pelo CI. Os testes H-3 usam mocks para repositórios e armazenamento e chamadas diretas aos controllers.

## Cenários cobertos

| Classe | Verificações |
| --- | --- |
| StudentOfferService | Identidade do aluno, inscrição ausente, consultas, professores, envio de plano, documentação e relatório, estados da oferta e da inscrição, metadados e histórico, arquivo ausente e vazio, formato PDF, erro de leitura e falha do armazenamento. |
| SupervisorProfessorService | Agrupamento e ordenação do painel, acesso do supervisor, arquivos ausentes, consultas REST, aprovação de plano e relatório, bloqueio de status incorreto do relatório, parecer, frequência, nota e histórico. |
| ProfessorResponsavelService | Painel, acesso do responsável, arquivos e histórico, busca por nome, conclusão do relatório, análise de documentação, status incorreto do relatório, resumo de encerramento, alunos pendentes e encaminhamento ao secretário. |
| StudentOfferController | Modelos, inicialização dos formulários, erros de validação, erros de negócio, submissões, mensagens, redirecionamentos, downloads e resposta 404. |
| StudentOfferApiController | Consultas e submissões com usuário autenticado, downloads, conteúdo e cabeçalhos PDF, resposta 404 e falha de autorização antes da leitura do arquivo. |
| SupervisorProfessorController | Painel, formulários, frequência pré-preenchida, validações, aprovação, redirecionamentos, downloads e usuário ausente. |
| SupervisorProfessorApiController | Consultas, aprovações com formulário e identidade corretos, downloads e usuário ausente. |
| ProfessorResponsavelController | Painel, valores sugeridos do supervisor, filtro por aluno, responsável incorreto, formulários, validação, avaliações finais, encerramento, erros de negócio, downloads e usuário ausente. |
| ProfessorResponsavelApiController | Consultas, filtro por nome, resumo e solicitação de encerramento, avaliações finais, downloads e falhas de acesso. |

## Cobertura após a PR 50

Os testes adicionais verificam os limites de envio do aluno, a preservação dos formulários, a aprovação do plano uma única vez e a busca de alunos sem filtro. Também exercitam as recusas de encerramento e de reavaliação de documentação. A suíte Java passou de 240 para 256 casos.

| Escopo do JaCoCo | Após a integração da PR 50 | Com os testes adicionais |
| --- | --- | --- |
| Projeto Java completo | 284 de 372 ramos (76,34%) | 298 de 372 ramos (80,11%) |
| Serviços e controllers de Aluno e Professores | 95 de 114 ramos (83,33%) | 109 de 114 ramos (95,61%) |

A medição inicial está na [execução da PR 50 na main](https://github.com/danilosp1/pescd-system-ES2/actions/runs/46020842731). Para reproduzir a medição do projeto completo no PowerShell, execute o mesmo conjunto de testes da CI e consulte o contador `BRANCH` na raiz de `target/site/jacoco/jacoco.xml`.

```powershell
.\mvnw.cmd clean test '-Dtest=!PescdApplicationTests' '-Dsurefire.failIfNoSpecifiedTests=false' jacoco:report
```

## Resultado da entrega original

A execução limpa com Java 21, JUnit Jupiter 5.13.4 e Mockito passou com 148 testes de unidade, sendo 88 novos casos do H-3, sem falhas, erros ou testes ignorados. A compilação mantém o alvo Java 17 do projeto.

| Classe | Decisões cobertas | Linhas cobertas |
| --- | --- | --- |
| StudentOfferService | 19 de 22 | 121 de 122 |
| SupervisorProfessorService | 5 de 6 | 70 de 72 |
| ProfessorResponsavelService | 18 de 24 | 117 de 120 |
| StudentOfferController | 16 de 20 | 89 de 89 |
| StudentOfferApiController | 7 de 8 | 30 de 30 |
| SupervisorProfessorController | 3 de 4 | 71 de 77 |
| SupervisorProfessorApiController | Sem decisões condicionais | 36 de 36 |
| ProfessorResponsavelController | 9 de 12 | 123 de 126 |
| ProfessorResponsavelApiController | Sem decisões condicionais | 52 de 52 |

O escopo H-3 atingiu 80,21% de branch coverage, com 77 de 96 decisões. A cobertura conta decisões inteiras, e 76 de 96 corresponderia a 79,17%. Portanto, 77 é a menor quantidade de decisões cobertas que atende à meta global de 80% neste escopo.

A cobertura de linhas ficou em 97,93%, com 709 de 724 linhas. Esses percentuais se referem às nove classes acima. O perfil executa a suíte de unidade inteira e restringe a medição e a meta de cobertura ao escopo desta issue.

## Problemas registrados na entrega original

### Arquivo anterior removido antes da confirmação do novo envio

StudentOfferService chama `storageService.delete` antes de `storageService.store` nos três envios. Se o armazenamento do novo PDF falhar depois da exclusão, o banco continua referenciando o arquivo anterior, que pode ter sido apagado. A transação do banco não reverte a exclusão no SeaweedFS.

O teste `storageFailureDoesNotSaveReportOrAdvanceStatus` reproduz uma falha com mocks e confirma que a exclusão foi solicitada, que o relatório não foi salvo, que o identificador antigo permaneceu e que o status não avançou. Para reproduzir no ambiente, é necessário um envio com arquivo anterior e uma falha entre exclusão e armazenamento. A correção deve coordenar armazenamento, persistência e limpeza do arquivo antigo.

### Aprovações de professor sem verificar o estado da oferta

Por inspeção, `approvePlan`, `approveReport`, `concludeReport` e `analyzeDocumentation` verificam o status da inscrição e a identidade do professor, mas não verificam o estado da oferta. Uma oferta concluída com inscrição ainda em status compatível pode receber alterações, contrariando a RNG-4. O fluxo de aluno possui a verificação de oferta em andamento.

### Formulário de encerramento sem verificar o responsável

Por inspeção, `ProfessorResponsavelController.closeOfferForm` resolve o usuário autenticado e carrega a oferta por ID, mas não compara o responsável antes de renderizar o formulário. Um professor pode tentar abrir `/professor/responsavel/ofertas/{offerId}/encerrar` com o ID de uma oferta alheia. O POST possui verificação no serviço, e a consulta REST usa `getOfferForResponsible`.

Os problemas estão registrados para correção nas tarefas de bugs. Esta entrega implementa os testes e a medição solicitados no H-3.
