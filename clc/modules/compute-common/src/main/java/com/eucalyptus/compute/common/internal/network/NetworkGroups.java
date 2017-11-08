/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/
package com.eucalyptus.compute.common.internal.network;

import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import org.hibernate.exception.ConstraintViolationException;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.compute.common.internal.identifier.ResourceIdentifiers;
import com.eucalyptus.compute.common.internal.util.DuplicateMetadataException;
import com.eucalyptus.compute.common.internal.util.MetadataException;
import com.eucalyptus.compute.common.internal.util.NoSuchMetadataException;
import com.eucalyptus.compute.common.internal.vpc.Vpc;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.PersistenceExceptions;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.records.Logs;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

/**
 *
 */
public class NetworkGroups {
  private static final String DEFAULT_NETWORK_NAME    = "default";
  private static final Logger LOG                     = Logger.getLogger( NetworkGroups.class );

  public static void createDefault( final OwnerFullName ownerFullName ) throws MetadataException {
    try ( final TransactionResource tx = Entities.transactionFor( Vpc.class ) ) {
      if ( Iterables.tryFind(
          Entities.query( Vpc.exampleDefault( ownerFullName.getAccountNumber() ) ),
          Predicates.alwaysTrue()
      ).isPresent( ) ) {
        return; // skip default security group creation when there is a default VPC
      }
    }

    try {
      try {
        NetworkGroup net = Transactions.find( NetworkGroup.named( AccountFullName.getInstance( ownerFullName.getAccountNumber() ), DEFAULT_NETWORK_NAME ) );
        if ( net == null ) {
          create( ownerFullName, DEFAULT_NETWORK_NAME, "default group" );
        }
      } catch ( NoSuchElementException | TransactionException ex ) {
        try {
          create( ownerFullName, DEFAULT_NETWORK_NAME, "default group" );
        } catch ( ConstraintViolationException ex1 ) {}
      }
    } catch ( DuplicateMetadataException ex ) {}
  }

  public static String defaultNetworkName( ) {
    return DEFAULT_NETWORK_NAME;
  }

  public static NetworkGroup create( final OwnerFullName ownerFullName,
                                     final String groupName,
                                     final String groupDescription ) throws MetadataException {
    return create( ownerFullName, null, groupName, groupDescription );
  }

  public static NetworkGroup create( final OwnerFullName ownerFullName,
                                     final Vpc vpc,
                                     final String groupName,
                                     final String groupDescription ) throws MetadataException {
    UserFullName userFullName = null;
    if ( ownerFullName instanceof UserFullName ) {
      userFullName = ( UserFullName ) ownerFullName;
    } else {
      try {
        User admin = Accounts.lookupPrincipalByAccountNumber( ownerFullName.getAccountNumber( ) );
        userFullName = UserFullName.getInstance( admin );
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
        throw new NoSuchMetadataException( "Failed to create group because owning user could not be identified.", ex );
      }
    }

    final String resourceDesc = groupName + ( vpc != null ? " in " + vpc.getDisplayName( ) : "" ) +
        " for " + userFullName.toString( );
    try ( final TransactionResource db = Entities.transactionFor( NetworkGroup.class ) ) {
      try {
        Entities.uniqueResult( NetworkGroup.withUniqueName(
            userFullName.asAccountFullName( ),
            CloudMetadatas.toDisplayName().apply( vpc ),
            groupName ) );
        throw new DuplicateMetadataException( "Failed to create group: " + resourceDesc );
      } catch ( final NoSuchElementException ex ) {
        final NetworkGroup entity = Entities.persist( NetworkGroup.create( userFullName, vpc, ResourceIdentifiers.generateString( NetworkGroup.ID_PREFIX ), groupName, groupDescription ) );
        db.commit();
        return entity;
      }
    } catch ( final ConstraintViolationException ex ) {
      Logs.exhaust().error( ex );
      throw new DuplicateMetadataException( "Failed to create group: " + resourceDesc, ex );
    } catch ( final Exception ex ) {
      Logs.exhaust( ).error( ex, ex );
      throw new MetadataException( "Failed to create group: " + resourceDesc, PersistenceExceptions.transform( ex ) );
    }
  }

}
