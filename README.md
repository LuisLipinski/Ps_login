# PS_Login — My Pet Admin

Microsserviço responsável exclusivamente por **credenciais, autenticação e sessão** do My Pet Admin.

## Estado atual

O backend funcional do PS_Login está concluído no escopo atual.

Entregas principais:
- convite e ativação de credencial;
- login por e-mail e senha;
- access token JWT;
- refresh token com rotação e detecção de reutilização;
- logout/revogação de sessão;
- troca autenticada de senha;
- recuperação e redefinição de senha.

## Plataforma
- Java 25 LTS
- Spring Boot 4.1.1
- Spring Cloud 2025.1.3
- PostgreSQL + Flyway
- Spring Security / OAuth2 Resource Server / JOSE JWT
- OpenFeign
- Spring Mail
- Swagger/OpenAPI
- JaCoCo

## Ownership

### PS_Login
É dono de:
- password hash;
- autenticação;
- access token JWT;
- refresh token e sessão;
- logout/revogação;
- convite de ativação;
- troca de senha;
- recuperação/reset de senha;
- tokens temporários relacionados à credencial.

### PS_User
Permanece fonte de verdade para:
- `userId`;
- `empresaId`;
- e-mail cadastral;
- status do usuário;
- roles/perfis.

O PS_Login consulta o PS_User por APIs internas protegidas por `X-Internal-Key` e não replica identidade de negócio.

## Ativação e definição da própria senha

Endpoints:
- `POST /internal/auth/invitations`
- `POST /auth/activation`

Fluxo:
1. PS_User cria a identidade de negócio;
2. um componente confiável solicita o convite ao PS_Login;
3. PS_Login gera token criptograficamente seguro;
4. somente SHA-256 do token é persistido;
5. usuário recebe o link por e-mail;
6. usuário define a própria senha;
7. credencial passa para `ACTIVE`.

MASTER/ADMIN nunca define nem visualiza a senha de outro usuário.

## Login

Endpoint:
- `POST /auth/login`

Requisitos:
- usuário deve estar `ATIVO` no PS_User;
- credencial deve estar `ACTIVE` no PS_Login;
- senha deve corresponder ao hash persistido.

A resposta de credenciais inválidas é neutra para não revelar existência, status ou estado da credencial.

Resposta de sucesso:
- `accessToken`
- `tokenType=Bearer`
- `expiresIn`
- `refreshToken`
- `refreshExpiresIn`

## Access token JWT

HS256 é usado temporariamente para compatibilidade com os serviços atuais.

Claims:
- `sub=userId`
- `empresaId`
- `roles`
- `iss`
- `iat`
- `exp`
- `jti`

`JWT_SECRET_KEY` deve ser Base64 representando pelo menos 32 bytes aleatórios. Enquanto microsserviços validarem HS256 diretamente, o valor precisa ser coordenado entre PS_Login e validadores, especialmente PS_Empresa.

Direção futura: API Gateway + assinatura assimétrica/JWKS, eliminando segredo de assinatura compartilhado entre microsserviços.

## Refresh token e sessão

Endpoints:
- `POST /auth/refresh`
- `POST /auth/logout`

Regras:
- refresh token é opaco e criptograficamente aleatório;
- somente SHA-256 é persistido;
- rotação obrigatória a cada uso;
- tokens pertencem a uma `family_id`;
- reutilização de token já consumido revoga toda a família;
- lookup usa lock pessimista para serializar rotações concorrentes;
- refresh revalida status, tenant e roles atuais no PS_User;
- logout revoga a família e é idempotente.

TTL técnico do refresh: `JWT_REFRESH_TTL`, default `P30D`. É configurável e não representa decisão definitiva de produto.

## Gerenciamento de senha

Endpoints:
- `POST /auth/password/forgot` — público, resposta neutra `202 Accepted`;
- `POST /auth/password/reset` — público, token de redefinição de uso único;
- `POST /auth/password/change` — exige Bearer JWT válido e senha atual.

Regras:
- token de reset é aleatório e somente SHA-256 é persistido;
- tokens de reset anteriores são revogados ao solicitar um novo;
- usuário é revalidado no PS_User antes de troca/reset;
- troca e reset revogam todos os refresh tokens ativos da credencial;
- política de tamanho de senha é parametrizada por `PASSWORD_MIN_LENGTH`.

### Limitação atual de revogação

Access token é JWT stateless. Portanto, após troca/reset de senha, access tokens já emitidos permanecem válidos até o TTL curto expirar. Refresh sessions são revogadas imediatamente.

Revogação imediata de access token exigirá estratégia futura no API Gateway/token version/denylist. Não é bloqueio para o fechamento atual do PS_Login.

## Banco

Banco lógico de produção esperado: `Ps_login_db`.

Migrations:
- V1: `login_credentials` + `activation_tokens`;
- V2: `refresh_tokens` e famílias de sessão;
- V3: `password_reset_tokens`;
- V4: alinha os tipos dos hashes SHA-256 de ativação, refresh e reset ao mapeamento Hibernate 7, preservando compatibilidade para bancos que já tenham recebido V1–V3.

Todas as migrations são validadas pelo CI em PostgreSQL 17 e o startup cross-service executa com `ddl-auto=validate`.

## Produção / Render

Variáveis obrigatórias:
- `SPRING_PROFILES_ACTIVE=prod`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `PS_USER_URL`
- `INTERNAL_API_KEY`
- `JWT_SECRET_KEY`
- `ACTIVATION_URL`
- `PASSWORD_RESET_URL`
- `MAIL_HOST`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `MAIL_FROM`

Variáveis opcionais/com default técnico:
- `MAIL_PORT=587`
- `JWT_ISSUER=ps-login`
- `JWT_ACCESS_TTL=PT15M`
- `JWT_REFRESH_TTL=P30D`
- `ACTIVATION_TOKEN_TTL=PT24H`
- `PASSWORD_RESET_TOKEN_TTL=PT30M`
- `PASSWORD_MIN_LENGTH=12`

`PORT` não precisa ser definido manualmente no Render; a plataforma injeta o valor.

### Health e SMTP

`/actuator/health` não depende do `MailHealthIndicator` (`management.health.mail.enabled=false`). A indisponibilidade do SMTP não deve tirar login/refresh/logout do ar nem provocar restart do serviço inteiro.

Falhas de envio continuam sendo falhas funcionais dos fluxos de convite/reset e devem ser observadas por logs/métricas. O SMTP real ainda é obrigatório para esses fluxos em produção.

## Quality gates

O pipeline `.github/workflows/ci.yml` valida:
- Java 25;
- `mvn clean verify`;
- JaCoCo mínimo de 90% de linhas e 70% de branches no escopo medido;
- todas as migrations Flyway `V*.sql` em PostgreSQL 17;
- Docker build;
- integração efêmera PS_Login ↔ PS_User usando PostgreSQL real;
- startup dos dois serviços com Flyway + Hibernate `ddl-auto=validate`;
- contrato interno de identidade;
- login real por e-mail/senha;
- claims `sub`, `empresaId` e `roles` do JWT;
- rejeição de senha inválida;
- rotação do refresh token;
- logout e rejeição de replay;
- bloqueio de login/refresh quando PS_User altera o usuário para `INATIVO`.

Logs dos dois serviços são publicados como artifact em toda execução da integração.

## Release de produção

O workflow `.github/workflows/release-pipeline.yml` é **manual** (`workflow_dispatch`) para não tentar deploy enquanto a infraestrutura de produção não estiver configurada.

Após configurar o Render, a execução na `master`:
1. roda Maven verify e Docker build;
2. exige o secret GitHub `RENDER_DEPLOY_HOOK`;
3. recebe a URL pública do PS_Login como input `service_url`;
4. dispara o deploy no Render;
5. aguarda `/version` devolver exatamente o commit liberado;
6. valida `/actuator/health` como `UP`;
7. cria tag `ps-login-v1.<run_number>` somente após deploy saudável.

## Pendências externas para produção

Estas ações dependem de infraestrutura/credenciais e não são resolvidas apenas no código:

1. criar ou confirmar o banco `Ps_login_db` no Neon;
2. apontar `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` para esse banco;
3. criar o Web Service do PS_Login no Render conectado a este repositório;
4. configurar todas as variáveis de produção listadas acima;
5. configurar SMTP real;
6. configurar no GitHub o secret `RENDER_DEPLOY_HOOK` do serviço;
7. coordenar o mesmo `JWT_SECRET_KEY` Base64 com os validadores HS256 atuais antes do primeiro login em produção;
8. executar o workflow manual **PS_Login Release** informando a URL pública do serviço;
9. realizar um smoke funcional em produção com uma identidade de teste real.

## Próximo domínio após produção

O disparo de convite não será acoplado transacionalmente ao PS_User. O próximo componente de backend é o **Onboarding/Gestão Orchestrator**, que coordenará PS_Empresa, PS_User, PS_Contrato e PS_Login. Depois dele entra o API Gateway.

O frontend permanece fora do escopo até Orchestrator e Gateway estarem concluídos.
