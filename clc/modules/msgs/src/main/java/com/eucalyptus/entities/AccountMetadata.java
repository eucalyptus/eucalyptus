/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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

package com.eucalyptus.entities;

import java.sql.SQLException;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.system.Ats;
import com.eucalyptus.upgrade.Upgrades;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.HasFullName;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.auth.type.RestrictedType.AccountRestrictedType;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import groovy.sql.Sql;

@MappedSuperclass
public abstract class AccountMetadata<STATE extends Enum<STATE>> extends AbstractStatefulPersistent<STATE> implements AccountRestrictedType, HasFullName<AccountMetadata> {
  @Column( name = "metadata_account_id" )
  private String          ownerAccountNumber;
  @Column( name = "metadata_unique_name", unique = true, nullable = false, length = 300 )
  private String          uniqueName;
  @Transient
  protected OwnerFullName ownerFullNameCached = null;
  
  /**
   * GRZE:NOTE: Should only /ever/ be used by sub classes.
   */
  protected AccountMetadata( ) {}
  
  /**
   * GRZE:NOTE: Should only /ever/ be used by sub classes.
   */
  protected AccountMetadata( OwnerFullName owner ) {
    this.ownerAccountNumber = owner != null
      ? owner.getAccountNumber( )
      : null;
  }
  
  public AccountMetadata( OwnerFullName owner, String displayName ) {
    super( displayName );
    this.ownerAccountNumber = owner != null
      ? owner.getAccountNumber( )
      : null;
  }
  
  public OwnerFullName getOwner( ) {
    if ( this.ownerFullNameCached != null ) {
      return this.ownerFullNameCached;
    } else if ( this.getOwnerAccountNumber( ) != null ) {
      OwnerFullName tempOwner;
      if ( Principals.nobodyFullName( ).getAccountNumber( ).equals( this.getOwnerAccountNumber( ) ) ) {
        tempOwner = Principals.nobodyFullName( );
      } else if ( Principals.systemFullName( ).getAccountNumber( ).equals( this.getOwnerAccountNumber( ) ) ) {
        tempOwner = Principals.systemFullName( );
      } else {
        tempOwner = AccountFullName.getInstance( this.getOwnerAccountNumber( ) );
      }
      return ( this.ownerFullNameCached = tempOwner );
    } else {
      throw new RuntimeException( "Failed to identify user with id " + this.ownerAccountNumber + " something has gone seriously wrong." );
    }
  }
  
  @Override
  public String getOwnerAccountNumber( ) {
    return this.ownerAccountNumber;
  }
  
  protected void setOwnerAccountNumber( String ownerAccountId ) {
    this.ownerAccountNumber = ownerAccountId;
  }
  
  protected void setOwner( OwnerFullName owner ) {
    this.ownerFullNameCached = null;
    this.setOwnerAccountNumber( owner != null
        ? owner.getAccountNumber()
        : null );
  }
  
  protected String getUniqueName( ) {
    if ( this.uniqueName == null ) {
      return this.uniqueName = this.createUniqueName( );
    } else {
      return this.uniqueName;
    }
  }
  
  protected void setUniqueName( String uniqueName ) {
    this.uniqueName = uniqueName;
  }
  
  @PrePersist
  private void generateOnCommit( ) {
    this.uniqueName = createUniqueName( );
  }
  
  protected String createUniqueName( ) {
    return ( this.ownerAccountNumber != null && this.getDisplayName( ) != null )
      ? this.ownerAccountNumber + ":" + this.getDisplayName( )
      : null;
  }
  
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 0;
    result = prime * result + ( ( this.uniqueName == null )
      ? 0
      : this.uniqueName.hashCode( ) );
    return result;
  }
  
  @Override
  public boolean equals( Object obj ) {
    if ( this == obj ) {
      return true;
    }
    if ( getClass( ) != obj.getClass( ) ) {
      return false;
    }
    AccountMetadata other = ( AccountMetadata ) obj;
    if ( this.uniqueName == null ) {
      if ( other.uniqueName != null ) {
        return false;
      }
    } else if ( !this.uniqueName.equals( other.uniqueName ) ) {
      return false;
    }
    return true;
  }
  
  @Override
  public int compareTo( AccountMetadata that ) {
    return this.getFullName( ).toString( ).compareTo( that.getFullName( ).toString( ) );
  }

  public static Function<AccountMetadata,String> accountNumber( ) {
    return FilterFunctions.ACCOUNT_ID;
  }

  private enum FilterFunctions implements Function<AccountMetadata,String> {
    ACCOUNT_ID {
      @Override
      public String apply( final AccountMetadata accountMetadata ) {
        return accountMetadata.getOwnerAccountNumber();
      }
    },
  }

  public abstract static class AccountName400UpgradeSupport {
    private static final Logger logger = Logger.getLogger( AccountName400UpgradeSupport.class );

    private static final String SQL_DROP_NAME_COLUMN = "alter table %s drop column if exists metadata_account_name";

    /**
     * Get the non-empty list of classes for upgrade.
     *
     * Classes must belong to the same persistence context.
     */
    protected abstract List<Class<? extends AccountMetadata>> getAccountMetadataClasses( );

    public Boolean call( ) throws Exception {
      final List<Class<? extends AccountMetadata>> accountMetadataClasses = getAccountMetadataClasses( );
      final Class<? extends AccountMetadata> contextClass = Iterables.get( accountMetadataClasses, 0 );
      final String context = Ats.inClassHierarchy( contextClass ).get( PersistenceContext.class ).name( );
      Sql sql = null;
      try {
        sql = Upgrades.DatabaseFilters.NEWVERSION.getConnection( context );
        for ( final Class<? extends AccountMetadata> accountMetadataClass : accountMetadataClasses ) {
          final int updated;
          try {
            updated = sql.executeUpdate(
                String.format( SQL_DROP_NAME_COLUMN, Ats.from( accountMetadataClass ).get( Table.class ).name( ) )
            );
            logger.info( "Cleared account alias for " + updated + " " + accountMetadataClass.getSimpleName( ) + "(s)" );
          } catch ( final SQLException e ) {
            throw Exceptions.toUndeclared( "Error clearing account alias for " + accountMetadataClass.getSimpleName( ), e );
          }
        }
        return true;
      } catch ( final Exception e ) {
        logger.error( e, e );
        return false;
      } finally {
        if ( sql != null ) {
          sql.close( );
        }
      }
    }
  }
}
