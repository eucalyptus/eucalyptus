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
package edu.ucsb.eucalyptus.admin.server;

import com.eucalyptus.auth.Hashes;
import com.eucalyptus.util.EucalyptusCloudException;
import edu.ucsb.eucalyptus.util.EucalyptusProperties;
import org.apache.log4j.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

public class Registration extends HttpServlet {
  private static Logger LOG = Logger.getLogger( Registration.class );

  private static String getMessage( String key, String uuid ) {

    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
           "<Signature>\n" +
           "  <SignedInfo>\n" +
           "    <SignatureMethod>http://www.w3.org/2001/04/xmldsig-more#hmac-sha256</SignatureMethod>" +
           "  </SignedInfo>\n" +
           "  <SignatureValue>" + getSignature( key, uuid ) + "  </SignatureValue>" +
           "  <Object>\n" + getConfigurationString( uuid ) + "\n</Object>" +
           "</Signature>";

  }

  private static String getConfigurationString( String uuid ) {
    return
        "<CloudSchema>\n" +
        "  <Services type=\"array\">\n" +
        "    <Service>\n" +
        "      <Name>ec2</Name>\n" +
        "      <EndpointUrl>" + getEucaUrl() + "</EndpointUrl>\n" +
        "      <Resources type=\"array\">\n" +
        "        <Resource>\n" +
        "          <Name>instances</Name>\n" +
        "        </Resource>\n" +
        "        <Resource>\n" +
        "          <Name>security_groups</Name>\n" +
        "        </Resource>\n" +
        "        <Resource>\n" +
        "          <Name>ssh_keys</Name>\n" +
        "        </Resource>\n" +
        "        <Resource>\n" +
        "          <Name>images</Name>\n" +
        "        </Resource>\n" +
        blockStorageConfiguration() +
        publicAddressConfiguration() +
        "      </Resources>\n" +
        "    </Service>\n" +
        "    <Service>\n" +
        "      <Name>s3</Name>\n" +
        "      <EndpointUrl>" + getStorageUrl() + "</EndpointUrl>\n" +
        "      <Resources type=\"array\">\n" +
        "        <Resource>\n" +
        "          <Name>buckets</Name>\n" +
        "        </Resource>\n" +
        "        <Resource>\n" +
        "          <Name>keys</Name>\n" +
        "        </Resource>\n" +
        "      </Resources>\n" +
        "    </Service>\n" +
        "  </Services>\n" +
        "  <id>" + uuid + "</id>" +
        "  <CloudType>eucalyptus</CloudType>\n" +
        "  <CloudVersion>1.5.2</CloudVersion>\n" +
        "  <SchemaVersion>1.0</SchemaVersion>\n" +
        "  <Description>Public cloud in the new cluster</Description>\n" +
        "</CloudSchema>\n";
  }

  private static String blockStorageConfiguration() {
    if ( EucalyptusProperties.disableBlockStorage ) {
      return "        <Resource>\n" +
             "          <Name>ebs_snapshots</Name>\n" +
             "        </Resource>\n" +
             "        <Resource>\n" +
             "          <Name>ebs_volumes</Name>\n" +
             "        </Resource>\n";
    } else {
      return "";
    }
  }

  private static String publicAddressConfiguration() {
    if ( EucalyptusProperties.disableNetworking ) {
      return "        <Resource>\n" +
             "          <Name>elastic_ips</Name>\n" +
             "        </Resource>\n";
    } else {
      return "";
    }
  }

  private static String getStorageUrl() {
    try {
      return EucalyptusProperties.getSystemConfiguration().getStorageUrl();
    } catch ( EucalyptusCloudException e ) {
      return "configuration error";
    }
  }

  private static String getRegistrationId() {
    try {
      return EucalyptusProperties.getSystemConfiguration().getRegistrationId();
    } catch ( EucalyptusCloudException e ) {
      return "configuration error";
    }
  }


  private static String getEucaUrl() {
    try {
      return EucalyptusProperties.getSystemConfiguration().getStorageUrl().replaceAll( "/services/Walrus", "/services/Eucalyptus" );
    } catch ( EucalyptusCloudException e ) {
      return "configuration error";
    }
  }

  @Override
  protected void doGet( final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse ) throws ServletException, IOException {
    String uuid = UUID.randomUUID().toString();
    String registrationId = getRegistrationId();
    ServletOutputStream op = httpServletResponse.getOutputStream();
    LOG.info( "Using registrationId: " + registrationId );
    op.write( getMessage( registrationId, UUID.randomUUID().toString() ).getBytes( ) );
    op.flush();
  }

  private static String getSignature( String key, String uuid ) {
    SecretKeySpec signingKey = new SecretKeySpec( key.getBytes(), Hashes.Mac.HmacSHA256.toString() );
    try {
      Mac mac = Mac.getInstance( Hashes.Mac.HmacSHA256.toString() );
      mac.init( signingKey );
      byte[] rawHmac = mac.doFinal( uuid.getBytes() );
      LOG.warn("\nkey='"+key+"'\nid='"+uuid+"'\nresult="+Hashes.getHexString( rawHmac ));
      return Hashes.getHexString( rawHmac );
    }
    catch ( Exception e ) {
      LOG.error( e );
      return "error: " + e.getMessage();
    }

  }
}
