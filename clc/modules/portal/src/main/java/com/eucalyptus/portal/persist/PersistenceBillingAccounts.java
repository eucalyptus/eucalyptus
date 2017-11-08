/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
