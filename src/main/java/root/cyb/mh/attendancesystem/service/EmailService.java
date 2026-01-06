package root.cyb.mh.attendancesystem.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import root.cyb.mh.attendancesystem.model.PaymentRequest;

import java.io.ByteArrayOutputStream;

@Service
public class EmailService {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender defaultMailSender;

    @Autowired
    private DataImportExportService dataImportExportService;

    public void sendInvoiceEmail(String toEmail, PaymentRequest request) throws MessagingException {
        logger.info("Starting email send process for Request #{} to {}", request.getId(), toEmail);

        // Determine Sender
        JavaMailSender sender = defaultMailSender;
        root.cyb.mh.attendancesystem.model.Company company = request.getCompany();

        if (company != null && company.getSmtpHost() != null && !company.getSmtpHost().isEmpty()) {
            logger.info("Using Company SMTP configuration for: {}", company.getName());
            sender = createCompanySender(company);
        } else {
            logger.info("Using Default System SMTP configuration.");
        }

        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(toEmail);
            helper.setSubject("Invoice for Payment Request #" + request.getId());

            String companyName = (company != null) ? company.getName() : "Skylink Construction";
            String contractorName = (request.getContractorName() != null) ? request.getContractorName() : "Contractor";

            String body = String.format("""
                    Dear %s,

                    Please find attached the invoice for Payment Request #%s.

                    Details:
                    Work Order: %s
                    Amount: $%s
                    Contractor: %s

                    Best Regards,
                    %s
                    """,
                    contractorName,
                    request.getWorkOrderNumber(),
                    request.getWorkOrderNumber(),
                    request.getAmount(),
                    request.getContractorName(),
                    companyName);

            helper.setText(body);

            // Generate PDF
            logger.debug("Generating Invoice PDF...");
            ByteArrayOutputStream pdfStream = new ByteArrayOutputStream();
            dataImportExportService.generateInvoicePdf(pdfStream, request);
            byte[] pdfBytes = pdfStream.toByteArray();
            logger.debug("PDF Generated, size: {} bytes", pdfBytes.length);

            // Attach PDF
            helper.addAttachment("Invoice_" + request.getId() + ".pdf", new ByteArrayResource(pdfBytes));

            logger.info("Sending email invoking JavaMailSender...");
            sender.send(message);
            logger.info("Email sent successfully!");
        } catch (Exception e) {
            logger.error("Failed to send email: ", e);
            throw e;
        }
    }

    private JavaMailSender createCompanySender(root.cyb.mh.attendancesystem.model.Company company) {
        org.springframework.mail.javamail.JavaMailSenderImpl sender = new org.springframework.mail.javamail.JavaMailSenderImpl();
        sender.setHost(company.getSmtpHost());
        sender.setPort(company.getSmtpPort() != null ? company.getSmtpPort() : 587);
        sender.setUsername(company.getSmtpUsername());
        sender.setPassword(company.getSmtpPassword());

        java.util.Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        return sender;
    }
}
