package com.mypetadmin.ps_login.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionSecurityValidationTest {

    @Test
    void aceitaChaveInternaComPeloMenos32Bytes() {
        assertThatCode(() -> new ProductionSecurityValidation("0123456789abcdef0123456789abcdef"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejeitaChaveInternaVaziaOuCurta() {
        assertThatThrownBy(() -> new ProductionSecurityValidation(" "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("configurada");

        assertThatThrownBy(() -> new ProductionSecurityValidation("chave-curta"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("256 bits");
    }
}
