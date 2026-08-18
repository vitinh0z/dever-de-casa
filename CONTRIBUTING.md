# Contribuindo com o dever de casa

Obrigado pelo interesse em contribuir. Este documento explica como o projeto está organizado e o fluxo esperado para propor mudanças.

## Antes de começar

Dê uma olhada nas issues abertas antes de sair codando. As issues estão organizadas em épicos (features grandes, como "Camada de integração com a API da Câmara") e cada épico tem sub-issues com o trabalho concreto e menor. Comece por uma sub-issue — elas já vêm com escopo definido. Se quiser trabalhar em algo que ainda não tem issue, abra uma antes de começar, pra evitar esforço duplicado ou trabalho que não se encaixa na direção do projeto.

## Fluxo de branches

- `main` é a branch estável. Só recebe merge vindo de `development`, e só quando o que está lá já foi validado.
- `development` é a branch de integração. Todo trabalho novo parte dela.
- Pra cada issue, crie uma branch a partir de `development` seguindo o padrão `tipo/numero-da-issue-descricao-curta`, por exemplo `feature/12-entidades-jpa` ou `fix/20-busca-por-nome`.

Abra o Pull Request sempre contra `development`, nunca direto contra `main`.

## Commits

Mensagens de commit devem ser claras sobre o porquê da mudança, não só o quê. Prefira commits pequenos e coesos a um commit gigante que mistura várias mudanças não relacionadas.

## Pull Requests

- Referencie a issue relacionada na descrição do PR (`Closes #12`).
- Descreva o que foi feito e, se relevante, como testar.
- PRs que alteram a camada de domínio ou o schema do banco devem explicar o impacto na modelagem existente.
- Espere a revisão antes de mergear, mesmo em projeto pessoal — é um hábito que vale manter.

## Padrões de código

- Siga a separação de camadas já estabelecida no projeto: `web` (controllers), `dto`, `service`, `mapper`, `domain` (entidades), `repository` e `integracao` (clientes de APIs externas). Controllers não acessam repositórios diretamente, e entidades JPA não vazam pra fora da camada de serviço — quem cruza essa fronteira é sempre um DTO.
- Nomeie classes e variáveis em português, já que o domínio do problema (parlamentar, proposição, votação) é todo em português — mantém o código legível pra quem conhece o domínio.
- Novas integrações com APIs externas devem ficar isoladas em `integracao`, com seu próprio cliente e seus próprios DTOs de resposta bruta, sem misturar com o modelo de domínio interno.

## Dúvidas

Se não tiver certeza sobre o escopo de uma issue ou sobre uma decisão de arquitetura, abra uma discussão na própria issue antes de implementar.
