/*************************************************************************
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
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.entities;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.annotation.Nullable;
import org.hibernate.criterion.Criterion;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.auth.type.RestrictedType;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

/**
 * Support for persistence DAOs.
 *
 * @param <RT> The RestrictedType for the AbstractOwnedPersistent
 * @param <AP> The AbstractOwnedPersistent type
 * @param <PE> The base persistence exception type.
 */
public abstract class AbstractPersistentSupport<RT extends RestrictedType, AP extends AbstractPersistent & RestrictedType, PE extends Exception> {

  protected final String typeDescription;

  protected AbstractPersistentSupport( final String typeDescription ) {
    this.typeDescription = typeDescription;
  }

  public <T> T lookupByName( @Nullable final OwnerFullName ownerFullName,
                             final String name,
                             final Function<? super AP,T> transform ) throws PE {
    return lookupByName( ownerFullName, name, Predicates.alwaysTrue(), transform );
  }

  public <T> T lookupByName( @Nullable final OwnerFullName ownerFullName,
                             final String name,
                             final Predicate<? super AP> filter,
                             final Function<? super AP,T> transform ) throws PE {
    return lookupByExample( exampleWithName( ownerFullName, name ), ownerFullName, name, filter, transform );
  }

  public <T> T lookupByExample( final AP example,
                                @Nullable final OwnerFullName ownerFullName,
                                final String key,
                                final Predicate<? super AP> filter,
                                final Function<? super AP,T> transform ) throws PE {
    try {
      return Transactions.one( example, filter, transform );
    } catch ( NoSuchElementException e ) {
      throw notFoundException( qualifyOwner("Unable to find "+typeDescription+" '"+key+"'", ownerFullName), e );
    } catch ( Exception e ) {
      throw metadataException( qualifyOwner("Error finding "+typeDescription+" '"+key+"'", ownerFullName), e );
    }
  }

  public List<AP> list( final OwnerFullName ownerFullName ) throws PE {
    try {
      return Transactions.findAll( exampleWithOwner( ownerFullName ) );
    } catch ( Exception e ) {
      throw metadataException( qualifyOwner( "Failed to find "+typeDescription+"s", ownerFullName ), e );
    }
  }

  public <T> List<T> list( final OwnerFullName ownerFullName,
                           final Predicate<? super AP> filter,
                           final Function<? super AP,T> transform ) throws PE {
    try {
      return Transactions.filteredTransform( exampleWithOwner( ownerFullName ), filter, transform );
    } catch ( Exception e ) {
      throw metadataException( qualifyOwner( "Failed to find "+typeDescription+"s", ownerFullName ), e );
    }
  }

  public <T> List<T> list( final OwnerFullName ownerFullName,
                           final Criterion criterion,
                           final Map<String,String> aliases,
                           final Predicate<? super AP> filter,
                           final Function<? super AP,T> transform ) throws PE {
    try {
      return Transactions.filteredTransform( exampleWithOwner( ownerFullName ), criterion, aliases, filter, transform );
    } catch ( Exception e ) {
      throw metadataException( qualifyOwner( "Failed to find "+typeDescription+"s", ownerFullName ), e );
    }
  }

  public <T> List<T> listByExample( final AP example,
                                    final Predicate<? super AP> filter,
                                    final Function<? super AP,T> transform ) throws PE {
    try {
      return Transactions.filteredTransform( example, filter, transform );
    } catch ( Exception e ) {
      throw metadataException( "Failed to find "+typeDescription+"s by example: " + LogUtil.dumpObject( example ), e );
    }
  }

  public <T> List<T> listByExample( final AP example,
                                    final Predicate<? super AP> filter,
                                    final Criterion criterion,
                                    final Map<String,String> aliases,
                                    final Function<? super AP,T> transform ) throws PE {
    try {
      return Transactions.filteredTransform( example, criterion, aliases, filter, transform );
    } catch ( Exception e ) {
      throw metadataException( "Failed to find "+typeDescription+"s by example: " + LogUtil.dumpObject(example), e );
    }
  }

  public long countByExample( final AP example ) throws PE {
    try {
      return Entities.count( example );
    } catch ( Exception e ) {
      throw metadataException( "Failed to count "+typeDescription+"s by example: " + LogUtil.dumpObject( example ), e );
    }
  }

  public long countByExample( final AP example,
                              final Criterion criterion,
                              final Map<String,String> aliases ) throws PE {
    try {
      return Entities.count( example, criterion, aliases );
    } catch ( Exception e ) {
      throw metadataException( "Failed to count "+typeDescription+"s by example: " + LogUtil.dumpObject(example), e );
    }
  }


  public AP updateByExample( final AP example,
                              final OwnerFullName ownerFullName,
                              final String key,
                              final Callback<AP> updateCallback ) throws PE {
    try {
      return Transactions.one( example, updateCallback );
    } catch ( NoSuchElementException e ) {
      throw notFoundException( qualifyOwner( "Unable to find "+typeDescription+" '"+key+"'", ownerFullName ), e );
    } catch ( Exception e ) {
      throw metadataException( qualifyOwner( "Error updating "+typeDescription+" '"+key+"'", ownerFullName ), e );
    }
  }

  public <T> T updateByExample( final AP example,
                                final OwnerFullName ownerFullName,
                                final String key,
                                final Function<? super AP,T> updateTransform ) throws PE {
    try {
      return Transactions.one( example, updateTransform );
    } catch ( NoSuchElementException e ) {
      throw notFoundException( qualifyOwner( "Unable to find "+typeDescription+" '"+key+"'", ownerFullName ), e );
    } catch ( Exception e ) {
      throw metadataException( qualifyOwner( "Error updating "+typeDescription+" '"+key+"'", ownerFullName ), e );
    }
  }

  public <T> T updateByExample( final AP example,
                                final Criterion criterion,
                                final Map<String,String> aliases,
                                final OwnerFullName ownerFullName,
                                final String key,
                                final Function<? super AP,T> updateTransform ) throws PE {
    try {
      return Transactions.one( example, criterion, aliases, Predicates.alwaysTrue( ), updateTransform );
    } catch ( NoSuchElementException e ) {
      throw notFoundException( qualifyOwner( "Unable to find "+typeDescription+" '"+key+"'", ownerFullName ), e );
    } catch ( Exception e ) {
      throw metadataException( qualifyOwner( "Error updating "+typeDescription+" '"+key+"'", ownerFullName ), e );
    }
  }

  public AP save( final AP natGateway ) throws PE {
    try {
      return Transactions.saveDirect( natGateway );
    } catch ( Exception e ) {
      throw metadataException( "Error creating "+typeDescription+" '"+ natGateway.getDisplayName()+"'", e );
    }
  }

  public boolean delete( final RT metadata ) throws PE {
    return delete( metadata, Predicates.<AP>alwaysTrue( ) );
  }

  public boolean delete( final RT metadata, final Predicate<? super AP> precondition ) throws PE {
    try {
      final String accountNumber = metadata instanceof RestrictedType.AccountRestrictedType ?
          ( (RestrictedType.AccountRestrictedType) metadata ).getOwnerAccountNumber( ) :
          metadata.getOwner( ).getAccountNumber( );
      return Transactions.delete(
          exampleWithName(
              AccountFullName.getInstance( accountNumber ),
              metadata.getDisplayName( ) ),
          precondition );
    } catch ( NoSuchElementException e ) {
      return false;
    } catch ( Exception e ) {
      throw metadataException( "Error deleting "+typeDescription+" '"+describe( metadata )+"'", e );
    }
  }

  public List<AP> deleteByExample( final AP example ) throws PE {
    try {
      return Transactions.each( example, new Callback<AP>(){
        @Override
        public void fire( final AP input ) {
          Entities.delete( input );
        }
      } );
    } catch ( Exception e ) {
      throw metadataException( "Error deleting "+typeDescription+"s by example: "+LogUtil.dumpObject( example ), e );
    }
  }

  public List<AP> deleteByExample( final AP example,
                                   final Criterion criterion,
                                   final Map<String,String> aliases ) throws PE {
    try {
      return Transactions.each( example, criterion, aliases, new Callback<AP>(){
        @Override
        public void fire( final AP input ) {
          Entities.delete( input );
        }
      } );
    } catch ( Exception e ) {
      throw metadataException( "Error deleting "+typeDescription+"s by example: "+LogUtil.dumpObject( example ), e );
    }
  }


  protected String describe( final RestrictedType metadata ) {
    return metadata.getDisplayName();
  }

  protected abstract PE notFoundException( String message, Throwable cause );

  protected abstract PE metadataException( String message, Throwable cause );

  protected abstract AP exampleWithOwner( OwnerFullName ownerFullName );

  protected abstract AP exampleWithName( OwnerFullName ownerFullName, String name );

  protected String qualifyOwner( final String text, final OwnerFullName ownerFullName ) {
    return ownerFullName == null ? text : text + " for " + ownerFullName;
  }

  public static Function<AbstractPersistent,Date> creation( ) {
    return AbstractPersistentDateFunctions.CREATION;
  }

  public static Function<AbstractPersistent,Date> lastUpdate( ) {
    return AbstractPersistentDateFunctions.LAST_UPDATE;
  }

  public AbstractPersistentSupport<RT, AP, PE> withRetries( ) {
    return withRetries( Entities.CONCURRENT_UPDATE_RETRIES );
  }

  public AbstractPersistentSupport<RT, AP, PE> withRetries( final int retries ) {
    return new RetryingAbstractPersistentSupport<>( this, retries );
  }

  private static class RetryingAbstractPersistentSupport<RT extends RestrictedType, AP extends AbstractPersistent & RestrictedType, PE extends Exception> extends DelegatingAbstractPersistentSupport<RT, AP, PE> {
    private final int retries;

    private RetryingAbstractPersistentSupport( final AbstractPersistentSupport<RT, AP, PE> delegate,
                                               final int retries ) {
      super( delegate );
      this.retries = retries;
    }

    @Override
    public <T> T updateByExample( final AP example, final OwnerFullName ownerFullName, final String key, final Function<? super AP, T> updateTransform ) throws PE {
      return updateWithRetries( example.getClass(), new Function<Void, T>() {
        @Override
        public T apply( @Nullable final Void nothing ) {
          try {
            return RetryingAbstractPersistentSupport.super.updateByExample( example, ownerFullName, key, updateTransform );
          } catch ( final Exception e ) {
            throw Exceptions.toUndeclared( e );
          }
        }
      }, ownerFullName, key );
    }

    @Override
    public AP updateByExample( final AP example, final OwnerFullName ownerFullName, final String key, final Callback<AP> updateCallback ) throws PE {
      return updateWithRetries( example.getClass( ), new Function<Void, AP>() {
        @Override
        public AP apply( @Nullable final Void nothing ) {
          try {
            return RetryingAbstractPersistentSupport.super.updateByExample( example, ownerFullName, key, updateCallback );
          } catch ( final Exception e ) {
            throw Exceptions.toUndeclared( e );
          }
        }
      }, ownerFullName, key );
    }

    @Override
    public <T> T updateByExample( final AP example, final Criterion criterion, final Map<String, String> aliases, final OwnerFullName ownerFullName, final String key, final Function<? super AP, T> updateTransform ) throws PE {
      return updateWithRetries( example.getClass( ), new Function<Void, T>() {
        @Override
        public T apply( @Nullable final Void nothing ) {
          try {
            return RetryingAbstractPersistentSupport.super.updateByExample( example, criterion, aliases, ownerFullName, key, updateTransform );
          } catch ( final Exception e ) {
            throw Exceptions.toUndeclared( e );
          }
        }
      }, ownerFullName, key );
    }

    @Override
    public List<AP> deleteByExample( final AP example ) throws PE {
      return deleteWithRetries( example.getClass( ), new Function<Void, List<AP>>() {
        @Override
        public List<AP> apply( @Nullable final Void nothing ) {
          try {
            return RetryingAbstractPersistentSupport.super.deleteByExample( example );
          } catch ( final Exception e ) {
            throw Exceptions.toUndeclared( e );
          }
        }
      }, example );
    }

    @Override
    public List<AP> deleteByExample( final AP example, final Criterion criterion, final Map<String, String> aliases ) throws PE {
      return deleteWithRetries( example.getClass( ), new Function<Void, List<AP>>() {
        @Override
        public List<AP> apply( @Nullable final Void nothing ) {
          try {
            return RetryingAbstractPersistentSupport.super.deleteByExample( example, criterion, aliases );
          } catch ( final Exception e ) {
            throw Exceptions.toUndeclared( e );
          }
        }
      }, example );
    }

    @Override
    public AbstractPersistentSupport<RT, AP, PE> withRetries( ) {
      return this;
    }

    @Override
    public AbstractPersistentSupport<RT, AP, PE> withRetries( final int retries ) {
      return this;
    }

    private <T> T updateWithRetries( final Class<?> type,
                                     final Function<Void, T> updateFunction,
                                     final OwnerFullName ownerFullName,
                                     final String key ) throws PE {
      try {
        return Entities.asTransaction( type, updateFunction, retries ).apply( null );
      } catch ( final Exception e ) {
        throw metadataException( qualifyOwner( "Error updating "+typeDescription+" '"+key+"'", ownerFullName ), e );
      }
    }

    private <T> T deleteWithRetries( final Class<?> type,
                                     final Function<Void, T> deleteFunction,
                                     final AP example ) throws PE {
      try {
        return Entities.asTransaction( type, deleteFunction, retries ).apply( null );
      } catch ( final Exception e ) {
        throw metadataException( "Error deleting "+typeDescription+"s by example: "+LogUtil.dumpObject( example ), e );
      }
    }
  }

  private static class DelegatingAbstractPersistentSupport<RT extends RestrictedType, AP extends AbstractPersistent & RestrictedType, PE extends Exception> extends AbstractPersistentSupport<RT, AP, PE> {
    private final AbstractPersistentSupport<RT, AP, PE> delegate;

    private DelegatingAbstractPersistentSupport( final AbstractPersistentSupport<RT, AP, PE> delegate ) {
      super( delegate.typeDescription );
      this.delegate = delegate;
    }

    public <T> T lookupByName( @Nullable final OwnerFullName ownerFullName, final String name, final Function<? super AP, T> transform ) throws PE {
      return delegate.lookupByName( ownerFullName, name, transform );
    }

    @Override
    public <T> T lookupByName( @Nullable final OwnerFullName ownerFullName, final String name, final Predicate<? super AP> filter, final Function<? super AP, T> transform ) throws PE {
      return delegate.lookupByName( ownerFullName, name, filter, transform );
    }

    public <T> T lookupByExample( final AP example, @Nullable final OwnerFullName ownerFullName, final String key, final Predicate<? super AP> filter, final Function<? super AP, T> transform ) throws PE {
      return delegate.lookupByExample( example, ownerFullName, key, filter, transform );
    }

    @Override
    public List<AP> list( final OwnerFullName ownerFullName ) throws PE {
      return delegate.list( ownerFullName );
    }

    public <T> List<T> list( final OwnerFullName ownerFullName, final Predicate<? super AP> filter, final Function<? super AP, T> transform ) throws PE {
      return delegate.list( ownerFullName, filter, transform );
    }

    public <T> List<T> list( final OwnerFullName ownerFullName, final Criterion criterion, final Map<String, String> aliases, final Predicate<? super AP> filter, final Function<? super AP, T> transform ) throws PE {
      return delegate.list( ownerFullName, criterion, aliases, filter, transform );
    }

    public <T> List<T> listByExample( final AP example, final Predicate<? super AP> filter, final Function<? super AP, T> transform ) throws PE {
      return delegate.listByExample( example, filter, transform );
    }

    public <T> List<T> listByExample( final AP example, final Predicate<? super AP> filter, final Criterion criterion, final Map<String, String> aliases, final Function<? super AP, T> transform ) throws PE {
      return delegate.listByExample( example, filter, criterion, aliases, transform );
    }

    public long countByExample( final AP example ) throws PE {
      return delegate.countByExample( example );
    }

    public long countByExample( final AP example, final Criterion criterion, final Map<String, String> aliases ) throws PE {
      return delegate.countByExample( example, criterion, aliases );
    }

    public AP updateByExample( final AP example, final OwnerFullName ownerFullName, final String key, final Callback<AP> updateCallback ) throws PE {
      return delegate.updateByExample( example, ownerFullName, key, updateCallback );
    }

    public <T> T updateByExample( final AP example, final OwnerFullName ownerFullName, final String key, final Function<? super AP, T> updateTransform ) throws PE {
      return delegate.updateByExample( example, ownerFullName, key, updateTransform );
    }

    @Override
    public <T> T updateByExample( final AP example, final Criterion criterion, final Map<String, String> aliases, final OwnerFullName ownerFullName, final String key, final Function<? super AP, T> updateTransform ) throws PE {
      return delegate.updateByExample( example, criterion, aliases, ownerFullName, key, updateTransform );
    }

    public AP save( final AP metadata ) throws PE {
      return delegate.save( metadata );
    }

    public boolean delete( final RT metadata ) throws PE {
      return delegate.delete( metadata );
    }

    @Override
    public boolean delete( final RT metadata, final Predicate<? super AP> precondition ) throws PE {
      return delegate.delete( metadata, precondition );
    }

    public List<AP> deleteByExample( final AP example ) throws PE {
      return delegate.deleteByExample( example );
    }

    public List<AP> deleteByExample( final AP example, final Criterion criterion, final Map<String, String> aliases ) throws PE {
      return delegate.deleteByExample( example, criterion, aliases );
    }

    @Override
    public String describe( final RestrictedType metadata ) {
      return delegate.describe( metadata );
    }

    @Override
    public PE notFoundException( final String message, final Throwable cause ) {
      return delegate.notFoundException( message, cause );
    }

    @Override
    public PE metadataException( final String message, final Throwable cause ) {
      return delegate.metadataException( message, cause );
    }

    @Override
    public AP exampleWithOwner( final OwnerFullName ownerFullName ) {
      return delegate.exampleWithOwner( ownerFullName );
    }

    @Override
    public AP exampleWithName( final OwnerFullName ownerFullName, final String name ) {
      return delegate.exampleWithName( ownerFullName, name );
    }

    @Override
    public String qualifyOwner( final String text, final OwnerFullName ownerFullName ) {
      return delegate.qualifyOwner( text, ownerFullName );
    }

    public static Function<AbstractPersistent, Date> creation() {
      return AbstractPersistentSupport.creation();
    }

    public static Function<AbstractPersistent, Date> lastUpdate() {
      return AbstractPersistentSupport.lastUpdate();
    }

    @Override
    public AbstractPersistentSupport<RT, AP, PE> withRetries() {
      return delegate.withRetries();
    }

    @Override
    public AbstractPersistentSupport<RT, AP, PE> withRetries( final int retries ) {
      return delegate.withRetries( retries );
    }
  }

  private enum AbstractPersistentDateFunctions implements Function<AbstractPersistent,Date> {
    CREATION {
      @Nullable
      @Override
      public Date apply( @Nullable final AbstractPersistent abstractPersistent ) {
        return abstractPersistent == null ?
            null :
            abstractPersistent.getCreationTimestamp( );
      }
    },
    LAST_UPDATE {
      @Nullable
      @Override
      public Date apply( @Nullable final AbstractPersistent abstractPersistent ) {
        return abstractPersistent == null ?
            null :
            abstractPersistent.getLastUpdateTimestamp( );
      }
    },
  }

}
