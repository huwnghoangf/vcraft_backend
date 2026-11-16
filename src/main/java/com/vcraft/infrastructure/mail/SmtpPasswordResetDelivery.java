package com.vcraft.infrastructure.mail;

import com.vcraft.domain.exception.ServiceUnavailableException;
import com.vcraft.domain.service.PasswordResetDelivery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

@Component
public class SmtpPasswordResetDelivery implements PasswordResetDelivery {
    private final boolean enabled;
    private final String from;
    private final String resetUrl;
    private final JavaMailSenderImpl sender = new JavaMailSenderImpl();
    public SmtpPasswordResetDelivery(@Value("${app.mail.enabled:false}") boolean enabled,
            @Value("${spring.mail.host:}") String host, @Value("${spring.mail.port:587}") int port,
            @Value("${spring.mail.username:}") String username, @Value("${spring.mail.password:}") String password,
            @Value("${app.mail.from:noreply@vcraft.local}") String from,
            @Value("${app.password-reset-url:http://localhost:3000/reset-password}") String resetUrl,
            @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}") boolean tls) {
        this.enabled = enabled && !host.isBlank(); this.from = from; this.resetUrl = resetUrl;
        sender.setHost(host); sender.setPort(port); sender.setUsername(username); sender.setPassword(password);
        var props = sender.getJavaMailProperties();
        props.setProperty("mail.smtp.auth", Boolean.toString(!username.isBlank()));
        props.setProperty("mail.smtp.starttls.enable", Boolean.toString(tls));
        props.setProperty("mail.smtp.starttls.required", Boolean.toString(tls));
        props.setProperty("mail.smtp.connectiontimeout", "5000");
        props.setProperty("mail.smtp.timeout", "5000");
        props.setProperty("mail.smtp.writetimeout", "5000");
    }
    public boolean available() { return enabled; }
    public void send(String recipient, String token) {
        if (!available()) throw new ServiceUnavailableException("Chưa cấu hình dịch vụ email");
        var mail = new SimpleMailMessage(); mail.setFrom(from); mail.setTo(recipient);
        mail.setSubject("vCraft - Đặt lại mật khẩu");
        mail.setText("Liên kết chỉ dùng một lần, có hiệu lực 15 phút:\n" + resetUrl
                + (resetUrl.contains("?") ? "&" : "?") + "token=" + token
                + "\nNếu bạn không yêu cầu, hãy bỏ qua email này.");
        try { sender.send(mail); }
        catch (org.springframework.mail.MailException ex) { throw new ServiceUnavailableException("Không thể gửi email, vui lòng thử lại sau"); }
    }
}
