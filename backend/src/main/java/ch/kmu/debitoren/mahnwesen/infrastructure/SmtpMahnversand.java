package ch.kmu.debitoren.mahnwesen.infrastructure;

import ch.kmu.debitoren.mahnwesen.domain.Mahnversand;
import jakarta.mail.MessagingException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Versand über SMTP. Absender ist eine feste Adresse der Anwendung mit dem Namen des Mandanten; Antworten gehen
 * an die E-Mail-Adresse des Mandanten. So bleibt die Zustellbarkeit (SPF, DKIM) bei einer Domain.
 */
@Component
class SmtpMahnversand implements Mahnversand {

    private final JavaMailSender mailer;
    private final String absender;

    SmtpMahnversand(JavaMailSender mailer, @Value("${debitoren.mail.absender}") String absender) {
        this.mailer = mailer;
        this.absender = absender;
    }

    @Override
    public void senden(Mahnmail mail) {
        try {
            var nachricht = mailer.createMimeMessage();
            var helfer = new MimeMessageHelper(nachricht, true, StandardCharsets.UTF_8.name());
            helfer.setFrom(absender, mail.absenderName());
            if (mail.antwortAn() != null) {
                helfer.setReplyTo(mail.antwortAn());
            }
            helfer.setTo(mail.an());
            helfer.setSubject(mail.betreff());
            helfer.setText(mail.text(), false);
            helfer.addAttachment(mail.dateiname(), new ByteArrayResource(mail.pdf()), "application/pdf");
            mailer.send(nachricht);
        } catch (MessagingException | UnsupportedEncodingException | MailException e) {
            throw new VersandFehlgeschlagen("Der Mailserver hat die Mahnung nicht angenommen: " + e.getMessage(), e);
        }
    }
}
