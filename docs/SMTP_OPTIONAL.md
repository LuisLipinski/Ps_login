# SMTP opcional temporariamente

Enquanto o My Pet Admin ainda nao possui provedor de e-mail configurado, o PS_Login deve conseguir iniciar em producao sem variaveis `MAIL_*`.

Nesta fase:

- login, JWT, refresh, logout e troca autenticada de senha continuam operacionais;
- `/actuator/health` permanece independente do SMTP;
- convite de ativacao e recuperacao/reset por e-mail nao devem ser usados ate um provedor SMTP real ser configurado;
- `ACTIVATION_URL`, `PASSWORD_RESET_URL` e `MAIL_FROM` possuem defaults tecnicos apenas para permitir o startup sem frontend/provedor;
- `MAIL_HOST`, `MAIL_USERNAME` e `MAIL_PASSWORD` deixam de ser requisitos de startup;
- ao habilitar SMTP real, configurar host, porta, credenciais e as flags de autenticacao/STARTTLS exigidas pelo provedor.

Esta e uma decisao temporaria de infraestrutura e nao remove a responsabilidade do PS_Login sobre convite e recuperacao de senha.
