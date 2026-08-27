package com.mypetadmin.ps_login.mail;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SmtpActivationMailSenderTest {

    @Test
    void enviaLinkDeAtivacaoComToken() {
        JavaMailSender javaMailSender = mock(JavaMailSender.class);
        SmtpActivationMailSender sender = new SmtpActivationMailSender(
                javaMailSender, "no-reply@mypetadmin.com", "https://app.mypetadmin.com/ativar");

        sender.sendActivation("user@example.com", "abc_token");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());
        assertThat(captor.getValue().getTo()).containsExactly("user@example.com");
        assertThat(captor.getValue().getText()).contains("https://app.mypetadmin.com/ativar?token=abc_token");
    }
}
