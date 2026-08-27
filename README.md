# PS_Login — My Pet Admin

Microsserviço responsável exclusivamente por **credenciais e autenticação** do My Pet Admin.

## Ownership

### PS_Login
- credencial e password hash;
- autenticação;
- emissão de JWT;
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
- Spring Security JOSE/JWT;
- OpenFeign;
- SMTP via Spring Mail;
- Swagger/OpenAPI;
- JaCoCo.

## Integração PS_User
O serviço consome `GET /internal/usuarios/identity?email={email}` com `X-Internal-Key` para validar identidade, status e roles.

## Fluxo de ativação
1. PS_User cria identidade sem senha.
2. Orquestração chama `POST /internal/auth/invitations`.
3. PS_Login valida a identidade no PS_User.
4. Gera token seguro e persiste somente SHA-256.
5. Usuário recebe link por e-mail e define a própria senha em `POST /auth/activation`.
6. Password hash fica exclusivamente no PS_Login.

## Login
Endpoint: `POST /auth/login`.

Fluxo:
1. recebe e-mail + senha;
2. consulta identidade no PS_User;
3. exige usuário `ATIVO`;
4. exige credencial local `ACTIVE`;
5. valida password hash;
6. emite access token JWT curto.

Falha de usuário inexistente, usuário inativo, credencial pendente ou senha incorreta responde de forma neutra como credenciais inválidas.

## JWT atual
Nesta fase, para compatibilidade com a validação já existente do PS_Empresa, o access token usa HS256.

Claims:
- `sub = userId`;
- `empresaId`;
- `roles`;
- `iss`;
- `iat`;
- `exp`;
- `jti`.

`JWT_SECRET_KEY` deve ser **Base64 de pelo menos 32 bytes aleatórios** e precisa ser o mesmo no PS_Login e nos serviços que ainda validam HS256 diretamente. Esta compatibilidade é transitória: quando o API Gateway centralizar autenticação, a direção é migrar assinatura para chave assimétrica/JWKS e parar de compartilhar segredo de assinatura entre serviços.

TTL do access token: `JWT_ACCESS_TTL`, default técnico `PT15M`.

## Política de senha
O backend valida confirmação e tamanho. O mínimo é parametrizado por `PASSWORD_MIN_LENGTH` (default técnico atual: 12) e não representa decisão definitiva de produto.

## Endpoints atuais
- `POST /internal/auth/invitations` — protegido por `X-Internal-Key`;
- `POST /auth/activation` — público com token de uso único;
- `POST /auth/login` — público;
- `GET /version`;
- `/actuator/health` e `/actuator/info`.

## Segurança
- stateless;
- `httpBasic`, `formLogin` e logout padrão desabilitados;
- `/internal/**` protegido por chave interna;
- token de ativação não é persistido em texto puro nem logado;
- respostas de autenticação inválida não revelam existência/status do usuário;
- comparação de senha usa `PasswordEncoder`;
- rate limiting permanece requisito do Gateway, com possibilidade de defesa complementar local.

## Banco
Banco lógico recomendado: `ps_login_db`.

Migration V1:
- `login_credentials`;
- `activation_tokens`.

## Produção / Render
Variáveis esperadas:
- `SPRING_PROFILES_ACTIVE=prod`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `PS_USER_URL`
- `INTERNAL_API_KEY`
- `JWT_SECRET_KEY`
- `JWT_ISSUER` (opcional, default `ps-login`)
- `JWT_ACCESS_TTL` (opcional, default `PT15M`)
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
- refresh token com rotação/revogação;
- logout/revogação de sessão;
- troca autenticada de senha;
- recuperação/reset de senha;
- integração futura com API Gateway e migração para assinatura assimétrica.
