# PS_Login — My Pet Admin

Microsserviço responsável exclusivamente por **credenciais e autenticação** do My Pet Admin.

## Ownership

### PS_Login
- credencial e password hash;
- autenticação;
- emissão futura de JWT;
- convite de ativação e tokens temporários;
- refresh/revogação quando o modelo de sessão for definido;
- alteração e recuperação de senha.

### Fora do PS_Login
Dados cadastrais, `empresaId`, status e roles pertencem ao **PS_User**.

## Plataforma
- Java 25 LTS;
- Spring Boot 4.1.1;
- Spring Cloud 2025.1.3;
- PostgreSQL;
- Flyway;
- Spring Security;
- OpenFeign;
- SMTP via Spring Mail;
- Swagger/OpenAPI;
- JaCoCo.

## Integração PS_User
O serviço consome `GET /internal/usuarios/identity?email={email}` com `X-Internal-Key` para validar a identidade antes de provisionar credencial.

## Fluxo de ativação
1. PS_User cria a identidade sem senha.
2. Orquestração chama `POST /internal/auth/invitations` com `userId` e e-mail.
3. PS_Login valida a identidade no PS_User.
4. PS_Login gera token criptograficamente seguro e persiste somente SHA-256 do token.
5. Convites anteriores ainda válidos são revogados.
6. O usuário recebe por e-mail o link de ativação.
7. O usuário envia token + nova senha para `POST /auth/activation`.
8. A senha é armazenada somente como hash e a credencial passa a `ACTIVE`.
9. O token é marcado como utilizado e não pode ser reutilizado.

MASTER/ADMIN não define nem visualiza senha de outro usuário.

## Política de senha
O backend valida confirmação e tamanho. O mínimo é parametrizado por `PASSWORD_MIN_LENGTH` (default técnico atual: 12) e não representa decisão definitiva de produto. A política pode evoluir sem voltar a levar senha ao PS_User.

## Endpoints atuais
- `POST /internal/auth/invitations` — protegido por `X-Internal-Key`;
- `POST /auth/activation` — público, exige token de uso único;
- `GET /version`;
- `/actuator/health` e `/actuator/info`.

## Segurança
- stateless;
- `httpBasic`, `formLogin` e logout padrão desabilitados;
- `/internal/**` protegido por chave interna;
- token de ativação não é persistido em texto puro nem deve ser logado;
- demais rotas continuam negadas até entrarem login/JWT.

## Banco
Banco lógico recomendado: `ps_login_db`.

Migration V1:
- `login_credentials`;
- `activation_tokens`.

## Produção / Render
Variáveis esperadas nesta etapa:
- `SPRING_PROFILES_ACTIVE=prod`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `PS_USER_URL`
- `INTERNAL_API_KEY`
- `ACTIVATION_URL`
- `ACTIVATION_TOKEN_TTL` (opcional, default `PT24H`)
- `PASSWORD_MIN_LENGTH` (opcional, default técnico `12`)
- `MAIL_HOST`
- `MAIL_PORT`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `MAIL_FROM`

`PORT` é fornecido pelo Render e possui fallback `8084`.

## Próximos incrementos
- `POST /auth/login`;
- emissão JWT com `sub=userId`, `empresaId`, `roles`;
- bloqueio de login para usuário INATIVO ou credencial não ativada;
- depois: refresh token/rotação/revogação e recuperação de senha.
