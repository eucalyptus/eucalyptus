/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.vmtypes

import groovy.sql.Sql
import java.util.concurrent.Callable
import org.apache.log4j.Logger
import com.eucalyptus.component.id.Eucalyptus
import com.eucalyptus.entities.Entities
import com.eucalyptus.scripting.Groovyness
import com.eucalyptus.upgrade.Upgrades.DatabaseFilters
import com.eucalyptus.upgrade.Upgrades.EntityUpgrade
import com.eucalyptus.upgrade.Upgrades.PostUpgrade
import com.eucalyptus.upgrade.Upgrades.PreUpgrade
import com.eucalyptus.upgrade.Upgrades.Version
import com.google.common.base.Function
import com.google.common.base.Predicate
import com.google.common.collect.Maps
import groovy.transform.TypeCheckingMode
import groovy.transform.CompileStatic

/**
 * This upgrade is somewhat complicated because stopped instances may have a primary key reference
 * to the old vm types. This class contains 3 enums {@link PRE}, {@link ENTITY}, {@link POST} 
 * (corresponding to the related upgrade stage).  One portion deserves additional comment as it happens 
 * prior to the upgrade phases, otherwise, more comments are associated with each enum.
 * 
 * <p>
 * <strong>SchemaUpdate</strong>
 * <ol>
 * <li>Rename the {@link VmType} table to be <tt>cloud_vm_type</tt> leaving the old definitions in
 * <tt>cloud_vm_types</tt> (schema update)
 * <li>Rename the {@link JoinColumn} used to reference the {@link VmType} in {@link VmBootRecord}
 * from <tt>vmtype_id</tt> to be <tt>metadata_vm_type_id</tt> (schema update)
 * </ol>
 * </p>
 * <p>
 * 
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */
@CompileStatic(TypeCheckingMode.SKIP)
public class VmTypes330Upgrade {
  private static Logger LOG = Logger.getLogger( VmTypes330Upgrade.class );
  static class VmTypeInfo {
    String  id;
    String name;
    Integer cpu;
    Integer disk;
    Integer memory;
    VmTypeInfo( String id, String name, Integer cpu, Integer disk, Integer memory ) {
      this.id = id;
      this.name = name;
      this.cpu = cpu;
      this.disk = disk;
      this.memory = memory;
    }
  }
  
  public static SQL(closure) {
    Sql sql = null;
    try {
      sql = DatabaseFilters.NEWVERSION.getConnection( "eucalyptus_cloud" );
      def result = closure(sql);
      return result;
    } catch ( Exception ex ) {
      throw ex;
    } finally {
      if ( sql != null ) {
        sql.close( );
      }
    }
  }
  
  public static TX(type,closure) {
    return { arg ->
      def func = { input ->  return closure(input)  } as Function
      def tx = Entities.asTransaction( type, func );
      def entity = tx.apply( arg );
      LOG.info( "Completed transaction for " + entity?.dump() )
      return entity != null ? Groovyness.expandoMetaClass( entity ) : entity
    }
  }
  
  
  /**
   * <p>
   * <strong>PreUpgrade</strong>
   * <ol>
   * <li>Drop the foreign key constraint between <tt>metadata_instances.vmtype_id</tt> and <tt>cloud_vm_types.id</tt> so we can remove them safely
   * <li>Read all the values from <tt>cloud_vm_types</tt> and store the <tt>config_vm_type_name</tt>
   * to <tt>id</tt> mapping along w/ the cpu/disk/memory settings for later use
   * </ol>
   * </p>
   * <p>
   * @author chris grzegorczyk <grze@eucalyptus.com>
   */
  @PreUpgrade( value = Eucalyptus.class,
  since = Version.v3_3_0 )
  @CompileStatic(TypeCheckingMode.SKIP)
  public static class PRE implements Callable<Boolean> {
    private static Logger LOG = Logger.getLogger( VmTypes330Upgrade.PRE.class );
    static final Map<String, VmTypeInfo> oldVmTypes = Maps.newHashMap( );
    
    /**
     * @see java.util.concurrent.Callable#call()
     */
    @Override
    public Boolean call( ) throws Exception {
      /**
       * Read the old vm types
       */
      SQL { Sql sql ->
        sql.eachRow('select * from cloud_vm_types', {
          def oldVmTypeInfo = new VmTypeInfo(it.id,
              it.metadata_vm_type_name,
              it.metadata_vm_type_cpu,
              it.metadata_vm_type_disk,
              it.metadata_vm_type_memory)
          oldVmTypes.put( oldVmTypeInfo.name, oldVmTypeInfo )
          LOG.info( "Saving old vm type info: " + oldVmTypeInfo.dump( ) )
          it
        })
      }
      /**
       * Find the foreign key constraint
       */
      def fkName = SQL({ Sql sql ->
        String fkQuery = """
SELECT
    tc.constraint_name, tc.table_name, kcu.column_name, 
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name 
FROM 
    information_schema.table_constraints AS tc 
    JOIN information_schema.key_column_usage AS kcu ON tc.constraint_name = kcu.constraint_name
    JOIN information_schema.constraint_column_usage AS ccu ON ccu.constraint_name = tc.constraint_name
WHERE constraint_type = 'FOREIGN KEY' AND tc.table_name='metadata_instances' AND ccu.table_name='cloud_vm_types'
"""
        sql.rows(fkQuery)?.constraint_name
      })
      
      /**
       * Remove the foreign key constraint
       */
      fkName.collect{ String fk ->
        LOG.info("Found constraint ${fk} between 'metadata_instances' and 'cloud_vm_types'");
        SQL({ Sql sql ->
          LOG.info("Dropping constraint ${fk} between 'metadata_instances' and 'cloud_vm_types'");
          def dropSql = 'ALTER TABLE metadata_instances DROP CONSTRAINT IF EXISTS '+fk
          LOG.info("\t${dropSql}");
          sql.execute(dropSql)
        })
      }

      true
    }
  }
  
  /**
   * <strong>EntityUpgrade</strong>
   * <ol>
   * <li>Store all of the new {@link VmType}s using the new table <tt>cloud_vm_type</tt>
   * <li>Update all {@link VmBootRecord} references to the old primary keys.  For each {@link VmType}:
   * <ol>
   * <li>Check to see if we previously had this {@link VmType}
   * <li>If so, update <tt>metadata_vm_type_id</tt> in {@link VmBootRecord} where ever <tt>vmtype_id</tt> refers to the old {@link VmType#getId()}
   * </ol>
   * </ol>
   * </p>
   * <p>
   * @author chris grzegorczyk <grze@eucalyptus.com>
   */
  @EntityUpgrade( entities = [ VmType.class ], since = Version.v3_3_0, value = Eucalyptus.class )
  @CompileStatic(TypeCheckingMode.SKIP)
  public static class ENTITY implements Predicate<Class> {
    private static Logger LOG = Logger.getLogger( VmTypes330Upgrade.ENTITY.class );

    /**
     * Copy over any settings from previous {@link VmType}s.
     */
    def upgradeVmTypes = TX( VmType.class, { VmType vmType ->
      vmType = Groovyness.expandoMetaClass( vmType );
      if ( PRE.oldVmTypes.containsKey( vmType.getName( ) ) ) {
        def oldVmTypeInfo = PRE.oldVmTypes.get( vmType.getName( ) );
        vmType.setCpu( oldVmTypeInfo.getCpu( ) )
        vmType.setDisk( oldVmTypeInfo.getDisk( ) )
        vmType.setMemory( oldVmTypeInfo.getMemory( ) )
        VmTypes.update( vmType )
        LOG.info( "Upgraded vm type info:    " + vmType.dump( ) )
      } else {
        LOG.info( "Defined new vm type info: " + vmType.dump( ) )
      }
      return vmType;
    });
    
    /**
     * Update the {@link VmBootRecord} references to use the new type and foreign key column
     */
    def updateVmBootRecords = TX( VmType.class, { VmType vmType ->
      vmType = Groovyness.expandoMetaClass( vmType );
      LOG.info( vmType.dump() )
      if ( PRE.oldVmTypes.containsKey( vmType.getName( ) ) ) {
        LOG.info( "Updating instances refering to ${vmType.getName( )}'s metadata_vm_type_id." )
        def oldVmTypeInfo = PRE.oldVmTypes.get( vmType.getName( ) );
        SQL { Sql sql ->
          int results = sql.executeUpdate( 'update metadata_instances set metadata_vm_type_id=? where vmtype_id=?', [vmType.id, oldVmTypeInfo.id])
          LOG.info( "Changed ${results} instances from ${oldVmTypeInfo.id} to ${vmType.id}." )
        }
      } else {
        LOG.info( "No instances updated which refered to ${vmType.getName( )}" )
      }
      return vmType;
    });
    
    @Override
    public boolean apply( Class arg0 ) {
      VmTypes.list( ).each{ upgradeVmTypes(it) }
      VmTypes.list( ).each{ updateVmBootRecords(it) }
      return true;
    }
  }
  
  /**
   * <strong>PostUpgrade</strong>
   * <ol>
   * <li>Drop the old <tt>cloud_vm_types</tt> table
   * <li>Remove the old <tt>vmtype_id</tt> column from {@link VmBootRecord}
   * </ol>
   * </p>
   * @author chris grzegorczyk <grze@eucalyptus.com>
   */
  @PostUpgrade( value = Eucalyptus.class, since = Version.v3_3_0 )
  @CompileStatic(TypeCheckingMode.SKIP)
  public static class POST implements Callable<Boolean> {
    private static Logger LOG = Logger.getLogger( VmTypes330Upgrade.POST.class );
    
    @Override
    public Boolean call( ) throws Exception {
      Sql sql = null;
      try {
        sql = DatabaseFilters.NEWVERSION.getConnection( "eucalyptus_cloud" );
        LOG.info("Dropping old table")
        sql.execute( "DROP TABLE IF EXISTS cloud_vm_types" );
        sql.execute( "ALTER TABLE metadata_instances DROP COLUMN IF EXISTS vmtype_id" );
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

