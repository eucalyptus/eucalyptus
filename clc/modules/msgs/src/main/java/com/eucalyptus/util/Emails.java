/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.util;

import java.io.UnsupportedEncodingException;
import java.util.Properties;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.apache.log4j.Logger;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableFieldType;

@ConfigurableClass( root = "bootstrap.notifications.email",
                    description = "Parameters controlling the delivery of notification emails." )
public class Emails {
  private static Logger LOG             = Logger.getLogger( Emails.class );
  @ConfigurableField( description = "SMTP host to use when sending email.  If unset, the following values are tried: 1) the value of the 'mail.smtp.host' system property, 2) localhost, 3) mailhost." )
  public static String  EMAIL_SMTP_HOST = null;
  @ConfigurableField( description = "SMTP port to use when sending email.  Defaults to 25" )
  public static Integer EMAIL_SMTP_PORT = 25;
  
  enum SessionProperties {
    MAIL_SMTP_HOST {
      
      @Override
      public String getPropertyValue( ) {
        if ( Emails.EMAIL_SMTP_HOST != null ) {
          return Emails.EMAIL_SMTP_HOST;
        } else if ( System.getProperty( this.getPropertyName( ) ) != null ) {
          return System.getProperty( this.getPropertyName( ) );
        } else {
          return "localhost";
        }
      }
      
    },
    MAIL_SMTP_PORT( String.valueOf( 25 ) ) {
      
      @Override
      public String getPropertyValue( ) {
        if ( Emails.EMAIL_SMTP_PORT != null ) {
          return String.valueOf( Emails.EMAIL_SMTP_PORT );
        } else {
          return super.getPropertyValue( );
        }
      }
      
    },
    MAIL_TRANSPORT_PROTOCOL( "smtp" ),
    MAIL_DEBUG( String.valueOf( true ) );
    private final String value;
    
    private SessionProperties( ) {
      this.value = null;
    }
    
    private SessionProperties( String value ) {
      this.value = value;
    }
    
    public String getPropertyValue( ) {
      return this.value;
    }
    
    public final String getPropertyName( ) {
      return this.name( ).replace( "_", "." ).toLowerCase( );
    }
  }
  
  private static Address validate( String emailAddress, String emailName ) {
    if ( emailAddress != null ) {
      try {
        InternetAddress addr = new InternetAddress( emailAddress, emailName );
        addr.validate( );
        return addr;
      } catch ( AddressException ex ) {
        throw new IllegalArgumentException( "Provided address could not be validated: " + ( emailName != null ? emailName : "" ) + " <" + emailAddress + ">", ex );
      } catch ( UnsupportedEncodingException ex ) {
        throw new IllegalArgumentException( "Provided address could not be validated: " + ( emailName != null ? emailName : "" ) + " <" + emailAddress + ">", ex );
      }
    } else {
      throw new IllegalArgumentException( "Address must be not-null." );
    }
  }
  
  public static void send( String from, String to, String subject, String content ) {
    send( from, null, to, subject, content );
  }
  
  public static void send( String from, String fromName, String to, String subject, String content ) {
    Address fromAddress = validate( from, fromName );
    Address toAddress = validate( to, null );
    final Properties properties = new Properties( System.getProperties( ) );
    for ( SessionProperties p : SessionProperties.values( ) ) {
      properties.setProperty( p.getPropertyName( ), p.getPropertyValue( ) );
    }
    try {
      doSend( subject, content, fromAddress, toAddress, properties );
    } catch ( MessagingException ex ) {
      try {
        properties.setProperty( SessionProperties.MAIL_SMTP_HOST.getPropertyName( ), "mailhost" );
        doSend( subject, content, fromAddress, toAddress, properties );
      } catch ( MessagingException ex1 ) {
        LOG.error( ex1, ex1 );
      }
    }
  }
  
  private static void doSend( String subject, String content, Address fromAddress, Address toAddress, final Properties properties ) throws MessagingException {
    Session session = Session.getDefaultInstance( properties );
    MimeMessage message = new MimeMessage( session );
    message.setSubject( subject );
    message.setText( content );
    message.setFrom( fromAddress );
    message.setRecipient( Message.RecipientType.TO, toAddress );
    Transport.send( message );
  }
  
}
