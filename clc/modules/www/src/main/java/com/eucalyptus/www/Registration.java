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

package com.eucalyptus.www;

import java.io.IOException;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.persistence.PersistenceException;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.ServiceUris;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.component.id.Storage;
import com.eucalyptus.component.id.Walrus;
import com.eucalyptus.crypto.Hmac;
import com.eucalyptus.network.NetworkGroups;
import com.eucalyptus.util.Internets;
import com.eucalyptus.ws.StackConfiguration;
import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration;

public class Registration extends HttpServlet {
  private static Logger LOG = Logger.getLogger( Registration.class );
  
  private static String getMessage( String key, String uuid ) {
    
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<Signature>\n" + "  <SignedInfo>\n"
           + "    <SignatureMethod>http://www.w3.org/2001/04/xmldsig-more#hmac-sha256</SignatureMethod>\n" + "  </SignedInfo>\n" + "  <SignatureValue>"
           + getSignature( key, uuid ) + "</SignatureValue>\n" + "  <Object>\n" + getConfigurationString( uuid ) + "\n</Object>\n" + "</Signature>";
  }
  
  private static String getConfigurationString( String uuid ) {
    return "<CloudSchema>\n" + "  <Services type=\"array\">\n" + "    <Service>\n" + "      <Name>ec2</Name>\n" + "      <EndpointUrl>"
       + ServiceUris.remote( Eucalyptus.class, Internets.localHostInetAddress( ) ) + "</EndpointUrl>\n" + "      <Resources type=\"array\">\n" + "        <Resource>\n"
           + "          <Name>instances</Name>\n" + "        </Resource>\n" + "        <Resource>\n" + "          <Name>security_groups</Name>\n"
           + "        </Resource>\n" + "        <Resource>\n" + "          <Name>ssh_keys</Name>\n" + "        </Resource>\n" + "        <Resource>\n"
           + "          <Name>images</Name>\n" + "        </Resource>\n" + blockStorageConfiguration( ) + publicAddressConfiguration( )
           + "      </Resources>\n" + "    </Service>\n" + "    <Service>\n" + "      <Name>s3</Name>\n" + "      <EndpointUrl>" + getWalrusUrl( )
           + "</EndpointUrl>\n" + "      <Resources type=\"array\">\n" + "        <Resource>\n" + "          <Name>buckets</Name>\n" + "        </Resource>\n"
           + "        <Resource>\n" + "          <Name>keys</Name>\n" + "        </Resource>\n" + "      </Resources>\n" + "    </Service>\n"
           + "  </Services>\n" + "  <id>" + uuid + "</id>" + "\n  <CloudType>eucalyptus</CloudType>\n" + "  <CloudVersion>" + System.getProperty( "euca.version" )
           + "</CloudVersion>\n" + "  <SchemaVersion>1.0</SchemaVersion>\n" + "  <Description>Public cloud in the new cluster</Description>\n"
           + "  <Credentials type=\"array\">\n" + "    <Credential>\n" + "      <Required type=\"boolean\">false</Required>\n"
           + "      <Name>username</Name>\n" + "      <Nickname>User ID</Nickname>\n" + "      <Description>Username....</Description>\n"
           + "    </Credential>\n" + "    <Credential>\n" + "      <Required type=\"boolean\">true</Required>\n" + "      <Name>aws_access_key</Name>\n"
           + "      <Nickname>Query ID</Nickname>\n" + "      <Description>Access key....</Description>\n" + "    </Credential>\n" + "    <Credential>\n"
           + "      <Required type=\"boolean\">true</Required>\n" + "      <Name>aws_secret_access_key</Name>\n" + "      <Nickname>Secret Key</Nickname>\n"
           + "      <Description>Secret Access key....</Description>\n" + "    </Credential>\n" + "  </Credentials>\n" + "</CloudSchema>\n";
  }
  
  private static String blockStorageConfiguration( ) {
    try {
      if ( !ServiceConfigurations.list( Storage.class ).isEmpty( ) ) {
        return "        <Resource>\n" + "          <Name>ebs_snapshots</Name>\n" + "        </Resource>\n" + "        <Resource>\n"
               + "          <Name>ebs_volumes</Name>\n" + "        </Resource>\n";
      }
    } catch ( PersistenceException e ) {
      LOG.debug( e, e );
    }
    return "";
  }
  
  private static String publicAddressConfiguration( ) {
    if ( NetworkGroups.networkingConfiguration( ).hasNetworking( ) ) {
      return "        <Resource>\n" + "          <Name>elastic_ips</Name>\n" + "        </Resource>\n";
    } else {
      return "";
    }
  }
  
  private static String getWalrusUrl( ) {
    if( Topology.isEnabledLocally( Walrus.class ) ) {
      ServiceConfiguration walrusConfig = Topology.lookup( Walrus.class );
      return ServiceUris.remote( walrusConfig ).toASCIIString( );
    } else {
      return "NOT REGISTERED.";
    }
  }
  
  private static String getRegistrationId( ) {
    return SystemConfiguration.getSystemConfiguration( ).getRegistrationId( );
  }
  
  @Override
  protected void doGet( final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse ) throws ServletException, IOException {
    String uuid = UUID.randomUUID( ).toString( );
    String registrationId = getRegistrationId( );
    ServletOutputStream op = httpServletResponse.getOutputStream( );
    LOG.info( "Using registrationId: " + registrationId );
    op.write( getMessage( registrationId, UUID.randomUUID( ).toString( ) ).getBytes( ) );
    op.flush( );
  }
  
  private static String getSignature( String key, String uuid ) {
    SecretKeySpec signingKey = new SecretKeySpec( key.getBytes( ), Hmac.HmacSHA256.toString( ) );
    try {
      Mac mac = Hmac.HmacSHA256.getInstance( );
      mac.init( signingKey );
      byte[] rawHmac = mac.doFinal( uuid.getBytes( ) );
      String sig = Hashes.getHexString( rawHmac );
      LOG.warn( "\nkey='" + key + "'\nid='" + uuid + "'\nresult=" + sig );
      return sig;
    } catch ( Exception e ) {
      LOG.error( e );
      return "error: " + e.getMessage( );
    }
    
  }
}
