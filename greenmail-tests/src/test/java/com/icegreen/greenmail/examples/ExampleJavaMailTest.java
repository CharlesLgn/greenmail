package com.icegreen.greenmail.examples;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.angus.mail.imap.IMAPStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

/**
 * Example using plain JavaMail for sending / receiving mails via GreenMail server.
 */
class ExampleJavaMailTest {
    @RegisterExtension
    static final GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP_IMAP);

    @Test
    void testSendAndReceive() throws MessagingException {
        Session smtpSession = greenMail.getSmtp().createSession();

        Message msg = new MimeMessage(smtpSession);
        msg.setFrom(new InternetAddress("foo@example.com"));
        msg.addRecipient(Message.RecipientType.TO,
                new InternetAddress("bar@example.com"));
        msg.setSubject("Email sent to GreenMail via plain JavaMail");
        msg.setText("Fetch me via IMAP");
        Transport.send(msg);

        // Create user, as connect verifies pwd
        greenMail.setUser("bar@example.com", "bar@example.com", "secret-pwd");

        // Alternative 1: Create session and store or ...
        Session imapSession = greenMail.getImap().createSession();
        Store store = imapSession.getStore("imap");
        store.connect("bar@example.com", "secret-pwd");
        Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_ONLY);
        Message msgReceived = inbox.getMessage(1);
        assertThat(msgReceived.getSubject()).isEqualTo(msg.getSubject());

        // Alternative 2: ... let GreenMail create and configure a store:
        IMAPStore imapStore = greenMail.getImap().createStore();
        imapStore.connect("bar@example.com", "secret-pwd");
        inbox = imapStore.getFolder("INBOX");
        inbox.open(Folder.READ_ONLY);
        msgReceived = inbox.getMessage(1);
        assertThat(msgReceived.getSubject()).isEqualTo(msg.getSubject());

        // Alternative 3: ... directly fetch sent message using GreenMail API
        assertThat(greenMail.getReceivedMessagesForDomain("bar@example.com")).hasSize(1);
        msgReceived = greenMail.getReceivedMessagesForDomain("bar@example.com")[0];
        assertThat(msgReceived.getSubject()).isEqualTo(msg.getSubject());

        store.close();
        imapStore.close();
    }
}
