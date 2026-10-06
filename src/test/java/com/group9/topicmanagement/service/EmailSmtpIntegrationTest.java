package com.group9.topicmanagement.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

class EmailSmtpIntegrationTest {
    @Test void sendsVietnameseMailToLocalSmtpServer() throws Exception {
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
             ExecutorService worker = Executors.newSingleThreadExecutor()) {
            server.setSoTimeout(10000);
            Future<String> received = worker.submit(() -> {
                try (Socket socket = server.accept()) {
                    socket.setSoTimeout(10000);
                    var input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                    var output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                    reply(output, "220 localhost test SMTP");
                    StringBuilder data = new StringBuilder();
                    boolean readingData = false;
                    String line;
                    while ((line = input.readLine()) != null) {
                        if (readingData) {
                            if (line.equals(".")) { readingData = false; reply(output, "250 accepted"); }
                            else data.append(line.startsWith("..") ? line.substring(1) : line).append("\r\n");
                        } else if (line.startsWith("EHLO") || line.startsWith("HELO")) reply(output, "250 localhost");
                        else if (line.startsWith("MAIL FROM") || line.startsWith("RCPT TO")) reply(output, "250 OK");
                        else if (line.equals("DATA")) { readingData = true; reply(output, "354 end with dot"); }
                        else if (line.equals("QUIT")) { reply(output, "221 bye"); break; }
                        else reply(output, "250 OK");
                    }
                    return data.toString();
                }
            });
            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(InetAddress.getLoopbackAddress().getHostAddress()); sender.setPort(server.getLocalPort());
            sender.setDefaultEncoding("UTF-8");
            sender.getJavaMailProperties().setProperty("mail.smtp.connectiontimeout", "5000");
            sender.getJavaMailProperties().setProperty("mail.smtp.timeout", "5000");
            var service = new EmailService(sender, true, "sender@test.local");
            assertThat(service.sendNotification("student@test.local", "Kết quả đề tài", "Điểm của nhóm đã công bố.").join())
                    .isEqualTo(EmailService.DeliveryStatus.SENT);
            MimeMessage message = new MimeMessage(sender.getSession(),
                    new ByteArrayInputStream(received.get(10, TimeUnit.SECONDS).getBytes(StandardCharsets.UTF_8)));
            assertThat(message.getSubject()).isEqualTo("Kết quả đề tài");
            assertThat(message.getAllRecipients()[0].toString()).isEqualTo("student@test.local");
            assertThat(message.getContent().toString()).contains("Điểm của nhóm đã công bố.");
        }
    }

    private static void reply(BufferedWriter output, String response) throws IOException {
        output.write(response + "\r\n"); output.flush();
    }
}
