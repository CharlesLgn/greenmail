package com.icegreen.greenmail.specificmessages;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.server.AbstractServer;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.Retriever;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.icegreen.greenmail.util.UserUtil;
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

/**
 * Tests if ReplyTo addresses of received messages are set correctly.
 */
class ReplyToTest {
    @RegisterExtension
    static final GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP_POP3_IMAP);

    private static final InternetAddress[] TO_ADDRESSES;
    private static final InternetAddress[] CC_ADDRESSES;
    private static final InternetAddress[] BCC_ADDRESSES;

    private static final InternetAddress FROM_ADDRESS;
    private static final InternetAddress[] REPLY_TO_ADDRESSES;

    static {
        try {
            TO_ADDRESSES = new InternetAddress[]{
                    new InternetAddress("to1@localhost", "To 1"),
                    new InternetAddress("to2@localhost", "To 2")
            };
            CC_ADDRESSES = new InternetAddress[]{
                    new InternetAddress("cc1@localhost", "Cc 1"),
                    new InternetAddress("cc2@localhost", "Cc 2")
            };
            BCC_ADDRESSES = new InternetAddress[]{
                    new InternetAddress("bcc1@localhost", "Bcc 1"),
                    new InternetAddress("bcc2@localhost", "Bcc 2")
            };
            FROM_ADDRESS = new InternetAddress("from@localhost", "From");
            REPLY_TO_ADDRESSES = new InternetAddress[]{
                    new InternetAddress("reply-to@localhost", "Reply-To")
            };
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testReplyToFromAddress() throws MessagingException {
        UserUtil.createUsers(greenMail, TO_ADDRESSES);
        UserUtil.createUsers(greenMail, CC_ADDRESSES);
        UserUtil.createUsers(greenMail, BCC_ADDRESSES);

        MimeMessage msg = new MimeMessage(greenMail.getSmtp().createSession());
        msg.setRecipients(Message.RecipientType.TO, TO_ADDRESSES);
        msg.setRecipients(Message.RecipientType.CC, CC_ADDRESSES);
        msg.setRecipients(Message.RecipientType.BCC, BCC_ADDRESSES);
        msg.setFrom(FROM_ADDRESS);
        msg.setSubject("Subject");
        msg.setText("text");

        GreenMailUtil.sendMimeMessage(msg);
        greenMail.waitForIncomingEmail(5000, 1);

        for (InternetAddress address : TO_ADDRESSES) {
            retrieveAndCheckReplyTo(address, FROM_ADDRESS);
        }
        for (InternetAddress address : CC_ADDRESSES) {
            retrieveAndCheckReplyTo(address, FROM_ADDRESS);
        }
        for (InternetAddress address : BCC_ADDRESSES) {
            retrieveAndCheckReplyTo(address, FROM_ADDRESS);
        }
    }

    @Test
    void testReplyToSpecificAddress() throws MessagingException {
        UserUtil.createUsers(greenMail, TO_ADDRESSES);
        UserUtil.createUsers(greenMail, CC_ADDRESSES);
        UserUtil.createUsers(greenMail, BCC_ADDRESSES);

        MimeMessage msg = new MimeMessage(greenMail.getSmtp().createSession());
        msg.setRecipients(Message.RecipientType.TO, TO_ADDRESSES);
        msg.setRecipients(Message.RecipientType.CC, CC_ADDRESSES);
        msg.setRecipients(Message.RecipientType.BCC, BCC_ADDRESSES);
        msg.setFrom(FROM_ADDRESS);
        msg.setReplyTo(REPLY_TO_ADDRESSES);
        msg.setSubject("Subject");
        msg.setText("text");

        GreenMailUtil.sendMimeMessage(msg);
        greenMail.waitForIncomingEmail(5000, 1);

        for (InternetAddress address : TO_ADDRESSES) {
            retrieveAndCheckReplyTo(address, REPLY_TO_ADDRESSES);
        }
        for (InternetAddress address : CC_ADDRESSES) {
            retrieveAndCheckReplyTo(address, REPLY_TO_ADDRESSES);
        }
        for (InternetAddress address : BCC_ADDRESSES) {
            retrieveAndCheckReplyTo(address, REPLY_TO_ADDRESSES);
        }
    }

    /**
     * Retrieve mail through IMAP and POP3 and check sender and receivers
     *
     * @param addr Address of account to retrieve
     */
    private void retrieveAndCheckReplyTo(InternetAddress addr, InternetAddress... replyToAddrs)
            throws MessagingException {
        String address = addr.getAddress();
        retrieveAndCheckReplyTo(greenMail.getPop3(), address, replyToAddrs);
        retrieveAndCheckReplyTo(greenMail.getImap(), address, replyToAddrs);
    }

    /**
     * Retrieve message from retriever and check the ReplyTo address
     *
     * @param server Server to read from
     * @param login  Account to retrieve
     */
    private void retrieveAndCheckReplyTo(AbstractServer server, String login, InternetAddress[] replyToAddrs)
            throws MessagingException {
        try (Retriever retriever = new Retriever(server)) {
            Message[] messages = retriever.getMessages(login);
            assertThat(messages).hasSize(1);
            Message message = messages[0];

            assertThat(toInetAddr(message.getReplyTo())).isEqualTo(replyToAddrs);
        }
    }

    /**
     * Cast input addresses from Address to the more specific InternetAddress
     *
     * @param addrs Input addresses
     * @return Output
     */
    private InternetAddress[] toInetAddr(Address[] addrs) {
        if (addrs == null) {
            return null;
        }
        final InternetAddress[] out = new InternetAddress[addrs.length];
        for (int i = 0; i < addrs.length; i++) {
            out[i] = (InternetAddress) addrs[i];
        }
        return out;
    }
}
