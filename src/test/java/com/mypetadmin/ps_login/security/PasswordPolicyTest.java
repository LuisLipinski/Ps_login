package com.mypetadmin.ps_login.security;

import com.mypetadmin.ps_login.exception.PasswordPolicyException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {

    private final PasswordPolicy policy = new PasswordPolicy(12);

    @Test
    void aceitaSenhaDentroDoLimiteQuandoConfirmacaoConfere() {
        policy.validate("SenhaSegura123", "SenhaSegura123");
    }

    @Test
    void rejeitaConfirmacaoDiferente() {
        assertThatThrownBy(() -> policy.validate("SenhaSegura123", "OutraSenha123"))
                .isInstanceOf(PasswordPolicyException.class);
    }

    @Test
    void rejeitaSenhaCurta() {
        assertThatThrownBy(() -> policy.validate("curta", "curta"))
                .isInstanceOf(PasswordPolicyException.class);
    }

    @Test
    void rejeitaSenhaMuitoLonga() {
        String password = "a".repeat(129);
        assertThatThrownBy(() -> policy.validate(password, password))
                .isInstanceOf(PasswordPolicyException.class);
    }
}
