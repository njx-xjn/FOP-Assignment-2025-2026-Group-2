import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.File;

public class EmailService {

    // UPDATE THESE WITH YOUR REAL CREDENTIALS
    private final String username = "goldenhour.autoemailsystem@gmail.com"; // Example placeholder
    private final String password = "mhwv bmas nekk axce"; // Example App Password

    public void sendDailyReport(String toAddress, String reportContent, String filePath) {
        // Setup Mail Server Properties
        Properties prop = new Properties();
        prop.put("mail.smtp.host", "smtp.gmail.com");
        prop.put("mail.smtp.port", "587");
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(prop,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(toAddress));
            message.setSubject("GoldenHour Daily Sales Report - " + java.time.LocalDate.now());

            // Create Multipart
            Multipart multipart = new MimeMultipart();

            // 1. Body Text
            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText(reportContent);
            multipart.addBodyPart(messageBodyPart);

            // 2. Attachment
            if (filePath != null && !filePath.isEmpty()) {
                MimeBodyPart attachPart = new MimeBodyPart();
                try {
                    attachPart.attachFile(new File(filePath));
                    multipart.addBodyPart(attachPart);
                } catch (Exception e) {
                    System.out.println("Error attaching file: " + e.getMessage());
                }
            }

            message.setContent(multipart);

            // Send
            Transport.send(message);
            System.out.println("Email Sent Successfully to " + toAddress);

        } catch (MessagingException e) {
            e.printStackTrace();
            System.out.println("Failed to send email. Check credentials or internet.");
        }
    }
}
