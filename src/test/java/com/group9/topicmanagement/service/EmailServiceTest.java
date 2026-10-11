package com.group9.topicmanagement.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailServiceTest {
    @Test void disabledMailDoesNotContactSmtpOrClaimSuccess() {
        JavaMailSender sender=mock(JavaMailSender.class);
        assertThat(new EmailService(sender,false,"sender@test.local").sendNotification("sv@test.local","Kết quả","Đã công bố").join())
                .isEqualTo(EmailService.DeliveryStatus.DISABLED);
        verifyNoInteractions(sender);
    }
    @Test void sendsVietnameseMessageWithCorrectRecipient() {
        JavaMailSender sender=mock(JavaMailSender.class);
        assertThat(new EmailService(sender,true,"sender@test.local").sendNotification("sv@test.local","Kết quả","Điểm đã công bố").join())
                .isEqualTo(EmailService.DeliveryStatus.SENT);
        ArgumentCaptor<SimpleMailMessage> captor=ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());
        assertThat(captor.getValue().getTo()).containsExactly("sv@test.local");
        assertThat(captor.getValue().getText()).isEqualTo("Điểm đã công bố");
    }
    @Test void smtpFailureIsReportedWithoutThrowingIntoBusinessRequest() {
        JavaMailSender sender=mock(JavaMailSender.class);
        doThrow(new MailSendException("offline")).when(sender).send(any(SimpleMailMessage.class));
        assertThat(new EmailService(sender,true,"sender@test.local").sendNotification("sv@test.local","Kết quả","Đã công bố").join())
                .isEqualTo(EmailService.DeliveryStatus.FAILED);
    }
    @Test void rejectsInvalidRecipientAndHeaderInjection() {
        JavaMailSender sender=mock(JavaMailSender.class);
        EmailService service=new EmailService(sender,true,"sender@test.local");
        assertThat(service.sendNotification("invalid","Kết quả","Nội dung").join()).isEqualTo(EmailService.DeliveryStatus.INVALID);
        assertThat(service.sendNotification("sv@test.local","Kết quả\r\nBcc: x@test.local","Nội dung").join()).isEqualTo(EmailService.DeliveryStatus.INVALID);
        verifyNoInteractions(sender);
    }
    @Test void cloudUsesBrevoOnlyAndStillRespectsDisabledFlag() {
        JavaMailSender sender=mock(JavaMailSender.class);
        BrevoEmailClient brevo=mock(BrevoEmailClient.class);
        when(brevo.isConfigured()).thenReturn(true);
        when(brevo.send("sender@test.local","sv@test.local","Kết quả","Đã công bố"))
                .thenReturn(EmailService.DeliveryStatus.SENT);
        EmailService service=new EmailService(sender,true,"sender@test.local","brevo",brevo);
        assertThat(service.sendNotification("sv@test.local","Kết quả","Đã công bố").join())
                .isEqualTo(EmailService.DeliveryStatus.SENT);
        verify(brevo,times(1)).send("sender@test.local","sv@test.local","Kết quả","Đã công bố");
        assertThat(new EmailService(sender,false,"sender@test.local","brevo",brevo)
                .sendNotification("sv@test.local","Kết quả","Đã công bố").join())
                .isEqualTo(EmailService.DeliveryStatus.DISABLED);
        verify(brevo,times(1)).send(anyString(),anyString(),anyString(),anyString());
        verifyNoInteractions(sender);
    }
    @Test void enabledBrevoWithoutKeyAndUnknownProvidersFailClosed() {
        JavaMailSender sender=mock(JavaMailSender.class);
        BrevoEmailClient brevo=mock(BrevoEmailClient.class);
        assertThatThrownBy(()->new EmailService(sender,true,"sender@test.local","brevo",brevo))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("BREVO_API_KEY");
        assertThatThrownBy(()->new EmailService(sender,false,"sender@test.local","typo",brevo))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("MAIL_PROVIDER");
        verifyNoInteractions(sender);
    }
    @Test void invalidCloudInputIsNotSentToEitherProvider() {
        JavaMailSender sender=mock(JavaMailSender.class);
        BrevoEmailClient brevo=mock(BrevoEmailClient.class);
        when(brevo.isConfigured()).thenReturn(true);
        EmailService service=new EmailService(sender,true,"sender@test.local","brevo",brevo);
        assertThat(service.sendNotification("invalid","Kết quả","Đã công bố").join()).isEqualTo(EmailService.DeliveryStatus.INVALID);
        assertThat(service.sendNotification("sv@test.local","Subject\nBcc: x@test.local","Body").join()).isEqualTo(EmailService.DeliveryStatus.INVALID);
        assertThat(new EmailService(sender,true,"invalid","brevo",brevo)
                .sendNotification("sv@test.local","Kết quả","Đã công bố").join()).isEqualTo(EmailService.DeliveryStatus.INVALID);
        verify(brevo,never()).send(anyString(),anyString(),anyString(),anyString());
        verifyNoInteractions(sender);
    }
}
