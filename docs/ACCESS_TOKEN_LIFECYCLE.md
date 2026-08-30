# Access token lifecycle

## Decisao atual

O My Pet Admin mantem access tokens JWT stateless e refresh tokens stateful com rotacao e deteccao de reuse.

- access token: TTL padrao de 5 minutos;
- refresh token: TTL padrao de 30 dias;
- refresh token usado e rotacionado;
- reuse de refresh token revoga a familia;
- logout revoga a familia de refresh tokens;
- troca/reset de senha revoga todas as sessoes de refresh da credencial.

## Consequencia de seguranca

Como o Gateway valida o JWT localmente, um access token ja emitido nao e revogado imediatamente por logout ou troca de senha. A janela residual fica limitada ao TTL do access token, atualmente 5 minutos.

Esse risco e aceito nesta fase para preservar baixa latencia, desacoplamento entre Gateway e PS_Login e simplicidade operacional. Nao sera introduzida blacklist distribuida, introspection por requisicao ou Redis apenas para invalidacao imediata enquanto o risco puder ser controlado por TTL curto.

## Quando reavaliar

Reavaliar revogacao imediata de access token se houver requisito regulatorio, operacoes financeiras/sensiveis, crescimento do risco de roubo de token, necessidade de encerramento administrativo instantaneo ou evidencias operacionais que tornem a janela residual inaceitavel.
