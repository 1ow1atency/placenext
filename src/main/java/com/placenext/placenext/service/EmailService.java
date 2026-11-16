package com.placenext.placenext.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Sends plain-text notification emails in the background.
 *
 * - If placenext.mail.enabled=false (the default), the email is only printed
 *   in the console, so the app works without any mail setup.
 * - If sending fails (wrong password, no internet...), it only logs a warning;
 *   the user's action (shortlist, accept offer, etc.) still succeeds.
 *
 * Callers must pass plain Strings (not entities), because this runs on a
 * separate thread outside the database session.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final boolean enabled;
    private final String from;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                        @Value("${placenext.mail.enabled:false}") boolean enabled,
                        @Value("${spring.mail.username:}") String from) {
        this.mailSenderProvider = mailSenderProvider;
        this.enabled = enabled;
        this.from = from;
    }

    @Async("mailExecutor")
    public void send(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            log.warn("Email skipped (no recipient): {}", subject);
            return;
        }

        String fullBody = body
                + "\n\n— PlaceNext Placement Portal"
                + "\nThis is an automated message. Please do not reply.";

        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (!enabled || sender == null || from.isBlank()) {
            log.info("""

                    ===== EMAIL (console only) =====
                    To:      {}
                    Subject: {}
                    {}
                    ================================""", to, "[PlaceNext] " + subject, fullBody);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject("[PlaceNext] " + subject);
            message.setText(fullBody);
            sender.send(message);
            log.info("Email sent to {}: {}", to, subject);
        } catch (Exception e) {
            log.warn("Email to {} failed ({}): {}", to, subject, e.getMessage());
        }
    }
}
