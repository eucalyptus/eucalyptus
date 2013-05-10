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

package com.eucalyptus.vm;

import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.util.async.AsyncRequests;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import edu.ucsb.eucalyptus.msgs.CreateTagsType;
import edu.ucsb.eucalyptus.msgs.DeleteResourceTag;
import edu.ucsb.eucalyptus.msgs.DeleteTagsType;
import edu.ucsb.eucalyptus.msgs.ResourceTag;
import edu.ucsb.eucalyptus.msgs.ResourceTagMessage;

enum MigrationTags implements Predicate<VmInstance> {
  STATE,
  SOURCE,
  DESTINATION;
  private static Logger LOG = Logger.getLogger( MigrationTags.class );
  
  public String toString( ) {
    return "euca:node:migration:" + this.name( ).toLowerCase( );
  }
  
  public static void deleteFor( final VmInstance vm ) {
    final DeleteTagsType deleteTags = new DeleteTagsType( );
    deleteTags.getTagSet( ).add( MigrationTags.STATE.deleteTag( ) );
    deleteTags.getTagSet( ).add( MigrationTags.SOURCE.deleteTag( ) );
    deleteTags.getTagSet( ).add( MigrationTags.DESTINATION.deleteTag( ) );
    deleteTags.getResourcesSet( ).add( vm.getInstanceId( ) );
    try {
      deleteTags.setEffectiveUserId( Accounts.lookupAccountByName( "eucalyptus" ).lookupAdmin( ).getUserId( ) );
    } catch ( AuthException ex ) {
      LOG.error( ex );
    }
    dispatch( deleteTags );
  }
  
  private static void dispatch( final ResourceTagMessage tagMessage ) {
    try {
      AsyncRequests.dispatch( Topology.lookup( Eucalyptus.class ), tagMessage );
    } catch ( Exception ex ) {
      LOG.trace( ex );
    }
  }
  
  public static void createFor( final VmInstance vm ) {
    final VmMigrationTask migrationTask = vm.getRuntimeState( ).getMigrationTask( );
    final CreateTagsType createTags = new CreateTagsType( );
    createTags.getTagSet( ).add( MigrationTags.STATE.getTag( migrationTask.getState( ).name( ) ) );
    if ( !Strings.isNullOrEmpty( migrationTask.getSourceHost( ) ) ) {
      createTags.getTagSet( ).add( MigrationTags.SOURCE.getTag( migrationTask.getSourceHost( ) ) );
    }
    if ( !Strings.isNullOrEmpty( migrationTask.getDestinationHost( ) ) ) {
      createTags.getTagSet( ).add( MigrationTags.DESTINATION.getTag( migrationTask.getDestinationHost( ) ) );
    }
    createTags.getResourcesSet( ).add( vm.getInstanceId( ) );
    try {
      createTags.setEffectiveUserId( Accounts.lookupAccountByName( "eucalyptus" ).lookupAdmin( ).getUserId( ) );
    } catch ( AuthException ex ) {
      LOG.error( ex );
    }
    dispatch( createTags );
  }
  
  ResourceTag getTag( String value ) {
    return new ResourceTag( this.toString( ), value );
  }
  
  private DeleteResourceTag deleteTag( ) {
    DeleteResourceTag rsrcTag = new DeleteResourceTag( );
    rsrcTag.setKey( this.toString( ) );
    return rsrcTag;
  }
  
  @Override
  public boolean apply( @Nullable VmInstance input ) {
    VmMigrationTask task = input.getRuntimeState( ).getMigrationTask( );
    if ( MigrationState.none.equals( task.getState( ) ) ) {
      MigrationTags.deleteFor( input );
    } else {
      MigrationTags.createFor( input );
    }
    return true;
  }
  
  public static void update( VmInstance input ) {
    SOURCE.apply( input );
  }
}
