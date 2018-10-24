/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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

import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.AbstractQuery;
import javax.persistence.criteria.CommonAbstractCriteria;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Subquery;
import javax.persistence.metamodel.CollectionAttribute;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.transaction.Synchronization;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;
import org.hibernate.Transaction;
import org.hibernate.annotations.QueryHints;
import org.hibernate.collection.internal.AbstractPersistentCollection;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.transaction.spi.TransactionCoordinator;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.jpa.internal.EntityManagerFactoryImpl;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.sql.JoinType;
import com.eucalyptus.bootstrap.Databases;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Ats;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Classes;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.HasNaturalId;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.NonNullFunction;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import static com.eucalyptus.util.Parameters.checkParam;
import static org.hamcrest.Matchers.notNullValue;
import groovy.lang.Closure;

@ConfigurableClass( root = "bootstrap.tx",
                    description = "Parameters controlling transaction behaviour." )
public class Entities {
  @ConfigurableField( description = "Maximum number of times a transaction may be retried before giving up.",
                      initial = "10" )
  public static Integer                                          CONCURRENT_UPDATE_RETRIES = 10;
  private static final boolean                                   CLEANUP_TX_SESSION        = Boolean.valueOf( System.getProperty( "com.eucalyptus.entities.cleanupTxSession", "true" ) );
  private static Cache<String, String>                           txLog                     = CacheBuilder.newBuilder().weakKeys().softValues().build(); // No softKeys available for CacheBuilder
  private static Logger                                          LOG                       = Logger.getLogger( Entities.class );
  private static ThreadLocal<String>                             txRootThreadLocal         = new ThreadLocal<>( );
  private static ThreadLocal<ConcurrentMap<String, CascadingTx>> txStateThreadLocal        = new ThreadLocal<ConcurrentMap<String, CascadingTx>>( ) {

                                                                                             @Override
                                                                                             protected ConcurrentMap<String, CascadingTx> initialValue( ) {
                                                                                               return Maps.newConcurrentMap( );
                                                                                             }

                                                                                           };

  static String lookatPersistenceContext( final Object obj ) throws RuntimeException {
    final Class type = Classes.typeOf( obj );
    final Ats ats = Ats.inClassHierarchy( type );
    PersistenceContext persistenceContext;
    if ( !ats.has( PersistenceContext.class ) ) {
      throw new RuntimeException( "Attempting to create an entity wrapper instance for non persistent type: " + type
                                  + ".  Class hierarchy contains: \n" + ats.toString( ) );
    } else {
      persistenceContext = ats.get( PersistenceContext.class );
    }
    return persistenceContext.name( );
  }

  public static boolean hasTransaction( final Object obj ) {
    final String ctx = lookatPersistenceContext( obj );
    final CascadingTx tx = txStateThreadLocal.get( ).get( ctx );
    if ( tx == null ) {
      return false;
    } else if ( tx.isUsable( ) ) {
      return true;
    } else {
      cleanStrandedTx( tx );
      return false;
    }
  }

  private static CascadingTx getTransaction( final Object obj ) {
    if ( hasTransaction( obj ) ) {
      return txStateThreadLocal.get( ).get( lookatPersistenceContext( obj ) );
    } else {
      throw new NoSuchElementException( "Failed to find active transaction for persistence context: " + lookatPersistenceContext( obj ) + " and object: " + obj );
    }
  }

  public static void removeTransaction( final CascadingTx tx ) {
    final String txId = makeTxRootName( tx );
    txLog.invalidate( txStateThreadLocal.toString( ) + tx.getRecord( ).getPersistenceContext( ) );
    txStateThreadLocal.get( ).remove( tx.getRecord( ).getPersistenceContext( ) );
    if ( txId.equals( txStateThreadLocal.get( ) ) ) {
      for ( final Entry<String, CascadingTx> e : txStateThreadLocal.get( ).entrySet( ) ) {
        cleanStrandedTx( e.getValue( ) );
      }
      txStateThreadLocal.get( ).clear( );
      txStateThreadLocal.remove( );
    }
  }

  private static void cleanStrandedTx( final CascadingTx txValue ) {
    LOG.error( "Found stranded transaction: " + txValue.getRecord( ).getPersistenceContext( ) + " started at: " + txValue.getRecord( ).getStack( ) );
    try {
      txValue.rollback( );
    } catch ( final Exception ex ) {
      LOG.trace( ex, ex );
    }
  }

  private static String makeTxRootName( final CascadingTx tx ) {
    return txStateThreadLocal.toString( ) + tx.getRecord( ).getPersistenceContext( );
  }

  private static CascadingTx createTransaction( final Object obj ) throws RecoverablePersistenceException, RuntimeException {
    final String ctx = lookatPersistenceContext( obj );
    final CascadingTx ret = new CascadingTx( ctx );
    try {
      ret.begin( );
      if ( txRootThreadLocal.get( ) == null ) {
        final String txId = makeTxRootName( ret );
        LOG.trace( "Creating root entry for transaction tree: " + txId + " at: \n" + ret.getRecord().getStack() );
        txRootThreadLocal.set( txId );
      }
      txStateThreadLocal.get( ).put( ctx, ret );
      return ret;
    } catch ( RuntimeException ex ) {
      ret.rollback( );
      throw ex;
    }
  }

  /**
   * @deprecated Use #transactionFor(Object) instead (try-with-resources)
   * @see #transactionFor(Object)
   */
  @Deprecated
  public static EntityTransaction get( final Object obj ) {
    if ( hasTransaction( obj ) ) {
      final CascadingTx tx = getTransaction( obj );
      final EntityTransaction etx = tx.join( );
      return etx;
    } else {
      return createTransaction( obj );
    }
  }

  /**
   * Get a restriction builder for the given entity.
   *
   * <p><code>
   *   Entities.query( Entities.restriction( X.class ).equal( X_.name, "example" ) ).list( )
   * </code></p>
   *
   * @param entityClass The entity class
   * @param <E> The entity type
   * @return The builder
   */
  public static <E> EntityRestrictionBuilder<E> restriction( @Nonnull final Class<E> entityClass ) {
    return new EntityRestrictionBuilder<>( entityClass );
  }

  /**
   * Get a type-safe query for the given entity.
   *
   * <p><code>
   *   Entities.query( X.class ).readonly( ).orderBy( X_.name ).list( )
   * </code></p>
   *
   * @param entityClass The entity class
   * @param <E> The entity type
   * @return The query
   */
  public static <E> EntityCriteriaQuery<E,E> criteriaQuery( @Nonnull final Class<E> entityClass ) {
    return new EntityCriteriaQuery<>( new SimpleEntityCriteriaQueryContext<>( entityClass ) );
  }

  /**
   * Get a type-safe query for the given entity restriction.
   *
   * <p><code>
   *   Entities.query( Entities.restriction( X.class ).equal( X_.name, "example" ).build( ) ).uniqueResult( )
   * </code></p>
   *
   * This is a shorter equivalent to:
   *
   * <p><code>
   *   Entities.query( X.class ).where( Entities.restriction( X.class ).equal( X_.name, "example" ).build( ) ).uniqueResult( )
   * </code></p>
   *
   *
   * @param restriction The entity restriction
   * @param <E> The entity type
   * @return The query
   */
  public static <E> EntityCriteriaQuery<E,E> criteriaQuery( @Nonnull final EntityRestriction<E> restriction ) {
    return criteriaQuery( restriction.getEntityClass( ) ).where( restriction );
  }

  /**
   * Get a type-safe query for the given entity restriction builder.
   *
   * <p><code>
   *   Entities.query( Entities.restriction( X.class ).equal( X_.name, "example" ) ).uniqueResult( )
   * </code></p>
   *
   * This is a shorter equivalent to:
   *
   * <p><code>
   *   Entities.query( X.class ).where( Entities.restriction( X.class ).equal( X_.name, "example" ).build( ) ).uniqueResult( )
   * </code></p>
   *
   *
   * @param restriction The entity restriction builder
   * @param <E> The entity type
   * @return The query
   */
  public static <E> EntityCriteriaQuery<E,E> criteriaQuery( @Nonnull final EntityRestrictionBuilder<E> restriction ) {
    return criteriaQuery( restriction.build( ) );
  }

  /**
   * Get a query for counting instances of the given entity.
   *
   * @param entityClass The entity class
   * @param <E> The entity type
   * @return The count query
   */
  public static <E> EntityCriteriaQuery<E,Long> count( @Nonnull final Class<E> entityClass ) {
    return new EntityCriteriaQuery<>( new CountEntityCriteriaQueryContext<>( entityClass ) );
  }

  /**
   * Get a query for counting instances using the given entity restriction.
   *
   * @param entityRestriction The entity restriction
   * @param <E> The entity type
   * @return The count query
   */
  public static <E> EntityCriteriaQuery<E,Long> count( @Nonnull final EntityRestriction<E> entityRestriction ) {
    return count( entityRestriction.getEntityClass( ) ).where( entityRestriction );
  }

  /**
   * Get a delete for the given entity.
   *
   * <p>This is a bulk operation and will not load entities.</p>
   *
   * @param entityClass The entity class
   * @param <E> The entity type
   * @return The delete
   */
  public static <E> EntityCriteriaDelete<E> delete( @Nonnull final Class<E> entityClass ) {
    return new EntityCriteriaDelete<>( new EntityCriteriaDeleteContext<>( entityClass ) );
  }

  /**
   * Get a delete for the given entity restriction.
   *
   * <p>This is a bulk operation and will not load entities.</p>
   *
   * @param entityRestriction The entity restriction.
   * @param <E> The entity type
   * @return The delete
   */
  public static <E> EntityCriteriaDelete<E> delete( @Nonnull final EntityRestriction<E> entityRestriction ) {
    return delete( entityRestriction.getEntityClass( ) ).where( entityRestriction );
  }

  /**
   * Create an AutoCloseable transaction for the given object.
   *
   * <pre>
   * try ( TransactionResource transaction = transactionFor( ... ) ) {
   *   ...
   *   transaction.commit( );
   * }
   * </pre>
   *
   * The transaction will rollback unless committed.
   */
  public static TransactionResource transactionFor( final Object obj ) {
    return new TransactionResource( get( obj ) );
  }

  /**
   * Create an AutoCloseable transaction for the given object.
   *
   * <pre>
   * try ( TransactionResource transaction = distinctTransactionFor( ... ) ) {
   *   ...
   *   transaction.commit( );
   * }
   * </pre>
   *
   * <p>The transaction will rollback unless committed.</p>
   *
   * <p>This will fail if there is already an active transaction for the
   * requested context.</p>
   *
   * @param obj The object used to determine the transaction context
   * @return the TransactionResource
   */
  public static TransactionResource distinctTransactionFor( final Object obj ) {
    if ( hasTransaction( obj ) ) {
      throw new IllegalStateException( "Found existing transaction for context " + lookatPersistenceContext( obj ) );
    }
    return new TransactionResource( createTransaction( obj ) );
  }

  /**
   * Create an AutoCloseable read-only transaction for the given object.
   *
   * <pre>
   * try ( TransactionResource transaction = distinctTransactionFor( ... ) ) {
   *   ...
   *   transaction.commit( );
   * }
   * </pre>
   *
   * <p>The transaction will rollback unless committed.</p>
   *
   * <p>This will fail if there is already an active transaction for the
   * requested context.</p>
   *
   * @param obj The object used to determine the transaction context
   * @return the TransactionResource
   */
  public static TransactionResource readOnlyDistinctTransactionFor( final Object obj ) {
    final TransactionResource tx = distinctTransactionFor( obj );
    readOnly( obj );
    return tx;
  }

  /**
   * Call the given closure in a transaction.
   *
   * <p>The closure is passed the related EntityTransaction</p>
   *
   * @param obj The object used to determine the transaction context
   * @param closure The closure to call
   * @param <R> The closure result type
   * @return The closure result
   * @see #get(Object)
   */
  public static <R> R transaction( final Object obj, final Closure<R> closure ) {
    try ( final TransactionResource transactionResource = transactionFor( obj ) ) {
      return closure.call( transactionResource );
    }
  }

  /**
   * Call the given closure in a transaction.
   *
   * <p>The closure is passed the related EntityTransaction</p>
   *
   * <p>This will fail if there is already an active transaction for the
   * requested context.</p>
   *
   * @param obj The object used to determine the transaction context
   * @param closure The closure to call
   * @param <R> The closure result type
   * @return The closure result
   * @see #get(Object)
   */
  public static <R> R distinctTransaction( final Object obj, final Closure<R> closure ) throws IllegalStateException {
    if ( hasTransaction( obj ) ) {
      throw new IllegalStateException( "Found existing transaction for context " + lookatPersistenceContext( obj ) );
    }
    return transaction( obj, closure );
  }

  /**
   * Configure the contextual transaction with optimizations for read-only use.
   *
   * WARNING! Transactions can be nested and this will be applied for the
   * outermost transaction. Read-only distinct transaction methods are
   * preferred. Use with caution.
   *
   * @param object The object used to determine the transaction context
   *
   */
  public static void readOnly( final Object object ) {
    final Session session = getTransaction( object ).txState.getSession( );
    session.setDefaultReadOnly( true );
    session.setFlushMode( FlushMode.MANUAL );
  }

  /**
   * Lock the given object for update.
   *
   * @param object The object used to determine the transaction context
   */
  public static void lock( final Object object ) {
    getTransaction( object ).txState.getEntityManager( ).lock( object, LockModeType.PESSIMISTIC_WRITE );
  }

  public static <T> void flush( final T object ) {
    getTransaction( object ).txState.getEntityManager( ).flush( );
  }

  public static <T> void flushSession( final T object ) {
    getTransaction( object ).txState.getSession().flush( );
  }

  /**
   * Set the flush mode to on-commit, avoiding any pre-query auto flushing
   *
   * @param object The object used to determine the transaction context
   */
  public static void flushOnCommit( final Object object ) {
    getTransaction( object ).txState.getEntityManager( ).setFlushMode( FlushModeType.COMMIT );
  }

  public static <T> void clearSession( final T object ) {
    getTransaction( object ).txState.getSession( ).clear( );
  }

  /**
   * @see #query(T,QueryOptions)
   */
  @Deprecated
  public static <T> List<T> query( final T example ) {
    return query( example, false );
  }

  /**
   * @see #query(T,QueryOptions)
   */
  @SuppressWarnings( { "unchecked", "cast" } )
  @Deprecated
  public static <T> List<T> query( final T example, final boolean readOnly ) {
    final Example qbe = Example.create( example );
    final List<T> resultList = ( List<T> ) getTransaction( example ).getTxState( ).getSession( )
                                                                    .createCriteria( example.getClass( ) )
                                                                    .setReadOnly( readOnly )
                                                                    .setResultTransformer( Criteria.DISTINCT_ROOT_ENTITY )
                                                                    .setCacheable( true )
                                                                    .add( qbe )
                                                                    .list( );
    return Lists.newArrayList( Sets.newHashSet( resultList ) );
  }

  /**
   * Query items matching the given example restricted by the given criterion.
   *
   * <P>The caller must have an active transaction for the entity.</P>
   *
   * @param example The example object
   * @param readOnly Use True if the results will not be modified
   * @param criterion Additional restrictions for the query
   * @param aliases Any aliases necessary for the given criterion
   * @param <T> The entity type
   * @return The result list
   */
  @SuppressWarnings( { "unchecked", "cast" } )
  @Deprecated
  public static <T> List<T> query( final T example,
                                   final boolean readOnly,
                                   final Criterion criterion,
                                   final Map<String,String> aliases ) {
    return query( example, readOnly, criterion, aliases, false );
  }


  /**
   * Query items matching the given example restricted by the given criterion.
   *
   * <P>The caller must have an active transaction for the entity.</P>
   *
   * @param example The example object
   * @param readOnly Use True if the results will not be modified
   * @param criterion Additional restrictions for the query
   * @param aliases Any aliases necessary for the given criterion
   * @param outerJoins True to use outer joins
   * @param <T> The entity type
   * @return The result list
   */
  @SuppressWarnings( { "unchecked", "cast" } )
  @Deprecated
  public static <T> List<T> query( final T example,
                                   final boolean readOnly,
                                   final Criterion criterion,
                                   final Map<String,String> aliases,
                                   final boolean outerJoins ) {
    final Example qbe = Example.create( example );
    final Criteria criteria = getTransaction( example ).getTxState( ).getSession( )
        .createCriteria( example.getClass( ) )
        .setReadOnly( readOnly )
        .setResultTransformer( Criteria.DISTINCT_ROOT_ENTITY )
        .setCacheable( true )
        .add( qbe )
        .add( criterion );
    for ( final Map.Entry<String,String> aliasEntry : aliases.entrySet() ) {
      criteria.createAlias(
          aliasEntry.getKey( ),
          aliasEntry.getValue( ),
          outerJoins ? JoinType.LEFT_OUTER_JOIN : JoinType.INNER_JOIN );
    }
    final List<T> resultList = ( List<T> ) criteria.list( );
    return Lists.newArrayList( Sets.newHashSet( resultList ) );
  }

  /**
   * @see #query(T,QueryOptions)
   */
  @SuppressWarnings( { "unchecked", "cast" } )
  @Deprecated
  public static <T> List<T> query( final T example, final boolean readOnly, final int maxResults ) {
    final Example qbe = Example.create( example );
    final List<T> resultList = ( List<T> ) getTransaction( example ).getTxState( ).getSession( )
                                                                    .createCriteria( example.getClass( ) )
                                                                    .setReadOnly( readOnly )
                                                                    .setResultTransformer( Criteria.DISTINCT_ROOT_ENTITY )
                                                                    .setCacheable( true )
                                                                    .add( qbe )
                                                                    .setMaxResults( maxResults )
                                                                    .setFetchSize( maxResults )
                                                                    .list( );
    return Lists.newArrayList( Sets.newHashSet( resultList ) );
  }

  @SuppressWarnings( "unchecked" )
  @Deprecated
  public static <T> List<T> query( final T example, final QueryOptions options ) {
    final Example qbe = setOptions( Example.create( example ), options );
    final List<T> resultList = ( List<T> ) setOptions( getTransaction( example ).getTxState().getSession()
        .createCriteria( example.getClass() ), options )
        .setResultTransformer( Criteria.DISTINCT_ROOT_ENTITY )
        .add( qbe )
        .list();
    return Lists.newArrayList( Sets.newLinkedHashSet( resultList ) );
  }

  private static Criteria setOptions( final Criteria criteria, final QueryOptions options ) {
    final Integer maxResults = options.getMaxResults( );
    if ( maxResults != null ) {
      criteria.setMaxResults( maxResults );
    }
    final Integer fetchSize = options.getFetchSize();
    if ( fetchSize != null ) {
      criteria.setFetchSize( fetchSize );
    }
    final Boolean cacheable = options.getCacheable();
    if ( cacheable != null ) {
      criteria.setCacheable( cacheable );
    }
    final Boolean readonly = options.getReadonly();
    if ( readonly != null ) {
      criteria.setReadOnly( readonly );
    }
    final Criterion criterion = options.getCriterion();
    if ( criterion != null ) {
      criteria.add( criterion );
    }
    return criteria;
  }

  private static Example setOptions( final Example example, final QueryOptions options ) {
    if ( options.getMatchMode( ) != null ) {
      example.enableLike( options.getMatchMode( ) );
    }
    return example;
  }

  public static QueryOptionsBuilder queryOptions( ) {
    return new QueryOptionsBuilder( );
  }

  public interface QueryOptions {
    @Nullable MatchMode getMatchMode( );
    @Nullable Integer getMaxResults( );
    @Nullable Integer getFetchSize( );
    @Nullable Boolean getCacheable( );
    @Nullable Boolean getReadonly( );
    @Nullable Criterion getCriterion( );
  }

  @SuppressWarnings( "unused" )
  public static final class QueryOptionsBuilder {
    private MatchMode matchMode;
    private Integer maxResults;
    private Integer fetchSize;
    private Boolean cacheable;
    private Boolean readonly;
    private Criterion criterion;

    public QueryOptionsBuilder withMatchMode( final MatchMode matchMode ) {
      this.matchMode = matchMode;
      return this;
    }

    public QueryOptionsBuilder withMaxResults( final Integer maxResults ) {
      this.maxResults = maxResults;
      return this;
    }

    public QueryOptionsBuilder withFetchSize( final Integer fetchSize ) {
      this.fetchSize = fetchSize;
      return this;
    }

    public QueryOptionsBuilder withCacheable( final Boolean cacheable ) {
      this.cacheable = cacheable;
      return this;
    }

    public QueryOptionsBuilder withReadonly( final Boolean readonly ) {
      this.readonly = readonly;
      return this;
    }

    public QueryOptionsBuilder withCriterion( final Criterion criterion ) {
      this.criterion = criterion;
      return this;
    }

    public QueryOptions build( ) {
      return new QueryOptions( ) {
        @Override
        public MatchMode getMatchMode() {
          return matchMode;
        }

        @Override
        public Integer getMaxResults() {
          return maxResults;
        }

        @Override
        public Integer getFetchSize() {
          return fetchSize;
        }

        @Override
        public Boolean getCacheable() {
          return cacheable;
        }

        @Override
        public Boolean getReadonly() {
          return readonly;
        }

        @Override
        public Criterion getCriterion() {
          return criterion;
        }
      };
    }
  }

  @Deprecated
  public static <T> T uniqueResult( final T example ) throws TransactionException, NoSuchElementException {
    try {
      final Object pk = resolvePrimaryKey( example );
      final String natId = resolveNaturalId( example );
      if ( pk != null ) {
        return maybePrimaryKey( example );
      } else if ( natId != null ) {
        return maybeNaturalId( example );
      } else {
        return maybeDefinitelyExample( example );
      }
    } catch ( final NoSuchElementException ex ) {
      throw ex;
    } catch ( final RuntimeException ex ) {
      Logs.extreme( ).trace( ex, ex );
      final Exception newEx = PersistenceExceptions.throwFiltered( ex );
      throw new TransactionInternalException( newEx.getMessage( ), newEx );
    }
  }

  public static void evict( final Object obj  ) {
    getTransaction( obj ).getTxState( ).getSession( ).evict( obj );
  }

  public static void evictCache( final Object obj ) {
    final String ctx = lookatPersistenceContext( obj );
    final EntityManagerFactoryImpl emf = PersistenceContexts.getEntityManagerFactory( ctx );
    final org.hibernate.Cache cache = emf.getSessionFactory( ).getCache( );
    cache.evictQueryRegions( );
    cache.evictDefaultQueryRegion( );
    cache.evictCollectionRegions( );
    cache.evictEntityRegions( );
    LOG.debug( "Evicted cache for " + obj );
  }

  private static <T> String resolveNaturalId( final T example ) {
    if ( ( example instanceof HasNaturalId ) && ( ( ( HasNaturalId ) example ).getNaturalId( ) != null ) ) {
      return ( ( HasNaturalId ) example ).getNaturalId( );
    } else {
      return null;
    }
  }

  private static <T> T maybeDefinitelyExample( final T example ) throws HibernateException, NoSuchElementException {
    @SuppressWarnings( "unchecked" )
    final T ret = ( T ) Entities.getTransaction( example )
                                .getTxState( )
                                .getSession( )
                                .createCriteria( example.getClass( ) )
                                .add( Example.create( example ) )
                                .setCacheable( true )
                                .setMaxResults( 1 )
                                .setFetchSize( 1 )
                                .setFirstResult( 0 )
                                .uniqueResult( );
    if ( ret == null ) {
      throw new NoSuchElementException( "example: " + LogUtil.dumpObject( example ) );
    }
    return ret;
  }

  private static <T> T maybeNaturalId( final T example ) throws HibernateException, NoSuchElementException {
    final String natId = ( ( HasNaturalId ) example ).getNaturalId( );
    @SuppressWarnings( "unchecked" )
    final T ret = ( T ) Entities.getTransaction( example )
                                .getTxState( )
                                .getSession( )
                                .createCriteria( example.getClass( ) )
                                .add( Restrictions.eq( "naturalId", natId ) )
                                .setCacheable( true )
                                .uniqueResult( );
    if ( ret == null ) {
      throw new NoSuchElementException( "NaturalId: " + natId );
    }
    return ret;
  }

  private static <T> T maybePrimaryKey( final T example ) throws NoSuchElementException {
    final Object id = resolvePrimaryKey( example );
    if ( id == null ) {
      return null;
    } else {
      final T res = ( T ) Entities.getTransaction( example ).getTxState( ).getEntityManager( ).find( example.getClass( ), id );
      if ( res == null ) {
        throw new NoSuchElementException( "@Id: " + id );
      } else {
        return res;
      }
    }
  }

  static <T> Object resolvePrimaryKey( final T example ) {
    return Entities.getTransaction( example ).getTxState( ).getEntityManager( ).getEntityManagerFactory( ).getPersistenceUnitUtil( ).getIdentifier( example );
  }

  @Deprecated
  public static Criteria createCriteria( final Class class1 ) {
    return getTransaction( class1 ).getTxState( ).getSession( ).createCriteria( class1 );
  }

  @Deprecated
  public static Criteria createCriteriaUnique( final Class class1 ) {
    return getTransaction( class1 ).getTxState( ).getSession( ).createCriteria( class1 ).setCacheable( true ).setFetchSize( 1 ).setMaxResults( 1 ).setFirstResult( 0 );
  }

  /**
   * Invokes underlying persist implementation per jsr-220
   *
   * @throws ConstraintViolationException
   * @see http://opensource.atlassian.com/projects/hibernate/browse/HHH-1273
   */
  public static <T> T persist( final T newObject ) throws ConstraintViolationException {
    try {
      getTransaction( newObject ).getTxState( ).getEntityManager( ).persist( newObject );
      return newObject;
    } catch ( final RuntimeException ex ) {
      PersistenceExceptions.throwFiltered( ex );
      throw ex;
    }
  }

  /**
   * <table>
   * <tbody>
   * <tr valign="top">
   * <th>Scenario</th>
   * <th><tt>EntityManager.persist</tt></th>
   * <th><tt>EntityManager.merge</tt></th>
   * <th><tt>SessionManager.saveOrUpdate</tt></th>
   * </tr>
   * <tr valign="top">
   * <th>Object passed was never persisted</th>
   *
   * <td>1. Object added to persistence context as new entity<br>
   * 2. New entity inserted into database at flush/commit</td>
   * <td>1. State copied to new entity.<br>
   * 2. New entity added to persistence context<br>
   * 3. New entity inserted into database at flush/commit<br>
   * 4. New entity returned</td>
   * <td>1. Object added to persistence context as new entity<br>
   * 2. New entity inserted into database at flush/commit</td>
   * </tr>
   * <tr valign="top">
   * <th>Object was previously persisted, but not loaded in this persistence context</th>
   * <td>1. <tt>EntityExistsException</tt> thrown (or a <tt>PersistenceException</tt> at
   * flush/commit)</td>
   *
   * <td>2. Existing entity loaded.<br>
   * 2. State copied from object to loaded entity<br>
   * 3. Loaded entity updated in database at flush/commit<br>
   * 4. Loaded entity returned</td>
   * <td>1. Object added to persistence context<br>
   * 2. Loaded entity updated in database at flush/commit</td>
   * </tr>
   * <tr valign="top">
   * <th>Object was previously persisted and already loaded in this persistence context</th>
   * <td>1. <tt>EntityExistsException</tt> thrown (or a <tt>PersistenceException</tt> at flush or
   * commit time)</td>
   *
   * <td>1. State from object copied to loaded entity<br>
   * 2. Loaded entity updated in database at flush/commit<br>
   * 3. Loaded entity returned</td>
   * <td>1. <tt>NonUniqueObjectException</tt> thrown</td>
   * </tr>
   * </tbody>
   * </table>
   *
   * @throws ConstraintViolationException
   * @throws NoSuchElementException
   */
  public static <T> T merge( final T newObject ) throws ConstraintViolationException {
    if ( !isPersistent( newObject ) ) {
      try {
        return uniqueResult( newObject );
      } catch ( final Exception ex ) {
        return persist( newObject );
      }
    } else {
      try {
        final T persistedObject = getTransaction( newObject ).getTxState( ).getEntityManager( ).merge( newObject );
        return persistedObject == newObject
                                           ? newObject
                                           : persistedObject;
      } catch ( final RuntimeException ex ) {

        PersistenceExceptions.throwFiltered( ex );
        throw ex;
      }
    }
  }

  /**
   * Invokes underlying merge implementation per jsr-220
   *
   * @param object The object to merge
   * @param <T> The return type
   * @return The persistent instance for the object
   */
  public static <T> T mergeDirect( final T object ) {
    try {
      return getTransaction( object ).getTxState( ).getEntityManager( ).merge( object );
    } catch ( final RuntimeException ex ) {
      PersistenceExceptions.throwFiltered( ex );
      throw ex;
    }
  }

  @Deprecated
  public <T> T lookupAndClose( final T example ) throws NoSuchElementException {
    EntityTransaction db;
    T ret = null;
    if ( !hasTransaction( example ) ) {
      db = get( example );
    } else {
      db = getTransaction( example ).join( );
    }
    try {
      ret = uniqueResult( example );
      db.commit( );
    } catch ( final TransactionException ex ) {
      db.rollback( );
      throw new NoSuchElementException( ex.getMessage( ) );
    }
    return ret;
  }

  public static <T> Function<T, T> merge( ) {
    return new Function<T, T>( ) {

      @Override
      public T apply( final T arg0 ) {
        return Entities.merge( arg0 );
      }
    };
  }

  public static <T> void refresh( final T newObject ) throws ConstraintViolationException {
    try {
      getTransaction( newObject ).getTxState( ).getEntityManager( ).refresh( newObject, ( LockModeType ) null );
    } catch ( final RuntimeException ex ) {
      PersistenceExceptions.throwFiltered( ex );
      throw ex;
    }
  }

  public static <T> void refresh( final T newObject, final LockModeType lockMode ) throws ConstraintViolationException {
    try {
      getTransaction( newObject ).getTxState( ).getEntityManager( ).refresh( newObject, lockMode );
    } catch ( final RuntimeException ex ) {
      PersistenceExceptions.throwFiltered( ex );
      throw ex;
    }
  }

  /**
   * {@inheritDoc Session}
   */
  public static boolean isPersistent( final Object obj ) {
    if ( !hasTransaction( obj ) ) {
      return false;
    } else {
      return getTransaction( obj ).getTxState( ).getSession( ).contains( obj );
    }
  }

  /**
   * Check if a collection or proxy is initialized.
   *
   * @param obj The object to test
   * @return True if initialized
   */
  public static boolean isInitialized( @Nullable final Object obj ) {
    return obj != null && Hibernate.isInitialized( obj );
  }

  /**
   * Check if a collection or proxy is readable.
   *
   * <p>A lazy object is readable if it has an active transaction or if it is
   * already initialized.</p>
   *
   * @param obj The object to test
   * @return True if readable
   */
  public static boolean isReadable( @Nullable final Object obj ) {
    final SessionImplementor sessionImplementor = getSession( obj );
    return obj != null && (
        ( sessionImplementor != null && sessionImplementor.isOpen( ) ) ||
            isInitialized( obj ) );
  }

  private static SessionImplementor getSession( @Nullable final Object obj ) {
    SessionImplementor session = null;
    if ( obj instanceof AbstractPersistentCollection ) {
      session = ((AbstractPersistentCollection) obj).getSession( );
    } else if ( obj instanceof HibernateProxy ) {
      session = ((HibernateProxy) obj).getHibernateLazyInitializer( ).getSession( );
    }
    return session;
  }

  /**
   * Initialize a lazy proxy / collection, etc.
   *
   * <p>Force loading of a lazy object.</p>
   *
   * <p>This WILL NOT initialize an entity.</p>
   *
   * @param obj The proxy / collection to initialize.
   */
  public static void initialize( @Nullable final Object obj ) {
    Hibernate.initialize( obj );
  }

  /**
   *
   */
  public static <T> void delete( final T deleteObject ) {
    getTransaction( deleteObject ).getTxState( ).getEntityManager( ).remove( deleteObject );
  }

  /**
   * Delete all entities for the given class.
   *
   * @param <T> The entity type
   * @param deleteClass The entity class
   */
  public static <T> int deleteAll( final Class<T> deleteClass ) {
    return deleteAllMatching( deleteClass, null, Collections.<String,Object>emptyMap() );
  }

  /**
   * Delete all matching entities for the given class.
   *
   * @param <T> The entity type
   * @param deleteClass The entity class
   * @param condition The condition to match
   */
  public static <T> int deleteAllMatching( final Class<T> deleteClass,
                                           final String condition,
                                           final Map<String,?> parameters ) {
    try {
      final Query query = getTransaction( deleteClass ).getTxState().getEntityManager()
          .createQuery( "DELETE FROM " + deleteClass.getName() + " " + Strings.nullToEmpty(condition));
      for ( final Entry<String,?> entry : parameters.entrySet() ) {
        query.setParameter( entry.getKey(), entry.getValue() );
      }
      return query.executeUpdate();
    } catch ( Exception e ) {
      LOG.error( deleteClass, e );
      throw Exceptions.toUndeclared( e );
    }
  }

  /**
   * Count the matching entities for the given example.
   *
   * @param example The example entity
   * @return The number of matching entities
   */
  @Deprecated
  public static long count( final Object example ) {
    return count( example, Restrictions.conjunction(), Collections.<String,String>emptyMap() );
  }

  /**
   * Count the matching entities for the given example.
   *
   * @param example The example entity
   * @param criterion Additional restrictions for the query
   * @param aliases Any aliases necessary for the given criterion
   * @return The number of matching entities
   */
  @Deprecated
  public static long count( final Object example,
                            final Criterion criterion,
                            final Map<String,String> aliases ) {
    final Example qbe = Example.create( example );
    final Criteria criteria = getTransaction( example ).getTxState( ).getSession( )
        .createCriteria( example.getClass( ) )
        .setReadOnly( true )
        .setCacheable( false )
        .add( qbe )
        .add( criterion )
        .setProjection( Projections.rowCount() );
    for ( final Map.Entry<String,String> aliasEntry : aliases.entrySet() ) {
      criteria.createAlias( aliasEntry.getKey(), aliasEntry.getValue() ); // inner join by default
    }
    final Number count = (Number)criteria.uniqueResult( );
    return count.longValue();
  }

  /**
   * TODO REMOVE THIS, DO NOT USE
   * @deprecated
   */
  @Deprecated
  public static void registerClose( final Class<?> emClass ) {
    final EntityManager entityManager = getTransaction( emClass ).getTxState( ).getEntityManager();
    Entities.registerSynchronization( emClass, new Synchronization( ){
      @Override public void afterCompletion( final int i ) { entityManager.close(); }
      @Override public void beforeCompletion( ) { }
    } );
  }

  public static <T> void registerSynchronization( final Class<T> syncClass,
                                                  final Synchronization synchronization ) {
    final Session session = getTransaction( syncClass ).getTxState().getSession();
    final Transaction transaction = session.getTransaction();
    transaction.registerSynchronization( synchronization );
  }

  /**
   * Private for a reason.
   */
  private static class CascadingTx implements EntityTransaction {
    private final TxRecord record;
    private TxState        txState;

    /**
     * Private for a reason.
     *
     * @see {@link CascadingTx#get(Class)}
     * @param persistenceContext
     * @throws RecoverablePersistenceException
     */
    @SuppressWarnings( "unchecked" )
    CascadingTx( final String ctx ) throws RecoverablePersistenceException {
      final String uuid = UUID.randomUUID( ).toString( );
      this.record = new TxRecord( ctx, uuid );
      try {
        this.txState = new TxState( ctx );
      } catch ( final RuntimeException ex ) {
        Logs.extreme( ).error( ex, ex );
        this.rollback( );
        throw PersistenceExceptions.throwFiltered( ex );
      }
    }

    /**
     * @delegate Do not change semantics here.
     * @see javax.persistence.EntityTransaction#getRollbackOnly()
     */
    @Override
    public boolean getRollbackOnly( ) throws RecoverablePersistenceException {
      return this.txState == null
                                 ? false
                                 : this.txState.getRollbackOnly( );
    }

    /**
     * @delegate Do not change semantics here.
     * @see javax.persistence.EntityTransaction#setRollbackOnly()
     */
    @Override
    public void setRollbackOnly( ) throws RecoverablePersistenceException {
      if ( this.txState != null ) {
        this.txState.setRollbackOnly( );
      }
    }

    /**
     * @delegate Do not change semantics here.
     * @see javax.persistence.EntityTransaction#isActive()
     */
    @Override
    public boolean isActive( ) throws RecoverablePersistenceException {
      return this.txState == null
                                 ? false
                                 : this.txState.isActive( );
    }

    /**
     * @delegate Do not change semantics here.
     * @see javax.persistence.EntityTransaction#begin()
     */
    @Override
    public void begin( ) throws RecoverablePersistenceException {
      try {
        this.txState.begin( );
      } catch ( final RecoverablePersistenceException ex ) {
        PersistenceExceptions.throwFiltered( ex );
        removeTransaction( this );
      } catch ( final RuntimeException ex ) {
        PersistenceExceptions.throwFiltered( ex );
        removeTransaction( this );
        throw ex;
      }
    }

    /**
     * @delegate Do not change semantics here.
     * @see javax.persistence.EntityTransaction#rollback()
     */
    @Override
    public void rollback( ) throws RecoverablePersistenceException {
      removeTransaction( this );
      if ( ( this.txState != null ) && this.txState.isActive( ) ) {
        try {
          this.txState.rollback( );
          this.txState = null;
        } catch ( final RuntimeException ex ) {
          Logs.extreme( ).error( ex );
//          throw PersistenceExceptions.throwFiltered( ex );
        }
      } else {
        Logs.extreme( ).debug( "Duplicate call to rollback( )" );
      }
    }

    /**
     * @delegate Do not change semantics here.
     * @see javax.persistence.EntityTransaction#commit()
     */
    @Override
    public void commit( ) throws RecoverablePersistenceException {
      removeTransaction( this );
      if ( ( this.txState != null ) && this.txState.isActive( ) ) {
        try {
          this.txState.commit( );
        } catch ( final RuntimeException ex ) {
          throw PersistenceExceptions.throwFiltered( ex );
        }
      } else if ( Logs.isExtrrreeeme( ) ) {
        Logs.extreme( ).error( "Duplicate call to commit( ): " + Threads.currentStackString( ) );
      }
    }

    TxState getTxState( ) {
      return this.txState;
    }

    boolean isUsable( ) {
      final boolean active = isActive( );
      final boolean fresh = ( System.currentTimeMillis( ) - record.getStartTime( ) ) < TimeUnit.MINUTES.toMillis( 1 );
      return
          active &&
          ( fresh || isConnected( ) );
    }

    boolean isConnected( ) {
      boolean connected = false;
      final TxState state = this.txState;
      if ( state != null && state.getSession( ) instanceof SessionImplementor ) {
        final SessionImplementor sessionImplementor = (SessionImplementor) state.getSession( );
        final TransactionCoordinator txCoordinator = sessionImplementor.getTransactionCoordinator( );
        if ( txCoordinator != null ) {
          final JdbcCoordinator jdbcCoordinator = txCoordinator.getJdbcCoordinator( );
          if ( jdbcCoordinator != null ) {
            final LogicalConnectionImplementor logicalConnectionImplementor = jdbcCoordinator.getLogicalConnection( );
            try {
              connected =
                  logicalConnectionImplementor.isOpen( ) &&
                  logicalConnectionImplementor.isPhysicallyConnected( ) &&
                  !logicalConnectionImplementor.getConnection( ).isClosed( );
            } catch ( SQLException e ) {
              // not connected
            }
          }
        }
      }
      return connected;
    }

    public EntityTransaction join( ) {
      return new EntityTransaction( ) {
        @Override public void setRollbackOnly( ) { }
        @Override public void rollback( ) { }
        @Override public void commit( ) { }
        @Override public void begin( ) { }

        @Override
        public boolean isActive( ) {
          return CascadingTx.this.isActive( );
        }

        @Override
        public boolean getRollbackOnly( ) {
          return CascadingTx.this.getRollbackOnly( );
        }
      };
    }

    TxRecord getRecord( ) {
      return this.record;
    }

    private class TxState implements EntityTransaction {
      private EntityManager                em;
      private EntityTransaction            transaction;
      private final WeakReference<Session> sessionRef;

      public TxState( final String ctx ) {
        try {
          final EntityManagerFactory anemf = PersistenceContexts.getEntityManagerFactory( ctx );
          checkParam( anemf, notNullValue() );
          this.em = anemf.createEntityManager( );
          checkParam( this.em, notNullValue() );
          this.transaction = this.em.getTransaction( );
          checkParam( this.transaction, notNullValue() );
          this.sessionRef = new WeakReference<Session>( ( Session ) this.em.getDelegate( ) );
        } catch ( final RuntimeException ex ) {
          this.doCleanup( );
          throw ex;
        }
      }

      private void doCleanup( ) {
        // transaction
        if ( this.transaction != null && this.transaction.isActive( ) ) try {
          this.transaction.rollback( );
        } catch ( final RuntimeException ex ) {
          LOG.warn( ex );
          Logs.extreme( ).warn( ex, ex );
        }
        this.transaction = null;

        if ( CLEANUP_TX_SESSION ) {
          // sessionRef
          if ( this.sessionRef != null && ( this.sessionRef.get( ) != null ) ) {
            this.sessionRef.clear( );
          }

          //em
          if ( this.em != null && this.em.isOpen( ) ) try {
            this.em.close( );
          } catch ( final RuntimeException ex ) {
            LOG.warn( ex );
            Logs.extreme( ).warn( ex, ex );
          }
          this.em = null;
        }
      }

      EntityManager getEntityManager( ) {
        return this.em;
      }

      Session getSession( ) {
        return this.sessionRef.get( );
      }

      /**
       * @delegate Do not change semantics here.
       * @see javax.persistence.EntityTransaction#begin()
       */
      @Override
      public void begin( ) {
        try {
          this.transaction.begin( );
        } catch ( final RuntimeException ex ) {
          LOG.warn( ex );
          Logs.extreme( ).warn( ex, ex );
          doCleanup();
          throw ex;
        }
      }

      /**
       * @delegate Do not change semantics here.
       * @see javax.persistence.EntityTransaction#commit()
       */
      @Override
      public void commit( ) {
        try {
          this.transaction.commit( );
        } catch ( final RuntimeException ex ) {
          LOG.trace( ex, ex );
          Logs.extreme( ).warn( ex, ex );
          throw ex;
        } finally {
          doCleanup();
        }
      }

      /**
       * @delegate Do not change semantics here.
       * @see javax.persistence.EntityTransaction#getRollbackOnly()
       */
      @Override
      public boolean getRollbackOnly( ) {
        return this.transaction.getRollbackOnly( );
      }

      /**
       * @delegate Do not change semantics here.
       * @see javax.persistence.EntityTransaction#isActive()
       */
      @Override
      public boolean isActive( ) {
        return this.transaction != null && this.transaction.isActive( );
      }

      /**
       * @delegate Do not change semantics here.
       * @see javax.persistence.EntityTransaction#rollback()
       */
      @Override
      public void rollback( ) {
        try {
          this.transaction.rollback( );
        } catch ( final RuntimeException ex ) {
          LOG.error( ex, ex );
          throw ex;
        } finally {
          doCleanup();
        }
      }

      /**
       * @delegate Do not change semantics here.
       * @see javax.persistence.EntityTransaction#setRollbackOnly()
       */
      @Override
      public void setRollbackOnly( ) {
        this.transaction.setRollbackOnly( );
      }

    }
  }

  public static class TxRecord {
    private final String            persistenceContext;
    private final String            uuid;
    private final Long              startTime;
    private final String            stack;

    TxRecord( final String persistenceContext, final String uuid ) {
      this.persistenceContext = persistenceContext;
      this.uuid = uuid;
      this.stack = Threads.currentStackRange( 0, 32 );
      this.startTime = System.currentTimeMillis( );
    }

    public Long getStartTime( ) {
      return this.startTime;
    }

    public String getPersistenceContext( ) {
      return this.persistenceContext;
    }

    public String getUuid( ) {
      return this.uuid;
    }

    String getStack( ) {
      return this.stack;
    }

  }

  public static class RetryTransactionException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final Iterable<Class<?>> entitiesToEvict;

    /**
     * @param cause Throwable that occurred requiring a retry
     */
    public RetryTransactionException( final Throwable cause ) {
      this( cause, Collections.<Class<?>>emptyList( ) );
    }

    public RetryTransactionException( final Throwable cause, final Class<?> entityToEvict ) {
      this( cause, Collections.<Class<?>>singleton( entityToEvict ) );
    }
    public RetryTransactionException( final Throwable cause, final Iterable<Class<?>> entitiesToEvict ) {
      super( cause );
      this.entitiesToEvict = entitiesToEvict;
    }

    public Iterable<Class<?>> getEntitiesToEvict( ) {
      return entitiesToEvict;
    }
  }

  private static class TransactionalFunction<E, D, R> implements Function<D, R> {
    private Class<E>       entityType;
    private Function<D, R> function;
    private Integer        retries = CONCURRENT_UPDATE_RETRIES;

    TransactionalFunction( Class<E> entityType, Function<D, R> function, Integer retries ) {
      this.entityType = entityType;
      this.function = function;
      this.retries = retries;
    }

    @Override
    public R apply( final D input ) {
      RuntimeException rootCause = null;
      for ( int i = 0; i < retries; i++ ) {
        try ( final TransactionResource tx = Entities.transactionFor( this.entityType ) ) {
          R ret = this.function.apply( input );
          tx.commit( );
          return ret;
        } catch ( RuntimeException ex ) {
          if ( Exceptions.isCausedBy( ex, OptimisticLockException.class ) ) {
            rootCause = Exceptions.findCause( ex, OptimisticLockException.class );
          } else if ( Exceptions.isCausedBy( ex, LockAcquisitionException.class ) ) {
            rootCause = Exceptions.findCause( ex, LockAcquisitionException.class );
          } else if ( Exceptions.isCausedBy( ex, StaleObjectStateException.class ) ) {
            rootCause = Exceptions.findCause( ex, StaleObjectStateException.class );
          } else if ( Exceptions.isCausedBy( ex, RetryTransactionException.class ) ) {
            rootCause = Exceptions.findCause( ex, RetryTransactionException.class );
            for ( final Class<?> entityClass : ((RetryTransactionException)rootCause).getEntitiesToEvict( ) ) {
              Entities.evictCache( entityClass );
            }
          } else {
            rootCause = ex;
            Logs.extreme( ).error( ex, ex );
            throw ex;
          }
          final StaleObjectStateException stale = Exceptions.findCause( ex, StaleObjectStateException.class );
          if ( stale != null ) try {
            Entities.evictCache( Class.forName( stale.getEntityName( ) ) );
          } catch ( ClassNotFoundException e ) { /* eviction failure */ }
          try {
            TimeUnit.MILLISECONDS.sleep( 20 );
          } catch ( InterruptedException ex1 ) {
            Exceptions.maybeInterrupted( ex1 );
          }
          continue;
        }
      }
      throw ( rootCause != null
                               ? rootCause
                               : new NullPointerException( "BUG: Transaction retry failed but root cause exception is unknown!" ) );
    }

  }
  public static <T> Supplier<T> asTransaction( final Supplier<T> supplier ) {
    final List<Class> generics = Classes.genericsToClasses( supplier );
    for ( final Class<?> type : generics ) {
      if ( PersistenceContexts.isPersistentClass( type ) ) {
        return asTransaction( type, supplier );
      }
    }
    throw new IllegalArgumentException( "Failed to find generics for provided supplier, cannot make into transaction: " + Threads.currentStackString( ) );
  }

  public static <E, T> Supplier<T> asTransaction( final Class<E> type, final Supplier<T> supplier ) {
    return asTransaction( type, supplier, CONCURRENT_UPDATE_RETRIES );
  }

  public static <E, T> Supplier<T> asTransaction( final Class<E> type, final Supplier<T> supplier, final Integer retries ) {
    final Function<Object, T> functionalized = Functions.forSupplier( supplier );
    final Function<Object, T> transactionalized = Entities.asTransaction( type, functionalized, retries );
    return Suppliers.compose( transactionalized, Suppliers.ofInstance( Void.class ) );
  }

  public static <E, T> Predicate<T> asTransaction( final Predicate<T> predicate ) {
    final List<Class> generics = Classes.genericsToClasses( predicate );
    for ( final Class<?> type : generics ) {
      if ( PersistenceContexts.isPersistentClass( type ) ) {
        return asTransaction( type, predicate );
      }
    }
    throw new IllegalArgumentException( "Failed to find generics for provided predicate, cannot make into transaction: " + Threads.currentStackString( ) );
  }

  public static <E, T> Predicate<T> asDistinctTransaction( final Class<E> type, final Predicate<T> predicate ) {
    ensureDistinct( type );
    return asTransaction( type, predicate );
  }

  public static <E, T> Predicate<T> asTransaction( final Class<E> type, final Predicate<T> predicate ) {
    return asTransaction( type, predicate, CONCURRENT_UPDATE_RETRIES );
  }

  public static <E, T> Predicate<T> asTransaction( final Class<E> type, final Predicate<T> predicate, final Integer retries ) {
    final Function<T, Boolean> funcionalized = Functions.forPredicate( predicate );
    final Function<T, Boolean> transactionalized = Entities.asTransaction( type, funcionalized, retries );
    return new Predicate<T>( ) {

      @Override
      public boolean apply( T input ) {
        return transactionalized.apply( input );
      }

    };
  }

  public static <T, R> Function<T, R> asTransaction( final Function<T, R> function ) {
    if ( function instanceof TransactionalFunction ) {
      return function;
    } else {
      final List<Class> generics = Classes.genericsToClasses( function );
      for ( final Class<?> type : generics ) {
        if ( PersistenceContexts.isPersistentClass( type ) ) {
          return asTransaction( type, function );
        }
      }
      throw new IllegalArgumentException( "Failed to find generics for provided function, cannot make into transaction: " + Threads.currentStackString( ) );
    }
  }

  public static <E, T, R> Function<T, R> asTransaction( final Class<E> type, final Function<T, R> function ) {
    if ( function instanceof TransactionalFunction ) {
      return function;
    } else {
      return asTransaction( type, function, CONCURRENT_UPDATE_RETRIES );
    }
  }

  public static <E, T, R> Function<T, R> asDistinctTransaction( final Class<E> type, final Function<T, R> function ) {
    ensureDistinct( type );
    return asTransaction( type, function );
  }

  public static <E, T, R> Function<T, R> asTransaction( final Class<E> type, final Function<T, R> function, final int retries ) {
    if ( function instanceof TransactionalFunction ) {
      return function;
    } else {
      return new TransactionalFunction<>( type, function, retries );
    }
  }

  /**
   * Commit the transaction if possible, else rollback.
   *
   * @param tx The transaction
   * @return True if committed
   */
  public static boolean commit( EntityTransaction tx ) {
    boolean committed = false;
    if ( tx.getRollbackOnly( ) ) {
      tx.rollback( );
    } else if ( Databases.isVolatile( ) ) {
      tx.rollback( );
    } else {
      tx.commit( );
      committed = true;
    }
    return committed;
  }

  private static void ensureDistinct( final Object type ) {
    if ( hasTransaction( type ) ) {
      throw new IllegalStateException( "Found existing transaction for context " + lookatPersistenceContext( type ) );
    }
  }

  /**
   * Entity restriction builder.
   *
   * <p>Use the fluent API to construct a restriction for querying or deleting
   * entities.</p>
   *
   * @param <E> The restricted entity type
   * @see #restriction
   */
  @SuppressWarnings( "unused" )
  public static class EntityRestrictionBuilder<E> {
    private final Class<E> entityClass;
    private final List<EntityRestriction<E>> restrictions = Lists.newArrayList( );

    EntityRestrictionBuilder( final Class<E> entityClass ) {
      this.entityClass = entityClass;
    }

    /**
     * Restriction that all passed restrictions must be true
     */
    @SafeVarargs
    public final EntityRestrictionBuilder<E> all( EntityRestriction<E>... all ) {
      restrictions.add( new EntityRestriction.ConjunctionEntityRestriction<>( entityClass, Arrays.asList( all ) ) );
      return this;
    }

    /**
     * Restriction that any one of the passed restrictions must be true
     */
    @SafeVarargs
    public final EntityRestrictionBuilder<E> any( EntityRestriction<E>... any ) {
      restrictions.add( new EntityRestriction.DisjunctionEntityRestriction<>( entityClass, Arrays.asList( any ) ) );
      return this;
    }

    /**
     * @see CriteriaBuilder#equal(Expression, Object)
     */
    public <V> EntityRestrictionBuilder<E> equal(
        @Nonnull final SingularAttribute<? super E, V> attribute,
        @Nonnull final V value
    ) {
      restrictions.add( new EntityRestriction.EqualPropertyEntityValueRestriction<>( entityClass, attribute, value ) );
      return this;
    }

    /**
     * @see CriteriaBuilder#notEqual(Expression, Object)
     */
    public <V> EntityRestrictionBuilder<E> notEqual(
        @Nonnull final SingularAttribute<? super E, V> attribute,
        @Nonnull final V value
    ) {
      restrictions.add( new EntityRestriction.NotEqualPropertyEntityValueRestriction<>( entityClass, attribute, value ) );
      return this;
    }

    /**
     * @see CriteriaBuilder#equal(Expression, Object)
     * @see CriteriaBuilder#lower(Expression)
     */
    public EntityRestrictionBuilder<E> equalIgnoreCase(
        @Nonnull final SingularAttribute<? super E, String> attribute,
        @Nonnull final String value
    ) {
      restrictions.add( new EntityRestriction.EqualIgnoreCasePropertyEntityValueRestriction<>( entityClass, attribute, value ) );
      return this;
    }

    /**
     * If the given value is non null it is checked for equality, else this is a no-op.
     *
     * @see CriteriaBuilder#equal(Expression, Object)
     */
    public <V> EntityRestrictionBuilder<E> equalIfNonNull(
        @Nonnull final SingularAttribute<? super E, V> attribute,
        @Nullable final V value
    ) {
      if ( value != null ) {
        equal( attribute, value );
      }
      return this;
    }

    /**
     * @see CriteriaBuilder#isNull(Expression)
     */
    public <V> EntityRestrictionBuilder<E> isNull( @Nonnull final SingularAttribute<? super E, V> attribute ) {
      restrictions.add( new EntityRestriction.NullPropertyEntityRestriction<>( entityClass, attribute ) );
      return this;
    }

    /**
     * @see CriteriaBuilder#isNotNull(Expression)
     */
    public <V> EntityRestrictionBuilder<E> isNotNull( @Nonnull final SingularAttribute<? super E, V> attribute ) {
      restrictions.add( new EntityRestriction.NotNullPropertyEntityRestriction<>( entityClass, attribute ) );
      return this;
    }

    /**
     * @see CriteriaBuilder#isTrue(Expression)
     */
    public EntityRestrictionBuilder<E> isTrue( @Nonnull final SingularAttribute<? super E, Boolean> attribute ) {
      restrictions.add( new EntityRestriction.TruePropertyEntityRestriction<>( entityClass, attribute ) );
      return this;
    }

    /**
     * @see CriteriaBuilder#isFalse(Expression)
     */
    public EntityRestrictionBuilder<E> isFalse( @Nonnull final SingularAttribute<? super E, Boolean> attribute ) {
      restrictions.add( new EntityRestriction.FalsePropertyEntityRestriction<>( entityClass, attribute ) );
      return this;
    }

    /**
     * @see CriteriaBuilder#isEmpty(Expression)
     */
    public <V,C extends Collection<V>> EntityRestrictionBuilder<E> isEmpty(
        @Nonnull final PluralAttribute<E, C, V> attribute
    ) {
      restrictions.add( new EntityRestriction.EmptyPropertyEntityRestriction<>( entityClass, attribute ) );
      return this;
    }

    /**
     * @see CriteriaBuilder#isNotEmpty(Expression)
     */
    public <V,C extends Collection<V>> EntityRestrictionBuilder<E> isNotEmpty(
        @Nonnull final PluralAttribute<E, C, V> attribute
    ) {
      restrictions.add( new EntityRestriction.NotEmptyPropertyEntityRestriction<>( entityClass, attribute ) );
      return this;
    }

    /**
     * @see CriteriaBuilder#isMember(Object, Expression)
     */
    public <V,C extends Collection<V>> EntityRestrictionBuilder<E> isMember(
        @Nonnull final PluralAttribute<E, C, V> attribute,
        @Nonnull final V value
    ) {
      restrictions.add( new EntityRestriction.MemberPropertyEntityValueRestriction<>( entityClass, attribute, value ) );
      return this;
    }

    /**
     * @see CriteriaBuilder#isNotMember(Object, Expression)
     */
    public <V,C extends Collection<V>> EntityRestrictionBuilder<E> isNotMember(
        @Nonnull final PluralAttribute<E, C, V> attribute,
        @Nonnull final V value
    ) {
      restrictions.add( new EntityRestriction.NotMemberPropertyEntityValueRestriction<>( entityClass, attribute, value ) );
      return this;
    }

    /**
     * @see CriteriaBuilder#like(Expression, String)
     */
    public EntityRestrictionBuilder<E> like(
        @Nonnull final SingularAttribute<? super E, String> attribute,
        @Nonnull final String match
    ) {
      restrictions.add( new EntityRestriction.LikePropertyEntityValueRestriction<>( entityClass, attribute, match ) );
      return this;
    }

    /**
     * @see CriteriaBuilder#notLike(Expression, String)
     */
    public EntityRestrictionBuilder<E> notLike(
        @Nonnull final SingularAttribute<? super E, String> attribute,
        @Nonnull final String match
    ) {
      restrictions.add( new EntityRestriction.NotLikePropertyEntityValueRestriction<>( entityClass, attribute, match ) );
      return this;
    }

    /**
     * @see CriteriaBuilder#lessThan(Expression, Comparable)
     */
    public EntityRestrictionBuilder<E> before(
        @Nonnull final SingularAttribute<? super E, Date> attribute,
        @Nonnull final Date date
    ) {
      restrictions.add( new EntityRestriction.DateBeforePropertyEntityValueRestriction<>( entityClass, attribute, date ) );
      return this;
    }

    /**
     * @see CriteriaBuilder#greaterThan(Expression, Comparable)
     */
    public EntityRestrictionBuilder<E> after(
        @Nonnull final SingularAttribute<? super E, Date> attribute,
        @Nonnull final Date date
    ) {
      restrictions.add( new EntityRestriction.DateAfterPropertyEntityValueRestriction<>( entityClass, attribute, date ) );
      return this;
    }

    /**
     * @see #after
     * @see #before
     */
    public EntityRestrictionBuilder<E> between(
        @Nonnull final SingularAttribute<? super E, Date> attribute,
        @Nonnull final Date begin,
        @Nonnull final Date end
    ) {
      after( attribute, begin );
      before( attribute, end );
      return this;
    }

    /**
     * @see CriteriaBuilder#gt(Expression, Number)
     */
    public <V extends Number> EntityRestrictionBuilder<E> gt(
        @Nonnull final SingularAttribute<? super E, V> attribute,
        @Nonnull final V value
    ) {
      restrictions.add( new EntityRestriction.GreaterThanPropertyEntityValueRestriction<>( entityClass, attribute, value ) );
      return this;
    }

    /**
     * @see CriteriaBuilder#gt(Expression, Number)
     */
    public <V extends Number> EntityRestrictionBuilder<E> ge(
      @Nonnull final SingularAttribute<? super E, V> attribute,
      @Nonnull final V value
    ) {
      restrictions.add( new EntityRestriction.GreaterThanOrEqualToPropertyEntityValueRestriction<>( entityClass, attribute, value ) );
      return this;
    }

    /**
     * @see CriteriaBuilder#lt(Expression, Number)
     */
    public <V extends Number> EntityRestrictionBuilder<E> lt(
        @Nonnull final SingularAttribute<? super E, V> attribute,
        @Nonnull final V value
    ) {
      restrictions.add( new EntityRestriction.LessThanPropertyEntityValueRestriction<>( entityClass, attribute, value ) );
      return this;
    }

    /**
     * @see CriteriaBuilder#lt(Expression, Number)
     */
    public <V extends Number> EntityRestrictionBuilder<E> le(
      @Nonnull final SingularAttribute<? super E, V> attribute,
      @Nonnull final V value
    ) {
      restrictions.add( new EntityRestriction.LessThanOrEqualToPropertyEntityValueRestriction<>( entityClass, attribute, value ) );
      return this;
    }

    /**
     * Build an EntityRestriction for all supplied restrictions.
     *
     * @return The restriction conjunction.
     */
    public EntityRestriction<E> build( ) {
      return new EntityRestriction.ConjunctionEntityRestriction<>( entityClass, restrictions );
    }
  }

  private static final class SimpleEntityCriteriaQueryContext<E> extends EntityCriteriaQueryContext<E,E,CriteriaQuery<E>> {

    SimpleEntityCriteriaQueryContext( final Class<E> entityClass ) {
      super( entityClass, new NonNullFunction<CriteriaBuilder, CriteriaQuery<E>>(){
        @Nonnull
        @Override
        public CriteriaQuery<E> apply( final CriteriaBuilder builder ) {
          return builder.createQuery( entityClass );
        }
      }  );
    }

    protected void init( ) {
      this.query.select( from );
    }
  }

  private static final class CountEntityCriteriaQueryContext<E> extends EntityCriteriaQueryContext<E,Long,CriteriaQuery<Long>> {

    CountEntityCriteriaQueryContext( final Class<E> entityClass ) {
      super( entityClass, new NonNullFunction<CriteriaBuilder, CriteriaQuery<Long>>(){
        @Nonnull
        @Override
        public CriteriaQuery<Long> apply( final CriteriaBuilder builder ) {
          return builder.createQuery( Long.class );
        }
      } );
    }

    protected void init( ) {
      this.query.select( builder.countDistinct( from ) );
    }
  }

  private static final class PropertySubqueryEntityCriteriaQueryContext<E,V> extends EntityCriteriaQueryContext<E,V,Subquery<V>> {
    private final SingularAttribute<? super E, V> attribute;
    private final CommonAbstractCriteria criteria;

    PropertySubqueryEntityCriteriaQueryContext(
        final CommonAbstractCriteria criteria,
        final Class<E> entityClass,
        final SingularAttribute<? super E, V> attribute
    ) {
      super( entityClass, new NonNullFunction<CriteriaBuilder, Subquery<V>>(){
        @Nonnull
        @Override
        public Subquery<V> apply( final CriteriaBuilder builder ) {
          return criteria.subquery( attribute.getType( ).getJavaType( ) );
        }
      } );
      this.criteria = criteria;
      this.attribute = attribute;
      this.query.select( from.get( attribute ) );
    }

    protected void init( ) {
    }
  }

  private static abstract class EntityCriteriaContext<E> {
    protected final Class<E> entityClass;
    protected final EntityManager em;
    protected final CriteriaBuilder builder;

    EntityCriteriaContext( final Class<E> entityClass ) {
      this.entityClass = entityClass;
      this.em = getTransaction( entityClass ).getTxState( ).getEntityManager( );
      this.builder = this.em.getCriteriaBuilder( );
    }

    protected abstract List<Expression<Boolean>> getRestrictions( );
  }

  private static abstract class EntityCriteriaQueryContext<E,R,Q extends AbstractQuery<R>> extends EntityCriteriaContext<E> {
    protected final Q query;
    protected final From<E,E> from;
    protected final List<Expression<Boolean>> restrictions;

    EntityCriteriaQueryContext(
        final Class<E> entityClass,
        final NonNullFunction<CriteriaBuilder,Q> queryBuilder
    ) {
      super( entityClass );
      this.query = queryBuilder.apply( builder );
      this.from = this.query.from( entityClass );
      final javax.persistence.criteria.Predicate where = builder.conjunction( );
      this.restrictions = where.getExpressions( );
      this.query.where( where );
      init( );
    }

    protected abstract void init( );

    @Override
    protected List<Expression<Boolean>> getRestrictions( ) {
      return restrictions;
    }
  }

  private static class EntityCriteriaDeleteContext<E> extends EntityCriteriaContext<E> {
    protected final CriteriaDelete<E> delete;
    protected final From<E,E> from;
    protected final List<Expression<Boolean>> restrictions;

    EntityCriteriaDeleteContext( final Class<E> entityClass ) {
      super( entityClass );
      this.delete = this.builder.createCriteriaDelete( entityClass );
      this.from = this.delete.from( entityClass );
      final javax.persistence.criteria.Predicate where = builder.conjunction( );
      this.restrictions = where.getExpressions( );
      this.delete.where( where );
    }

    @Override
    protected List<Expression<Boolean>> getRestrictions( ) {
      return restrictions;
    }
  }

  @SuppressWarnings( "unused" )
  private static final class EntityTypedQueryOptions {
    private Integer firstResult;
    private Integer maxResults;
    private Integer fetchSize;
    private Boolean readonly;
    private FlushMode flushMode;

    public Integer getFirstResult( ) {
      return firstResult;
    }

    public void setFirstResult( final Integer firstResult ) {
      this.firstResult = firstResult;
    }

    public Integer getMaxResults( ) {
      return maxResults;
    }

    public void setMaxResults( final Integer maxResults ) {
      this.maxResults = maxResults;
    }

    public Integer getFetchSize( ) {
      return fetchSize;
    }

    public void setFetchSize( final Integer fetchSize ) {
      this.fetchSize = fetchSize;
    }

    public Boolean getReadonly( ) {
      return readonly;
    }

    public void setReadonly( final Boolean readonly ) {
      this.readonly = readonly;
    }

    public FlushMode getFlushMode( ) {
      return flushMode;
    }

    public void setFlushMode( final FlushMode flushMode ) {
      this.flushMode = flushMode;
    }

    void setOptions( final TypedQuery<?> query ) {
      if ( firstResult != null ) {
        query.setFirstResult( firstResult );
      }
      if ( maxResults != null ) {
        query.setMaxResults( maxResults );
      }
      if ( fetchSize != null ) {
        query.setHint( QueryHints.FETCH_SIZE, fetchSize );
      }
      if ( readonly != null ) {
        query.setHint( QueryHints.READ_ONLY, readonly );
      }
      if ( flushMode != null ) {
        query.setHint( QueryHints.FLUSH_MODE, flushMode );
      }
    }
  }

  /**
   * Callback for construction of subquery
   *
   * @param <E> The subquery entity type
   * @param <R> The subquery result type
   */
  public interface EntityCriteriaSubqueryCallback<E,R> {
    void restrict( final EntityCriteriaSubquery<E,R> subquery );
  }

  /**
   * Entity type-safe query
   *
   * @param <E> The entity type
   * @param <R> The query result type
   */
  @SuppressWarnings( "unused" )
  public static abstract class EntityCriteriaQueryBase<E,R,C extends AbstractQuery<R>,B extends EntityCriteriaQueryBase<E,R,C,B>> {
    protected final EntityCriteriaQueryContext<E,R,C> context;
    protected final EntityTypedQueryOptions options;

    EntityCriteriaQueryBase( @Nonnull final EntityCriteriaQueryContext<E,R,C> context ) {
      this.context = context;
      this.options = new EntityTypedQueryOptions( );
    }

    protected abstract B self( );

    /**
     * Add a where condition to the criteria.
     *
     * @param restriction The restriction to add.
     * @return This criteria query for method chaining.
     */
    public B where( @Nonnull final EntityRestriction<E> restriction ) {
      context.restrictions.add( restriction.build( context.builder, context.from ) );
      return self( );
    }

    /**
     * Add a where condition to the criteria.
     *
     * @param restriction The restriction to add.
     * @return This criteria query for method chaining.
     */
    public B where( @Nonnull final EntityRestrictionBuilder<E> restriction ) {
      return where( restriction.build( ) );
    }

    /**
     * Add a where condition to the criteria.
     *
     * @param restrictionFunction The function to build the restriction to add.
     * @return This criteria query for method chaining.
     */
    public B whereRestriction(
        @Nonnull final java.util.function.Function<EntityRestrictionBuilder<E>, EntityRestrictionBuilder<E>> restrictionFunction
    ) {
      return where( restrictionFunction.apply( Entities.restriction( context.entityClass ) ).build( ) );
    }

    /**
     * Directly add an equality restriction to the join.
     *
     * <p><code>
     *   Entities.query( X.class )
     *     .join( X.relation ).whereEqual( Y_.name, "example" ).list( )
     * </code></p>

     * <p>This is a shorthand for:</p>
     *
     * <p><code>
     *   Entities.query( X.class )
     *     .join( X.relation ).where( Entities.restriction( Y.class ).equal( Y_.name, "example" ) ).list( )
     * </code></p>
     *
     * @see Entities.EntityRestrictionBuilder#equal(SingularAttribute, Object)
     */
    public <V> B whereEqual(
        @Nonnull final SingularAttribute<? super E, V> attribute,
        @Nonnull final V value
    ) {
      context.restrictions.add( new EntityRestriction.EqualPropertyEntityValueRestriction<>(
          context.entityClass,
          attribute,
          value
      ).build( context.builder, context.from ) );
      return self( );
    }

    /**
     * Add a subselect contains condition to the criteria.
     *
     * @param attribute The attribute to check if contained
     * @param subqueryEntityClass The subquery entity class
     * @param subqueryAttribute The attribute of the subquery entity to match
     * @param subqueryCallback Callback to build subquery constraints.
     * @param <V> The contained value type
     * @param <S> The subquery entity type
     * @return
     */
    public <V,S> B whereIn(
        @Nonnull final SingularAttribute<? super E, V> attribute,
        @Nonnull final Class<S> subqueryEntityClass,
        @Nonnull final SingularAttribute<? super S, V> subqueryAttribute,
        @Nonnull final EntityCriteriaSubqueryCallback<S,V> subqueryCallback ) {
      final EntityCriteriaSubquery<S,V> subquery = new EntityCriteriaSubquery<>(
          new PropertySubqueryEntityCriteriaQueryContext<>( context.query, subqueryEntityClass, subqueryAttribute ) );
      subqueryCallback.restrict( subquery );
      context.restrictions.add( context.builder.in( context.from.get( attribute ) ).value( subquery.expression( ) ) );
      return self( );
    }

    /**
     * Join on the given attribute and restriction.
     *
     * @param attribute The entity attribute that defines the relation
     * @param restriction Restriction on the joined entity
     * @param <J> The joined entity type
     * @return This criteria query for method chaining.
     */
    public <J> B join(
        @Nonnull final SingularAttribute<? super E, J> attribute,
        @Nonnull final EntityRestriction<J> restriction
    ) {
      final Join<E,J> join = context.from.join( attribute );
      context.restrictions.add( restriction.build( context.builder, join ) );
      return self( );
    }

    /**
     * Join on the given attribute and restriction.
     *
     * @param attribute The entity attribute that defines the relation
     * @param restriction Restriction on the joined entity
     * @param <J> The joined entity type
     * @return This criteria query for method chaining.
     */
    public <J> B join(
        @Nonnull final CollectionAttribute<? super E, J> attribute,
        @Nonnull final EntityRestriction<J> restriction
    ) {
      final Join<E,J> join = context.from.join( attribute );
      context.restrictions.add( restriction.build( context.builder, join ) );
      return self( );
    }

    /**
     * Join on the given attribute and restriction.
     *
     * @param attribute The entity attribute that defines the relation
     * @param restriction Restriction on the joined entity
     * @param <J> The joined entity type
     * @return This criteria query for method chaining.
     */
    public <J> B join(
        @Nonnull final ListAttribute<? super E, J> attribute,
        @Nonnull final EntityRestriction<J> restriction
    ) {
      final Join<E,J> join = context.from.join( attribute );
      context.restrictions.add( restriction.build( context.builder, join ) );
      return self( );
    }

    /**
     * Join on the given attribute and restriction.
     *
     * @param attribute The entity attribute that defines the relation
     * @param restriction Restriction on the joined entity
     * @param <J> The joined entity type
     * @return This criteria query for method chaining.
     */
    public <J> B join(
        @Nonnull final SetAttribute<? super E, J> attribute,
        @Nonnull final EntityRestriction<J> restriction
    ) {
      final Join<E,J> join = context.from.join( attribute );
      context.restrictions.add( restriction.build( context.builder, join ) );
      return self( );
    }
  }

  public static final class EntityCriteriaSubquery<E,R> extends EntityCriteriaQueryBase<E,R,Subquery<R>,EntityCriteriaSubquery<E,R>> {

    EntityCriteriaSubquery( @Nonnull final EntityCriteriaQueryContext<E, R, Subquery<R>> context ) {
      super( context );
    }

    protected EntityCriteriaSubquery<E, R> self() {
      return this;
    }

    Expression<R> expression( ) {
      return context.query;
    }

    /**
     * Join on the given attribute and restriction.
     *
     * <p>Restrictions should be added to the returned type-safe join.</p>
     *
     * @param attribute The entity attribute that defines the relation
     * @param <J> The joined entity type
     * @return A criteria join for method chaining
     */
    public <J> EntityCriteriaJoin<J> join(
        @Nonnull final SingularAttribute<? super E, J> attribute
    ) {
      final Join<E,J> join = context.from.join( attribute );
      return new EntityCriteriaJoin<>( attribute.getJavaType( ), this.context, join );
    }

    /**
     * Join on the given attribute and restriction.
     *
     * <p>Restrictions should be added to the returned type-safe join.</p>
     *
     * @param attribute The entity attribute that defines the relation
     * @param <J> The joined entity type
     * @return A criteria join for method chaining
     */
    public <J> EntityCriteriaJoin<J> join(
        @Nonnull final CollectionAttribute<? super E, J> attribute
    ) {
      final Join<E,J> join = context.from.join( attribute );
      return new EntityCriteriaJoin<>( attribute.getElementType( ).getJavaType( ), this.context, join );
    }

    /**
     * Join on the given attribute and restriction.
     *
     * <p>Restrictions should be added to the returned type-safe join.</p>
     *
     * @param attribute The entity attribute that defines the relation
     * @param <J> The joined entity type
     * @return A criteria join for method chaining
     */
    public <J> EntityCriteriaJoin<J> join(
        @Nonnull final ListAttribute<? super E, J> attribute
    ) {
      final Join<E,J> join = context.from.join( attribute );
      return new EntityCriteriaJoin<>( attribute.getElementType( ).getJavaType( ), this.context, join );
    }

    /**
     * Join on the given attribute and restriction.
     *
     * <p>Restrictions should be added to the returned type-safe join.</p>
     *
     * @param attribute The entity attribute that defines the relation
     * @param <J> The joined entity type
     * @return A criteria join for method chaining.
     */
    public <J> EntityCriteriaJoin<J> join(
        @Nonnull final SetAttribute<? super E, J> attribute
    ) {
      final Join<E,J> join = context.from.join( attribute );
      return new EntityCriteriaJoin<>( attribute.getElementType( ).getJavaType( ), this.context, join );
    }

    /**
     * Outer join on the given attribute and restriction.
     *
     * <p>Restrictions should be added to the returned type-safe join.</p>
     *
     * @param attribute The entity attribute that defines the relation
     * @param <J> The joined entity type
     * @return A criteria join for method chaining
     */
    public <J> EntityCriteriaJoin<J> joinLeft(
        @Nonnull final SingularAttribute<? super E, J> attribute
    ) {
      final Join<E,J> join = context.from.join( attribute, javax.persistence.criteria.JoinType.LEFT );
      return new EntityCriteriaJoin<>( attribute.getJavaType( ), this.context, join );
    }

    /**
     * Outer join on the given attribute and restriction.
     *
     * <p>Restrictions should be added to the returned type-safe join.</p>
     *
     * @param attribute The entity attribute that defines the relation
     * @param <J> The joined entity type
     * @return A criteria join for method chaining
     */
    public <J> EntityCriteriaJoin<J> joinLeft(
        @Nonnull final CollectionAttribute<? super E, J> attribute
    ) {
      final Join<E,J> join = context.from.join( attribute, javax.persistence.criteria.JoinType.LEFT );
      return new EntityCriteriaJoin<>( attribute.getElementType( ).getJavaType( ), this.context, join );
    }

    /**
     * Outer join on the given attribute and restriction.
     *
     * <p>Restrictions should be added to the returned type-safe join.</p>
     *
     * @param attribute The entity attribute that defines the relation
     * @param <J> The joined entity type
     * @return A criteria join for method chaining
     */
    public <J> EntityCriteriaJoin<J> joinLeft(
        @Nonnull final ListAttribute<? super E, J> attribute
    ) {
      final Join<E,J> join = context.from.join( attribute, javax.persistence.criteria.JoinType.LEFT );
      return new EntityCriteriaJoin<>( attribute.getElementType( ).getJavaType( ), this.context, join );
    }

    /**
     * Outer join on the given attribute and restriction.
     *
     * <p>Restrictions should be added to the returned type-safe join.</p>
     *
     * @param attribute The entity attribute that defines the relation
     * @param <J> The joined entity type
     * @return A criteria join for method chaining.
     */
    public <J> EntityCriteriaJoin<J> joinLeft(
        @Nonnull final SetAttribute<? super E, J> attribute
    ) {
      final Join<E,J> join = context.from.join( attribute, javax.persistence.criteria.JoinType.LEFT );
      return new EntityCriteriaJoin<>( attribute.getElementType( ).getJavaType( ), this.context, join );
    }
  }

  public static final class EntityCriteriaQuery<E,R> extends EntityCriteriaQueryBase<E,R,CriteriaQuery<R>,EntityCriteriaQuery<E,R>>  {

    EntityCriteriaQuery( @Nonnull final EntityCriteriaQueryContext<E,R, CriteriaQuery<R>> context ) {
      super( context );
    }

    protected EntityCriteriaQuery<E,R> self( ) {
      return this;
    }

    /**
     * Join on the given attribute and restriction.
     *
     * <p>Restrictions should be added to the returned type-safe join.</p>
     *
     * @param attribute The entity attribute that defines the relation
     * @param <J> The joined entity type
     * @return A criteria join for method chaining
     */
    public <J> EntityCriteriaQueryJoin<J,R,EntityCriteriaQuery<E,R>> join(
        @Nonnull final SingularAttribute<? super E, J> attribute
    ) {
      final Join<E,J> join = context.from.join( attribute );
      return new EntityCriteriaQueryJoin<>( attribute.getJavaType( ), this, this.context, join );
    }

    /**
     * Join on the given attribute and restriction.
     *
     * <p>Restrictions should be added to the returned type-safe join.</p>
     *
     * @param attribute The entity attribute that defines the relation
     * @param <J> The joined entity type
     * @return A criteria join for method chaining
     */
    public <J> EntityCriteriaQueryJoin<J,R,EntityCriteriaQuery<E,R>> join(
        @Nonnull final CollectionAttribute<? super E, J> attribute
    ) {
      final Join<E,J> join = context.from.join( attribute );
      return new EntityCriteriaQueryJoin<>( attribute.getElementType( ).getJavaType( ), this, this.context, join );
    }

    /**
     * Join on the given attribute and restriction.
     *
     * <p>Restrictions should be added to the returned type-safe join.</p>
     *
     * @param attribute The entity attribute that defines the relation
     * @param <J> The joined entity type
     * @return A criteria join for method chaining
     */
    public <J> EntityCriteriaQueryJoin<J,R,EntityCriteriaQuery<E,R>> join(
        @Nonnull final ListAttribute<? super E, J> attribute
    ) {
      final Join<E,J> join = context.from.join( attribute );
      return new EntityCriteriaQueryJoin<>( attribute.getElementType( ).getJavaType( ), this, this.context, join );
    }

    /**
     * Join on the given attribute and restriction.
     *
     * <p>Restrictions should be added to the returned type-safe join.</p>
     *
     * @param attribute The entity attribute that defines the relation
     * @param <J> The joined entity type
     * @return A criteria join for method chaining
     */
    public <J> EntityCriteriaQueryJoin<J,R,EntityCriteriaQuery<E,R>> join(
        @Nonnull final SetAttribute<? super E, J> attribute
    ) {
      final Join<E,J> join = context.from.join( attribute );
      return new EntityCriteriaQueryJoin<>( attribute.getElementType( ).getJavaType( ), this, this.context, join );
    }

    /**
     * Order the query result ascending by the given attribute
     *
     * @param attribute The entity attribute to order by
     * @return This criteria query for method chaining.
     */
    public EntityCriteriaQuery<E,R> orderBy( @Nonnull final SingularAttribute<? super E, ?> attribute ) {
      context.query.orderBy( context.builder.asc( context.from.get( attribute ) ) );
      return this;
    }

    /**
     * Order the query result descending by the given attribute
     *
     * @param attribute The entity attribute to order by
     * @return This criteria query for method chaining.
     */
    public EntityCriteriaQuery<E,R> orderByDesc( @Nonnull final SingularAttribute<? super E, ?> attribute ) {
      context.query.orderBy( context.builder.desc( context.from.get( attribute ) ) );
      return this;
    }

    /**
     * Configure the first result index
     *
     * @param firstResult The first result to return
     * @return This criteria query for method chaining.
     */
    public EntityCriteriaQuery<E,R> firstResult( @Nullable final Integer firstResult ) {
      this.options.setFirstResult( firstResult );
      return this;
    }

    /**
     * Configure the maximum number of results
     *
     * @param maxResults The maximum results to return
     * @return This criteria query for method chaining.
     */
    public EntityCriteriaQuery<E,R> maxResults( @Nullable final Integer maxResults ) {
      this.options.setMaxResults( maxResults );
      return this;
    }

    /**
     * Configure the fetch size for the query.
     *
     * @param fetchSize The fetch size to use
     * @return This criteria query for method chaining.
     */
    public EntityCriteriaQuery<E,R> fetchSize( @Nullable final Integer fetchSize ) {
      this.options.setFetchSize( fetchSize );
      return this;
    }

    /**
     * Configure the flush mode for the query.
     *
     * @param flushMode The flush mode to use
     * @return This criteria query for method chaining.
     */
    public EntityCriteriaQuery<E,R> fetchSize( @Nullable final FlushMode flushMode ) {
      this.options.setFlushMode( flushMode );
      return this;
    }

    /**
     * Configure the read-only flag for the query.
     *
     * @param readonly True to set read-only
     * @return This criteria query for method chaining.
     */
    public EntityCriteriaQuery<E,R> readonly( @Nullable final Boolean readonly ) {
      this.options.setReadonly( readonly );
      return this;
    }

    /**
     * Configure the read-only flag to true for the query.
     *
     * @return This criteria query for method chaining.
     */
    public EntityCriteriaQuery<E,R> readonly( ) {
      return readonly( Boolean.TRUE );
    }

    /**
     * Get a unique entity for the query.
     *
     * @return The entity
     */
    @Nonnull
    public R uniqueResult( ) {
      try {
        return configuredTypedQuery( ).getSingleResult( );
      } catch ( final NoResultException | NonUniqueResultException e ) {
        throw (NoSuchElementException) new NoSuchElementException( ).initCause( e );
      } catch ( final PersistenceException e ) {
        Logs.extreme( ).trace( e, e );
        throw e;
      }
    }

    /**
     * Get a unique entity for the query if available.
     *
     * @return The entity option
     */
    @Nonnull
    public Optional<R> uniqueResultOption( ) {
      final List<R> results = list( );
      final Optional<R> resultOption;
      if ( results.size( ) == 1 ) {
        resultOption = Optional.of( results.get( 0 ) );
      } else {
        resultOption = Optional.absent( );
      }
      return resultOption;
    }

    /**
     * List entities for the query.
     *
     * @return The list of entities
     */
    @Nonnull
    public List<R> list( ) {
      try {
        return configuredTypedQuery( ).getResultList( );
      } catch ( final PersistenceException e ) {
        Logs.extreme( ).trace( e, e );
        throw e;
      }
    }

    private TypedQuery<R> configuredTypedQuery( ) {
      final TypedQuery<R> typedQuery = context.em.createQuery( context.query );
      options.setOptions( typedQuery );
      return typedQuery;
    }
  }

  public static final class EntityCriteriaJoin<JE> extends EntityCriteriaBaseJoin<JE,EntityCriteriaJoin<JE>> {
    EntityCriteriaJoin( @Nonnull Class<JE> joinEntityClass,
                        @Nonnull final EntityCriteriaContext<?> context,
                        @Nonnull final Join<?, JE> join ) {
      super( joinEntityClass, context, join );
    }

    @Override
    protected  EntityCriteriaJoin<JE> self() {
      return this;
    }

    /**
     * Join on the given attribute and restriction.
     *
     * <p>Restrictions should be added to the returned type-safe join.</p>
     *
     * @param attribute The entity attribute that defines the relation
     * @param <J> The joined entity type
     * @return A criteria join for method chaining
     */
    public <J> EntityCriteriaJoin<J> join(
        @Nonnull final SingularAttribute<? super JE, J> attribute
    ) {
      final Join<JE,J> join = this.join.join( attribute );
      return new EntityCriteriaJoin<>( attribute.getJavaType( ), this.context, join );
    }

    /**
     * Join on the given attribute and restriction.
     *
     * <p>Restrictions should be added to the returned type-safe join.</p>
     *
     * @param attribute The entity attribute that defines the relation
     * @param <J> The joined entity type
     * @return A criteria join for method chaining
     */
    public <J> EntityCriteriaJoin<J> join(
        @Nonnull final CollectionAttribute<? super JE, J> attribute
    ) {
      final Join<JE,J> join = this.join.join( attribute );
      return new EntityCriteriaJoin<>( attribute.getElementType( ).getJavaType( ), this.context, join );
    }

    /**
     * Join on the given attribute and restriction.
     *
     * <p>Restrictions should be added to the returned type-safe join.</p>
     *
     * @param attribute The entity attribute that defines the relation
     * @param <J> The joined entity type
     * @return A criteria join for method chaining
     */
    public <J> EntityCriteriaJoin<J> join(
        @Nonnull final ListAttribute<? super JE, J> attribute
    ) {
      final Join<JE,J> join = this.join.join( attribute );
      return new EntityCriteriaJoin<>( attribute.getElementType( ).getJavaType( ), this.context, join );
    }

    /**
     * Join on the given attribute and restriction.
     *
     * <p>Restrictions should be added to the returned type-safe join.</p>
     *
     * @param attribute The entity attribute that defines the relation
     * @param <J> The joined entity type
     * @return A criteria join for method chaining.
     */
    public <J> EntityCriteriaJoin<J> join(
        @Nonnull final SetAttribute<? super JE, J> attribute
    ) {
      final Join<JE,J> join = this.join.join( attribute );
      return new EntityCriteriaJoin<>( attribute.getElementType( ).getJavaType( ), this.context, join );
    }
  }

  /**
   * Entity type-safe join for queries
   *
   * @param <JE> The joined entity type
   * @param <ECQ> The query type
   */
  public static final class EntityCriteriaQueryJoin<JE,ECQR,ECQ extends EntityCriteriaQuery<?,ECQR>> extends EntityCriteriaBaseJoin<JE,EntityCriteriaQueryJoin<JE,ECQR,ECQ>>  {
    private final ECQ entityCriteriaQuery;

    EntityCriteriaQueryJoin( @Nonnull Class<JE> joinEntityClass,
                             @Nonnull ECQ entityCriteriaQuery,
                             @Nonnull final EntityCriteriaContext<?> context,
                             @Nonnull final Join<?, JE> join ) {
      super( joinEntityClass, context, join );
      this.entityCriteriaQuery = entityCriteriaQuery;
    }

    @Override
    protected EntityCriteriaQueryJoin<JE, ECQR, ECQ> self() {
      return this;
    }

    /**
     * Get the original query.
     *
     * @return The query.
     */
    public ECQ entityCriteriaQuery( ) {
      return entityCriteriaQuery;
    }

    /**
     * Evaluate the root for this join.
     *
     * @see EntityCriteriaQuery#uniqueResult()
     * @return The unique result entity
     */
    @Nonnull
    public ECQR uniqueResult( ) {
      return entityCriteriaQuery( ).uniqueResult( );
    }

    /**
     * Evaluate the root for this join.
     *
     * @see EntityCriteriaQuery#uniqueResultOption()
     * @return The optional unique result entity
     */
    @Nonnull
    public Optional<ECQR> uniqueResultOption( ) {
      return entityCriteriaQuery( ).uniqueResultOption( );
    }

    /**
     * Evaluate the root for this join.
     *
     * @see EntityCriteriaQuery#list()
     * @return The list of entities
     */
    @Nonnull
    public List<ECQR> list( ) {
      return entityCriteriaQuery( ).list( );
    }

    /**
     * Join on the given attribute and restriction.
     *
     * <p>Restrictions should be added to the returned type-safe join.</p>
     *
     * @param attribute The entity attribute that defines the relation
     * @param <J> The joined entity type
     * @return A criteria join for method chaining
     */
    public <J> EntityCriteriaQueryJoin<J,ECQR,ECQ> join(
        @Nonnull final SingularAttribute<? super JE, J> attribute
    ) {
      final Join<JE,J> join = this.join.join( attribute );
      return new EntityCriteriaQueryJoin<>( attribute.getJavaType( ), entityCriteriaQuery, this.context, join );
    }

    /**
     * Join on the given attribute and restriction.
     *
     * <p>Restrictions should be added to the returned type-safe join.</p>
     *
     * @param attribute The entity attribute that defines the relation
     * @param <J> The joined entity type
     * @return A criteria join for method chaining
     */
    public <J> EntityCriteriaQueryJoin<J,ECQR,ECQ> join(
        @Nonnull final CollectionAttribute<? super JE, J> attribute
    ) {
      final Join<JE,J> join = this.join.join( attribute );
      return new EntityCriteriaQueryJoin<>( attribute.getElementType( ).getJavaType( ), entityCriteriaQuery, this.context, join );
    }

    /**
     * Join on the given attribute and restriction.
     *
     * <p>Restrictions should be added to the returned type-safe join.</p>
     *
     * @param attribute The entity attribute that defines the relation
     * @param <J> The joined entity type
     * @return A criteria join for method chaining
     */
    public <J> EntityCriteriaQueryJoin<J,ECQR,ECQ> join(
        @Nonnull final ListAttribute<? super JE, J> attribute
    ) {
      final Join<JE,J> join = this.join.join( attribute );
      return new EntityCriteriaQueryJoin<>( attribute.getElementType( ).getJavaType( ), entityCriteriaQuery, this.context, join );
    }

    /**
     * Join on the given attribute and restriction.
     *
     * <p>Restrictions should be added to the returned type-safe join.</p>
     *
     * @param attribute The entity attribute that defines the relation
     * @param <J> The joined entity type
     * @return A criteria join for method chaining.
     */
    public <J> EntityCriteriaQueryJoin<J,ECQR,ECQ> join(
        @Nonnull final SetAttribute<? super JE, J> attribute
    ) {
      final Join<JE,J> join = this.join.join( attribute );
      return new EntityCriteriaQueryJoin<>( attribute.getElementType( ).getJavaType( ), entityCriteriaQuery, this.context, join );
    }
  }

  /**
   * Entity type-safe join
   *
   * @param <JE> The joined entity type
   */
  public static abstract class EntityCriteriaBaseJoin<JE,ECJT> {
    protected final Class<JE> joinEntityClass;
    protected final EntityCriteriaContext<?> context;
    protected final Join<?, JE> join;

    EntityCriteriaBaseJoin( @Nonnull Class<JE> joinEntityClass,
                            @Nonnull final EntityCriteriaContext<?> context,
                            @Nonnull final Join<?, JE> join ) {
      this.joinEntityClass = joinEntityClass;
      this.context = context;
      this.join = join;
    }

    protected abstract ECJT self( );

    /**
     * Add a where condition to the join.
     *
     * @param restriction The restriction to add.
     * @return This criteria join for method chaining.
     */
    public ECJT where( @Nonnull final EntityRestriction<JE> restriction ) {
      context.getRestrictions( ).add( restriction.build( context.builder, join ) );
      return self( );
    }

    /**
     * Add a where condition to the join.
     *
     * @param restriction The restriction to add.
     * @return This criteria join for method chaining.
     */
    public ECJT where( @Nonnull final EntityRestrictionBuilder<JE> restriction ) {
      return where( restriction.build( ) );
    }

    /**
     * Directly add an equality restriction to the query.
     *
     * <p><code>
     *   Entities.query( X.class ).whereEquals( X_.type, "example" ).list( )
     * </code></p>

     * <p>This is a shorthand for:</p>
     *
     * <p><code>
     *   Entities.query( X.class ).where( Entities.restriction( X.class ).equal( X_.type, "example" ) ).list( )
     * </code></p>
     *
     * @see Entities.EntityRestrictionBuilder#equal(SingularAttribute, Object)
     */
    public <V> ECJT whereEqual(
        @Nonnull final SingularAttribute<? super JE, V> attribute,
        @Nonnull final V value
    ) {
      context.getRestrictions( ).add( new EntityRestriction.EqualPropertyEntityValueRestriction<>(
          joinEntityClass,
          attribute,
          value
      ).build( context.builder, join ) );
      return self( );
    }

    /**
     * Join on the given attribute and restriction.
     *
     * @param attribute The entity attribute that defines the relation
     * @param restriction Restriction on the joined entity
     * @param <J> The joined entity type
     * @return This criteria query for method chaining.
     */
    public <J> ECJT join(
        @Nonnull final SingularAttribute<? super JE, J> attribute,
        @Nonnull final EntityRestriction<J> restriction
    ) {
      final Join<JE,J> join = this.join.join( attribute );
      context.getRestrictions( ).add( restriction.build( context.builder, join ) );
      return self( );
    }

    /**
     * Join on the given attribute and restriction.
     *
     * @param attribute The entity attribute that defines the relation
     * @param restriction Restriction on the joined entity
     * @param <J> The joined entity type
     * @return This criteria query for method chaining.
     */
    public <J> ECJT join(
        @Nonnull final CollectionAttribute<? super JE, J> attribute,
        @Nonnull final EntityRestriction<J> restriction
    ) {
      final Join<JE,J> join = this.join.join( attribute );
      context.getRestrictions( ).add( restriction.build( context.builder, join ) );
      return self( );
    }

    /**
     * Join on the given attribute and restriction.
     *
     * @param attribute The entity attribute that defines the relation
     * @param restriction Restriction on the joined entity
     * @param <J> The joined entity type
     * @return This criteria query for method chaining.
     */
    public <J> ECJT join(
        @Nonnull final ListAttribute<? super JE, J> attribute,
        @Nonnull final EntityRestriction<J> restriction
    ) {
      final Join<JE,J> join = this.join.join( attribute );
      context.getRestrictions( ).add( restriction.build( context.builder, join ) );
      return self( );
    }

    /**
     * Join on the given attribute and restriction.
     *
     * @param attribute The entity attribute that defines the relation
     * @param restriction Restriction on the joined entity
     * @param <J> The joined entity type
     * @return This criteria query for method chaining.
     */
    public <J> ECJT join(
        @Nonnull final SetAttribute<? super JE, J> attribute,
        @Nonnull final EntityRestriction<J> restriction
    ) {
      final Join<JE,J> join = this.join.join( attribute );
      context.getRestrictions( ).add( restriction.build( context.builder, join ) );
      return self( );
    }
  }

  /**
   * Entity type-safe bulk delete
   *
   * @param <E> The entity type
   */
  public static final class EntityCriteriaDelete<E> {
    private final EntityCriteriaDeleteContext<E> context;

    EntityCriteriaDelete( final EntityCriteriaDeleteContext<E> context ) {
      this.context = context;
    }

    /**
     * Add a where condition to the criteria.
     *
     * @param restriction The restriction to add.
     * @return This delete criteria for method chaining.
     */
    public EntityCriteriaDelete<E> where( @Nonnull final EntityRestriction<E> restriction ) {
      context.restrictions.add( restriction.build( context.builder, context.from ) );
      return this;
    }


    /**
     * Directly add an equality restriction to the join.
     *
     * <p><code>
     *   Entities.query( X.class )
     *     .join( X.relation ).whereEqual( Y_.name, "example" ).list( )
     * </code></p>

     * <p>This is a shorthand for:</p>
     *
     * <p><code>
     *   Entities.query( X.class )
     *     .join( X.relation ).where( Entities.restriction( Y.class ).equal( Y_.name, "example" ) ).list( )
     * </code></p>
     *
     * @return This delete criteria for method chaining.
     * @see Entities.EntityRestrictionBuilder#equal(SingularAttribute, Object)
     */
    public <V> EntityCriteriaDelete<E> whereEqual(
        @Nonnull final SingularAttribute<? super E, V> attribute,
        @Nonnull final V value
    ) {
      context.restrictions.add( new EntityRestriction.EqualPropertyEntityValueRestriction<>(
          context.entityClass,
          attribute,
          value
      ).build( context.builder, context.from ) );
      return this;
    }

    /**
     * Add a subselect contains condition to the criteria.
     *
     * @param attribute The attribute to check if contained
     * @param subqueryEntityClass The subquery entity class
     * @param subqueryAttribute The attribute of the subquery entity to match
     * @param subqueryCallback Callback to build subquery constraints.
     * @param <V> The contained value type
     * @param <S> The subquery entity type
     * @return This delete criteria for method chaining.
     */
    public <V,S> EntityCriteriaDelete<E> whereIn(
        @Nonnull final SingularAttribute<? super E, V> attribute,
        @Nonnull final Class<S> subqueryEntityClass,
        @Nonnull final SingularAttribute<? super S, V> subqueryAttribute,
        @Nonnull final EntityCriteriaSubqueryCallback<S,V> subqueryCallback ) {
      final EntityCriteriaSubquery<S,V> subquery = new EntityCriteriaSubquery<>(
          new PropertySubqueryEntityCriteriaQueryContext<>( context.delete, subqueryEntityClass, subqueryAttribute ) );
      subqueryCallback.restrict( subquery );
      context.restrictions.add( context.builder.in( context.from.get( attribute ) ).value( subquery.expression( ) ) );
      return this;
    }

    /**
     * Perform the deletion.
     *
     * @return The count of entities deleted.
     */
    public int delete( ) {
      return context.em.createQuery( context.delete ).executeUpdate( );
    }
  }
}
