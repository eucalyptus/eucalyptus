/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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

package com.eucalyptus.util;

import static com.eucalyptus.upgrade.Upgrades.Version.v3_2_0;
import java.io.Serializable;
import java.util.concurrent.Callable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Databases;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.crypto.Digest;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.upgrade.Upgrades.PreUpgrade;
import com.eucalyptus.util.UniqueIds.PersistedCounter.Transaction;
import com.google.common.base.Function;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import groovy.sql.Sql;

public class UniqueIds implements Serializable {
  private static final long serialVersionUID = 1L;
  private static Logger     LOG              = Logger.getLogger( UniqueIds.class );
  private static final Long BLOCK_SIZE       = 1000L;
  
  @Entity
  @PersistenceContext( name = "eucalyptus_config" )
  @Table( name = "config_unique_ids_sets" )
  public static class PersistedCounter extends AbstractPersistent {
    enum Transaction implements Function<Long, String> {
      NEXT_INDEX {
        
        @Override
        public String apply( Long arg0 ) {
          return Crypto.getDigestBase64( Long.toString( arg0 ), Digest.SHA512 ).replaceAll( "\\.", "" );
        }
        
      }
      
    }
    
    @Transient
    private static final long serialVersionUID = 1L;
    @Column( name = "config_unique_ids_current_base" )
    private Long              currentBase;
    @Column( name = "config_unique_ids_set_name", unique = true )
    private String            idSetName;
    
    private PersistedCounter( ) {
      super( );
    }
    
    private PersistedCounter( final Long counter, final String counterName ) {
      this.currentBase = counter;
      this.idSetName = counterName;
    }
    
    private Long nextBlock( Long size ) {
      Long oldBase = this.getCurrentBase( ) + 1;
      this.setCurrentBase( oldBase + size );
      return oldBase;
    }
    
    private Long getCurrentBase( ) {
      return this.currentBase;
    }
    
    private void setCurrentBase( Long currentBase ) {
      this.currentBase = currentBase;
    }
    
    private String getIdSetName( ) {
      return this.idSetName;
    }
    
    private void setIdSetName( String idSetName ) {
      this.idSetName = idSetName;
    }
    
  }
  
  public static Long nextIndex( Class c, long extent ) {
    return nextIndex( c.toString( ), extent );
  }
  
  public static String nextId( Class c ) {
    return nextId( c.toString( ) );
  }
  
  public static String nextId( ) {
    return nextId( "SYSTEM" );
  }
  
  private static String nextId( final String counterName ) {
    return Transaction.NEXT_INDEX.apply( nextIndex( counterName, 1 ) );
  }
  
  private static final LoadingCache<String,Function<Long, Long>> counterMap = CacheBuilder.newBuilder().build(
    new CacheLoader<String,Function<Long, Long>>(){
    @Override
    public Function<Long, Long> load( final String counterName ) {
      return new Function<Long, Long>( ) {
        @Override
        public Long apply( Long arg0 ) {
          Long ret = 0l;
          try {
            final PersistedCounter entity = Entities.uniqueResult( new PersistedCounter( null, counterName ) );
            ret = entity.nextBlock( arg0 );
            return ret;
          } catch ( final Exception ex ) {
            try {
              final PersistedCounter entity = Entities.persist( new PersistedCounter( 0L, counterName ) );
              ret = entity.nextBlock( arg0 );
              return ret;
            } catch ( final Exception ex1 ) {
              LOG.error( ex1, ex1 );
              throw Exceptions.toUndeclared( "Failed to initialize counter for: " + counterName + " because of: " + ex.getMessage( ), ex );
            }
          }
        }
      };
    }} );
  
  private static Long nextIndex( final String counterName, long extent ) {
    return Entities.asTransaction( PersistedCounter.class, counterMap.getUnchecked( counterName ), 1000 ).apply( extent );
    
  }
  
  @PreUpgrade( value = Empyrean.class,
               since = v3_2_0 )
  public enum DropOldTable implements Callable<Boolean> {
    INSTANCE;
    @Override
    public Boolean call( ) throws Exception {
      Sql sql = null;
      try {
        sql = Databases.getBootstrapper( ).getConnection( "eucalyptus_config" );
        sql.execute( "drop table if exists config_unique_ids" );
        return true;
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
        return false;
      } finally {
        if ( sql != null ) {
          sql.close( );
        }
      }
    }
  }
}
