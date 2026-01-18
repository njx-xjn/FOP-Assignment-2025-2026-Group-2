import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.File;

// THE MAILMAN
// This class handles the technical details of connecting to Gmail's SMTP server
// and sending the daily report with the text file attached.
//
// ** CONNECTION TO GUI.JAVA **
// This class is instantiated and used inside 'GUI.performAutoEmail()'.
public class EmailService {

    // --- CREDENTIALS ---
    // NOTE: For Gmail, you cannot use your normal login password.
    // You must generate an "App Password" in Google Account Settings -> Security.
    private final String username = "goldenhour.autoemailsystem@gmail.com"; 
    private final String password = "mhwv bmas nekk axce"; // The 16-digit App Password

    // Main method to trigger the email
    public void sendDailyReport(String toAddress, String reportContent, String filePath) {
        
        // --- 1. SERVER CONFIGURATION ---
        // We need to tell Java specifically how to talk to Gmail.
        Properties prop = new Properties();
        prop.put("mail.smtp.host", "smtp.gmail.com"); // The Server Address
        prop.put("mail.smtp.port", "587");            // Port 587 is standard for TLS encryption
        prop.put("mail.smtp.auth", "true");           // We need to log in
        prop.put("mail.smtp.starttls.enable", "true");// Encrypt the connection (Security)

        // --- 2. AUTHENTICATION ---
        // Create a Session (a logged-in connection) using the credentials above.
        Session session = Session.getInstance(prop,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        try {
            // --- 3. CONSTRUCT THE EMAIL ---
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            
            // Who are we sending this to? (Passed from GUI.java)
            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(toAddress));
            
            // Subject Line with today's date
            message.setSubject("GoldenHour Daily Sales Report - " + java.time.LocalDate.now());

            // --- 4. BUILD THE CONTENT (MULTIPART) ---
            // An email with an attachment is called "Multipart" because it has parts:
            // Part 1: The Message Text
            // Part 2: The Attached File
            Multipart multipart = new MimeMultipart();

            // PART 1: The Text Body
            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText(reportContent);
            multipart.addBodyPart(messageBodyPart);

            // PART 2: The Attachment
            if (filePath != null && !filePath.isEmpty()) {
                MimeBodyPart attachPart = new MimeBodyPart();
                try {
                    // Load the file from disk using the path provided
                    attachPart.attachFile(new File(filePath));
                    multipart.addBodyPart(attachPart);
                } catch (Exception e) {
                    System.out.println("Error attaching file: " + e.getMessage());
                }
            }

            // Seal the envelope (put the parts into the message)
            message.setContent(multipart);

            // --- 5. SEND ---
            // Hand the message over to the Transport layer to fly across the internet
            Transport.send(message);
            System.out.println("Email Sent Successfully to " + toAddress);

        } catch (MessagingException e) {
            // If internet is down or password is wrong, print error
            e.printStackTrace();
            System.out.println("Failed to send email. Check credentials or internet.");
        }
    }
}