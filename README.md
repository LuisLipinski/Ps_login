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
- Swagger/OpenAPI;
- JaCoCo.

## Integração PS_User
O serviço consome:

`GET /internal/usuarios/identity?email={email}`

A chamada recebe `X-Internal-Key` automaticamente pelo cliente Feign.

## Segurança da fundação
- serviço stateless;
- `httpBasic` e `formLogin` desabilitados;
- `/internal/**` protegido por `X-Internal-Key`;
- health/info/version e Swagger públicos;
- demais rotas negadas até os fluxos de autenticação serem implementados.

## Banco
Banco lógico recomendado: `ps_login_db`.

A migration V1 prepara as tabelas de credencial e tokens de ativação. Senha em texto puro e token de ativação em texto puro nunca devem ser persistidos.

## Produção / Render
Variáveis esperadas:

- `SPRING_PROFILES_ACTIVE=prod`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `PS_USER_URL`
- `INTERNAL_API_KEY`

`PORT` é fornecido pelo Render e possui fallback local para `8084`.

## Estado
P1 — fundação do microsserviço. Fluxos de convite, definição de senha, login/JWT, refresh token e recuperação de senha entram em incrementos separados.
