# PS_Login — My Pet Admin

Microsserviço responsável exclusivamente por **credenciais e autenticação** do My Pet Admin.

## Plataforma
- Java 25 LTS
- Spring Boot 4.1.1
- Spring Cloud 2025.1.3
- PostgreSQL + Flyway
- Spring Security / JOSE JWT
- OpenFeign
- Spring Mail
- Swagger/OpenAPI
- JaCoCo

## Ownership
PS_Login é dono de credenciais, autenticação, access/refresh tokens, sessão, logout e fluxos de senha. Dados cadastrais, `empresaId`, status e roles permanecem no PS_User.

## Ativação
- `POST /internal/auth/invitations`
- `POST /auth/activation`

Usuário recebe convite, define a própria senha e nenhum token de ativação é persistido em texto puro.

## Login
`POST /auth/login`

Retorna:
- `accessToken`
- `tokenType=Bearer`
- `expiresIn`
- `refreshToken`
- `refreshExpiresIn`

Usuário precisa estar `ATIVO` no PS_User e a credencial precisa estar `ACTIVE` no PS_Login.

## JWT
HS256 temporário para compatibilidade com o PS_Empresa atual.
Claims: `sub=userId`, `empresaId`, `roles`, `iss`, `iat`, `exp`, `jti`.

`JWT_SECRET_KEY` deve ser Base64 de pelo menos 32 bytes aleatórios. Com Gateway, a direção é migrar para assinatura assimétrica/JWKS.

## Refresh token e sessão
- `POST /auth/refresh` rotaciona o refresh token a cada uso;
- refresh token é opaco e criptograficamente aleatório;
- somente SHA-256 é persistido;
- refresh token pertence a uma `family_id`;
- reutilização de token já consumido revoga toda a família, mitigando replay;
- consulta de refresh revalida o usuário atual no PS_User antes de emitir novo access token;
- status/roles não são reaproveitados cegamente do access token anterior;
- lookup do refresh usa lock pessimista para serializar rotações concorrentes;
- `POST /auth/logout` revoga a família e é idempotente.

TTL técnico do refresh: `JWT_REFRESH_TTL`, default `P30D`. É configurável e não representa decisão definitiva de produto.

## Banco
Banco lógico recomendado: `ps_login_db`.

Migrations:
- V1: credenciais + tokens de ativação;
- V2: refresh tokens e famílias de sessão.

## Produção / Render
Variáveis:
- `SPRING_PROFILES_ACTIVE=prod`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `PS_USER_URL`
- `INTERNAL_API_KEY`
- `JWT_SECRET_KEY`
- `JWT_ISSUER` opcional
- `JWT_ACCESS_TTL` opcional, default `PT15M`
- `JWT_REFRESH_TTL` opcional, default `P30D`
- `ACTIVATION_URL`
- `ACTIVATION_TOKEN_TTL` opcional
- `PASSWORD_MIN_LENGTH` opcional
- `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`

## Próximos incrementos
- troca autenticada de senha;
- recuperação/reset de senha;
- integração/deploy com banco Neon e Render;
- API Gateway + migração futura para JWT assimétrico.
