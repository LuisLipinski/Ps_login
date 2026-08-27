# PS_Login — My Pet Admin

Microsserviço responsável exclusivamente por **credenciais, autenticação e sessão** do My Pet Admin.

## Estado atual

O backend funcional do PS_Login está concluído no escopo atual, com CI validando Maven/JaCoCo, migrations em PostgreSQL 17 e Docker Java 25.

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

Revogação imediata de access token exigirá uma estratégia futura como token/session version, denylist ou validação centralizada no Gateway.

## Banco

Banco lógico recomendado: `Ps_login_db`, seguindo a convenção dos bancos atuais do projeto.

Migrations:
- V1: `login_credentials` + `activation_tokens`;
- V2: `refresh_tokens` e famílias de sessão;
- V3: `password_reset_tokens`.

Todas as migrations são validadas pelo CI em PostgreSQL 17.

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

## Quality gates

O pipeline do PS_Login valida:
- Java 25;
- `mvn clean verify`;
- JaCoCo mínimo de 90% de linhas e 70% de branches no escopo medido;
- todas as migrations Flyway `V*.sql` em PostgreSQL 17;
- Docker build.

## Próximas etapas

1. criar/validar o banco lógico `Ps_login_db` no Neon;
2. publicar o PS_Login no Render;
3. configurar secrets e URLs de produção;
4. validar integração real PS_Login ↔ PS_User;
5. coordenar `JWT_SECRET_KEY` com os validadores HS256 atuais;
6. integrar o disparo de convite aos fluxos de onboarding e criação de usuário por meio de um componente confiável/orquestrador;
7. evoluir para API Gateway e, futuramente, JWT assimétrico/JWKS.
