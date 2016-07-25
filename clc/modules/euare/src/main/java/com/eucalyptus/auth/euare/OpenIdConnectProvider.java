/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
package com.eucalyptus.auth.euare;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.euare.persist.entities.OpenIdProviderEntity;
import com.eucalyptus.auth.euare.principal.EuareOpenIdConnectProvider;
import com.eucalyptus.component.auth.SystemCredentials;
import com.eucalyptus.component.id.Euare;
import com.eucalyptus.crypto.Ciphers;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.crypto.util.PEMFiles;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.util.RestrictedTypes.Resolver;
import com.google.common.base.Charsets;
import com.google.common.base.Function;

/**
 * @author dkavanagh
 * 
 */
public class OpenIdConnectProvider {
  private static Logger LOG = Logger.getLogger( OpenIdConnectProvider.class );

  public static boolean isUrlValid(final String url) {
    try{
      if(!url.startsWith("https://")) {
        throw new Exception("Malformed url");
      }
      // TODO: validate url more, (try HEAD)?
      return true;
    }catch(final Exception ex) {
      return false;
    }
     
  }

  @Resolver( OpenIdProviderEntity.class )
  public enum Lookup implements Function<String, OpenIdProviderEntity> {
    INSTANCE;
    @Override
    public OpenIdProviderEntity apply( final String url ) {
      try{
        OpenIdProviderEntity found = null;
        try ( final TransactionResource db = Entities.transactionFor( OpenIdProviderEntity.class ) ) {
          found = Entities.criteriaQuery(OpenIdProviderEntity.named( url )
          ).uniqueResult( );
          db.rollback();
        } catch(final NoSuchElementException ex){
          ;
        } catch(final Exception ex){
          throw ex;
        }
        return found;
      }catch(final Exception ex){
        throw Exceptions.toUndeclared(ex);
      }
    }
  }
  
  public static void updateOpenIdConnectProvider(final String url, final List<String> thumbprintList)
      throws NoSuchElementException, AuthException {
    try ( final TransactionResource db = Entities.transactionFor( OpenIdProviderEntity.class ) ) {
      final OpenIdProviderEntity found = Entities.criteriaQuery(
          OpenIdProviderEntity.named( url )
      ).uniqueResult( );
      try {
        if (thumbprintList != null && thumbprintList.size() > 0)
          found.getThumbprints().clear();
          found.getThumbprints().addAll(thumbprintList);
      } catch (final Exception ex) {
        throw new AuthException(AuthException.INVALID_OPENID_PROVIDER_URL);
      }
      Entities.persist(found);
      db.commit();
    } catch (final Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }  
}
