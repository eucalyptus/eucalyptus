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
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.ws.handlers.wssecurity;

import java.security.GeneralSecurityException;
import java.util.Collection;

import org.apache.ws.security.WSEncryptionPart;

import com.eucalyptus.auth.SystemCredentialProvider;
import com.eucalyptus.auth.util.EucaKeyStore;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.util.EucalyptusProperties;
import com.eucalyptus.ws.util.CredentialProxy;
import com.google.common.collect.Lists;

public class ClusterWsSecHandler extends WsSecHandler {
  private static final String WSA_NAMESPACE = "http://www.w3.org/2005/08/addressing";

  public ClusterWsSecHandler( ) throws GeneralSecurityException {
    super( new CredentialProxy( SystemCredentialProvider.getCredentialProvider( Component.eucalyptus ).getCertificate( ), SystemCredentialProvider.getCredentialProvider( Component.eucalyptus ).getPrivateKey( ) ) );
  }

  @Override
  public Collection<WSEncryptionPart> getSignatureParts() {
    return Lists.newArrayList( new WSEncryptionPart( "To", WSA_NAMESPACE, "Content" ),new WSEncryptionPart( "MessageID", WSA_NAMESPACE, "Content" ), new WSEncryptionPart( "Action", WSA_NAMESPACE, "Content" ) );
  }
  
  

  @Override
  public boolean shouldTimeStamp() {
    return false;
  }

}
