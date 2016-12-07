/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.portal.persist;

import java.util.function.Function;
import javax.annotation.Nullable;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.entities.AbstractPersistentSupport;
import com.eucalyptus.portal.BillingAccount;
import com.eucalyptus.portal.BillingAccounts;
import com.eucalyptus.portal.PortalMetadataException;
import com.eucalyptus.portal.PortalMetadataNotFoundException;
import com.eucalyptus.portal.common.PortalMetadata;
import com.eucalyptus.util.CompatFunction;
import com.eucalyptus.util.Exceptions;

/**
 *
 */
@ComponentNamed
public class PersistenceBillingAccounts
    extends AbstractPersistentSupport<PortalMetadata.BillingAccountMetadata,BillingAccount,PortalMetadataException>
    implements BillingAccounts {

  protected PersistenceBillingAccounts( ) {
    super( "account" );
  }

  @SuppressWarnings( "ThrowableResultOfMethodCallIgnored" )
  @Override
  protected PortalMetadataException notFoundException( final String message, final Throwable cause ) {
    final PortalMetadataNotFoundException exception = Exceptions.findCause( cause, PortalMetadataNotFoundException.class );
    if ( exception != null ) {
      return exception;
    }
    return new PortalMetadataNotFoundException( message, cause );
  }

  @Override
  protected PortalMetadataException metadataException( final String message, final Throwable cause ) {
    final PortalMetadataException exception = Exceptions.findCause( cause, PortalMetadataException.class );
    if ( exception != null ) {
      return exception;
    }
    return new PortalMetadataException( message, cause );
  }

  @Override
  protected BillingAccount exampleWithOwner( final OwnerFullName ownerFullName ) {
    final BillingAccount billingAccount = new BillingAccount( );
    billingAccount.setOwner( ownerFullName );
    return billingAccount;
  }

  @Override
  protected BillingAccount exampleWithName( final OwnerFullName ownerFullName, final String name ) {
    final BillingAccount billingAccount = new BillingAccount( );
    billingAccount.setOwner( ownerFullName );
    billingAccount.setDisplayName( name );
    return billingAccount;
  }

  @Override
  public <T> T lookupByAccount( final String accountNumber,
                                @Nullable final OwnerFullName ownerFullName,
                                final Function<? super BillingAccount, T> transform
  ) throws PortalMetadataException {
    return lookupByName( ownerFullName, accountNumber, CompatFunction.of( transform ) );
  }

  @Override
  public <T> T updateByAccount( final String accountNumber,
                                @Nullable final OwnerFullName ownerFullName,
                                final Function<? super BillingAccount, T> updateTransform
  ) throws PortalMetadataException {
    return withRetries( ).updateByExample(
        exampleWithName( ownerFullName, accountNumber ),
        ownerFullName,
        accountNumber,
        CompatFunction.of( updateTransform )
    );
  }

  @Override
  public <T> T save( final BillingAccount account,
                     final Function<? super BillingAccount, T> transform ) throws PortalMetadataException {
    return transform.apply( save( account ) );
  }
}
