CREATE TABLE IF NOT EXISTS sessoes_acesso (
    id         UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id    UUID        NOT NULL,
    user_type  VARCHAR(50) NOT NULL,
    entrada_em TIMESTAMPTZ NOT NULL,
    saida_em   TIMESTAMPTZ
);

-- Índice parcial: garante unicidade de sessão aberta por usuário
-- (não implementável no H2, apenas no PostgreSQL)
CREATE UNIQUE INDEX IF NOT EXISTS uq_sessao_aberta
    ON sessoes_acesso (user_id)
    WHERE saida_em IS NULL;
