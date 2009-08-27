/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
/*
 *
 * Author: Dmitrii Zagorodnov dmitrii@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.admin.server;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Properties;
import java.util.UUID;
import org.apache.log4j.Logger;

public class ServletUtils extends HttpServlet {
    private static Logger LOG = Logger.getLogger(ServletUtils.class);

    public static void sendMail(String from, String to, String subject, String body)
            throws ServletException, IOException {
        Properties properties = System.getProperties();
        String mailHost = properties.getProperty("mail.smtp.host");
        if(mailHost == null) {
            mailHost = InetAddress.getLocalHost().getHostName();
            properties.setProperty("mail.smtp.host", mailHost);
        }
        try {
            send(from, to, subject, body);
        } catch(Exception ex) {
            //try again with mailhost
            properties.setProperty("mail.smtp.host", "mailhost");
            try {
                send(from, to, subject, body);
            } catch(Exception exception) {
		properties.setProperty("mail.smtp.host", "localhost");
		try {
		    //try with localhost
                    send(from, to, subject, body);
		} catch(Exception exception1) {
		    LOG.error("Unable to send mail" + exception1); 
		}
            }
        }
    }

    private static void send(String from, String to, String subject, String body) throws Exception {
        Properties properties = System.getProperties();
        Session session = Session.getDefaultInstance(properties);
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
        message.setSubject(subject);
        message.setText(body);
        Transport.send(message);
    }

    public static String getRequestUrl(HttpServletRequest req) {
        String scheme = req.getScheme();             // http
        String serverName = req.getServerName();     // hostname.com
        int serverPort = req.getServerPort();        // 80
        String contextPath = req.getContextPath();   // /mywebapp
        String servletPath = req.getServletPath();   // /servlet/MyServlet - WRONG in case of GWT
        String pathInfo = req.getPathInfo();         // /a/b;c=123
        String queryString = req.getQueryString();          // d=789
        String url = scheme+"://"+serverName+":"+serverPort+contextPath+"/";
        return url;
    }

    public static String genGUID() {
        UUID guid = UUID.randomUUID();
        return guid.toString();
    }
}
