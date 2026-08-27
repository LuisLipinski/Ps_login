package com.mypetadmin.ps_login.controller;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VersionControllerTest {

    @Test
    void retornaCommitConfigurado() {
        VersionController controller = new VersionController("abc123");

        assertThat(controller.version()).isEqualTo("abc123");
    }
}
