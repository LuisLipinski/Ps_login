package com.mypetadmin.ps_login.mail;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SmtpPasswordResetMailSenderTest {

    @Test
    void enviaLinkDeRedefinicaoComToken() {
        JavaMailSender javaMailSender = mock(JavaMailSender.class);
        SmtpPasswordResetMailSender sender = new SmtpPasswordResetMailSender(
                javaMailSender, "no-reply@mypetadmin.com", "https://app.mypetadmin.com/redefinir-senha");

        sender.sendReset("user@example.com", "abc_token");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());
        assertThat(captor.getValue().getTo()).containsExactly("user@example.com");
        assertThat(captor.getValue().getText())
                .contains("https://app.mypetadmin.com/redefinir-senha?token=abc_token");
    }
}
