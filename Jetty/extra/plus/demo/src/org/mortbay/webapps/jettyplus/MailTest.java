package org.mortbay.webapps.jettyplus;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.mail.Session;
import javax.mail.Message;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.InternetAddress;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.mortbay.util.Log;

/**
 * MailTest.java
 *
 *
 * Created: Fri May 30 23:29:50 2003
 *
 * @author <a href="mailto:janb@wafer">Jan Bartel</a>
 * @version 1.0
 */
public class MailTest extends HttpServlet
{
    public static final String DATE_FORMAT = "EEE, d MMM yy HH:mm:ss Z";
    SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);


    public void doGet (HttpServletRequest request,
                       HttpServletResponse response) 
        throws ServletException, IOException
    {
        response.setContentType ("text/html");
        Writer writer = response.getWriter();
        writer.write ("<HTML><TITLE>Mail Sending Test</TITLE>");
        
        try
        {
            InitialContext ctx = new InitialContext();
            Session session = (Session)ctx.lookup ("java:comp/env/mail/TestMail");
            
            // create a message
            Message msg = new MimeMessage(session);
            
            String sender = request.getParameter("sender");
            String recipient = request.getParameter("recipient");

            if (sender == null)
                throw new ServletException ("No sender configured");
            if (sender.trim().equals(""))
                throw new ServletException ("No sender configured");

            if (recipient == null)
                throw new ServletException ("No recipient configured");
            if (recipient.trim().equals(""))
                throw new ServletException ("No recipient configured");
                

            Log.event("Sender="+sender);
            Log.event("Recipient="+recipient);

            // set the from and to address
            InternetAddress addressFrom = new InternetAddress(sender);
            msg.setFrom(addressFrom);
            
           
            msg.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            msg.setSubject("JettyPlus Mail Test Succeeded");
            msg.setContent("The test of the JettyPlus Mail Service @ "+new Date()+" has been successful.", "text/plain");
            msg.addHeader ("Date", dateFormat.format(new Date()));
            Transport.send(msg);

            writer.write ("Congratulations, your test of the JettyPlus Mail Service succeeded. Your recipient should now have mail");
        }
        catch (Exception e)
        {
            Log.warning (e.getMessage());
            writer.write ("<font color=red>Test failed: "+e.getMessage()+"</font>");
        }

        writer.write ("</BODY></HTML>");
    }
    
} // MailTest
