
// import java.util.Properties;
// import javax.mail.*;
// import javax.mail.internet.*;

public class EmailService {
    // NOTE: This feature requires valid SMTP credentials to work.
    // For safety, we will use a dummy implementation that PRINTS the email to
    // console
    // instead of actually sending it, unless the user configures real credentials
    // below.

    private final String username = "your_email@gmail.com";
    private final String password = "your_app_password";

    public void sendDailyReport(String toAddress, String reportContent) {
        System.out.println(">>> [MOCKED EMAIL] Sending Data to " + toAddress + " >>>");
        System.out.println("Subject: GoldenHour Daily Sales Report");
        System.out.println("Body: \n" + reportContent);
        System.out.println(">>> [END EMAIL] >>>");

        // UNCOMMENT BELOW TO ENABLE REAL EMAIL (Requires javax.mail jar in classpath)
        /*
         * Properties prop = new Properties();
         * prop.put("mail.smtp.host", "smtp.gmail.com");
         * prop.put("mail.smtp.port", "587");
         * prop.put("mail.smtp.auth", "true");
         * prop.put("mail.smtp.starttls.enable", "true"); //TLS
         * 
         * Session session = Session.getInstance(prop,
         * new javax.mail.Authenticator() {
         * protected PasswordAuthentication getPasswordAuthentication() {
         * return new PasswordAuthentication(username, password);
         * }
         * });
         * 
         * try {
         * Message message = new MimeMessage(session);
         * message.setFrom(new InternetAddress(username));
         * message.setRecipients(
         * Message.RecipientType.TO,
         * InternetAddress.parse(toAddress)
         * );
         * message.setSubject("GoldenHour Daily Sales Report");
         * message.setText(reportContent);
         * 
         * Transport.send(message);
         * System.out.println("Email Sent Successfully!");
         * 
         * } catch (MessagingException e) {
         * e.printStackTrace();
         * }
         */
    }
}
