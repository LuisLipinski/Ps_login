package com.mypetadmin.ps_login.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenCodecTest {

    private final TokenCodec codec = new TokenCodec();

    @Test
    void geraTokenAleatorioUrlSafe() {
        String first = codec.generate();
        String second = codec.generate();

        assertThat(first).isNotBlank().doesNotContain("=");
        assertThat(second).isNotEqualTo(first);
    }

    @Test
    void hashSha256EhDeterministicoESemTokenPuro() {
        String hash = codec.hash("token-secreto");

        assertThat(hash).hasSize(64).doesNotContain("token-secreto");
        assertThat(codec.hash("token-secreto")).isEqualTo(hash);
    }
}
