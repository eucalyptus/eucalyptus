/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.transport.auth;

import edu.ucsb.eucalyptus.cloud.EucalyptusCloudException;
import edu.ucsb.eucalyptus.cloud.entities.CertificateInfo;
import edu.ucsb.eucalyptus.cloud.entities.EntityWrapper;
import edu.ucsb.eucalyptus.cloud.entities.UserInfo;
import edu.ucsb.eucalyptus.keys.AbstractKeyStore;
import edu.ucsb.eucalyptus.keys.UserKeyStore;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;
import edu.ucsb.eucalyptus.util.EucalyptusProperties;
import org.hibernate.Session;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.MatchMode;

import javax.persistence.EntityManager;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.List;

public class UserAuthentication extends CertAuthentication {

  public EucalyptusMessage authenticate( final X509Certificate cert, final EucalyptusMessage msg ) throws EucalyptusCloudException
  {
    try
    {
      return super.authenticate( cert, msg );
    }
    catch ( EucalyptusCloudException e )
    {
      AbstractKeyStore userKs = UserKeyStore.getInstance();
      String userAlias = null;
      try
      {
        userAlias = userKs.getCertificateAlias( cert );
      }
      catch ( GeneralSecurityException ex )
      {
        throw new EucalyptusCloudException( ex );
      }

      EntityManager em = EntityWrapper.getEntityManagerFactory(EucalyptusProperties.NAME).createEntityManager();
      Session session = ( Session ) em.getDelegate();
      UserInfo searchUser = new UserInfo();

      CertificateInfo searchCert = new CertificateInfo();
      searchCert.setCertAlias( userAlias );

      Example qbeUser = Example.create( searchUser ).enableLike( MatchMode.EXACT );
      Example qbeCert = Example.create( searchCert ).enableLike( MatchMode.EXACT );
      List<UserInfo> users =
          ( List<UserInfo> ) session
              .createCriteria( UserInfo.class )
              .add( qbeUser )
              .createCriteria( "certificates" )
              .add( qbeCert )
              .list();

      if ( users.size() > 1 )
      {
        em.close();
        throw new EucalyptusCloudException( "Certificate with more than one entities: " + userAlias );
      }
      else if ( users.size() < 1 )
      {
        em.close();
        throw new EucalyptusCloudException( "WS-Security screw up -- unauthorized user has authorized certificate: " + userAlias );
      }
      else
      {
        msg.setUserId( users.get( 0 ).getUserName() );
        if ( users.get( 0 ).isAdministrator() )
          msg.setEffectiveUserId( EucalyptusProperties.NAME );
        else
          msg.setEffectiveUserId( users.get( 0 ).getUserName() );
        em.close();
      }
      return msg;
    }
  }

}
