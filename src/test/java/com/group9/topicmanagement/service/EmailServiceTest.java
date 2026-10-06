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
}
