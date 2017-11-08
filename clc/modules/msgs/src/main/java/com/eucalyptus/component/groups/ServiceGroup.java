/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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
package com.eucalyptus.component.groups;

import java.util.Collection;
import java.util.Map;

import org.apache.log4j.Logger;

import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.annotation.Partition;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */
@Partition( value = { ServiceGroup.class } )
public abstract class ServiceGroup extends ComponentId implements Predicate<ComponentId>, Supplier<ServiceGroup> {
  private static Logger LOG = Logger.getLogger( ServiceGroup.class );
  
  protected ServiceGroup( String name ) {
    super( name );
  }
  
  protected ServiceGroup( ) {
    super( );
  }
  
  @Override
  public abstract boolean apply( ComponentId input );
  
  @Override
  public ServiceGroup get( ) {
    return ComponentIds.lookup( this.getClass( ) );
  }
  
  /**
   * Never partitioned. Reproduced for reference.
   * 
   * @see com.eucalyptus.component.ComponentId#isPartitioned()
   */
  @Override
  public boolean isPartitioned( ) {
    return false;
  }
  
  /**
   * Always registerable. Reproduced for reference.
   * 
   * @see com.eucalyptus.component.ComponentId#isRegisterable()
   */
  @Override
  public boolean isRegisterable( ) {
    return super.isRegisterable( );
  }
  
  public Supplier<Map<ComponentId, Boolean>> groupMemberSupplier( ) {
    return new Supplier<Map<ComponentId, Boolean>>( ) {
      @Override
      public Map<ComponentId, Boolean> get( ) {
        return Maps.asMap( Sets.newHashSet( ComponentIds.list( ) ), Functions.forPredicate( ServiceGroup.this ) );
      }
    };
  }

  public Collection<? extends ComponentId> list() {
    return Collections2.filter( ComponentIds.list( ), this );
  }

}
