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

package com.eucalyptus.component;

import static com.eucalyptus.util.Parameters.checkParam;
import static org.hamcrest.Matchers.notNullValue;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.auth.principal.FullName;
import com.google.common.base.Joiner;

public class ComponentFullName implements FullName {
  public final static String  VENDOR = "euca";
  private final ComponentId   componentId;
  private final ComponentId   realComponentId;
  private final String        partition;
  private final String        name;
  private final String        qName;
  private final String        authority;
  private final String        relativeId;
  private final static String PREFIX = "arn:euca";
  
  ComponentFullName( ComponentId componentType, String partition, String name, String... pathPartsArray ) {
    checkParam( componentType, notNullValue() );
    
    this.realComponentId = componentType;
    this.name = name != null
      ? name
      : "null";
    partition = partition != null
      ? partition
      : "null";
    boolean hasParentComponent = this.realComponentId.partitionParent( ) != null;
    ComponentId tempComponentId = Empyrean.INSTANCE;
    String tempPartition = "";
    if ( hasParentComponent || this.realComponentId.isPartitioned( ) ) {
      if ( this.realComponentId.isCloudLocal( ) ) {
        tempComponentId = Eucalyptus.INSTANCE;
      } else if ( this.realComponentId.isAlwaysLocal( ) ) {
        tempComponentId = Empyrean.INSTANCE;
      }
      tempPartition = partition;
    } else if ( !hasParentComponent && !this.realComponentId.isPartitioned( ) ) {
      tempComponentId = this.realComponentId;
      tempPartition = this.realComponentId.name( );
    } else if ( !this.realComponentId.isPartitioned( ) && hasParentComponent ) {
      ComponentId parentId = this.realComponentId.partitionParent( );
      if ( parentId.getClass( ).equals( Eucalyptus.class ) ) {
        tempComponentId = Eucalyptus.INSTANCE;
        tempPartition = tempComponentId.name( );
      } else {
        tempComponentId = Empyrean.INSTANCE;
        tempPartition = tempComponentId.name( );
      }
    }
    this.componentId = tempComponentId;
    this.partition = tempPartition;
    
    String displayPartition = ( this.componentId.name( ).equals( this.partition ) )
      ? ""
      : this.partition;
    String displayCompType = ( this.realComponentId.equals( this.componentId ) )
      ? ""
      : this.realComponentId.name( );
    this.authority = Joiner.on( SEP ).join( PREFIX, this.componentId.name( ), displayPartition, displayCompType, this.name );
    String relIdStr = SEP_PATH + Joiner.on( SEP_PATH ).join( pathPartsArray );
    this.relativeId = relIdStr.startsWith( "//" ) ? relIdStr.substring( 1 ) : relIdStr;
    this.qName = this.authority + this.relativeId;
  }
  
  ComponentFullName( ServiceConfiguration config, String... parts ) {
    this( config.getComponentId( ), config.getPartition( ), config.getName( ), parts );
  }
  
  @Override
  public final String getVendor( ) {
    return VENDOR;
  }
  
  @Override
  public final String getRegion( ) {
    return this.getPartition( );
  }
  
  @Override
  public final String getNamespace( ) {
    return this.componentId.getName( );
  }
  
  @Override
  public final String getAuthority( ) {
    return this.authority;
  }
  
  @Override
  public final String getRelativeId( ) {
    return this.relativeId;
  }
  
  @Override
  public final String getPartition( ) {
    return this.partition;
  }
  
  @Override
  public String toString( ) {
    return this.qName;
  }
  
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( this.name == null )
      ? 0
      : this.name.hashCode( ) );
    result = prime * result + ( ( this.partition == null )
      ? 0
      : this.partition.hashCode( ) );
    return result;
  }
  
  @Override
  public boolean equals( Object obj ) {
    if ( this == obj ) {
      return true;
    }
    if ( obj == null ) {
      return false;
    }
    if ( !this.getClass( ).equals( obj.getClass( ) ) ) {
      return false;
    }
    
    ComponentFullName that = ( ComponentFullName ) obj;
    if ( this.name == null ) {
      if ( that.name != null ) {
        return false;
      }
    } else if ( !this.name.equals( that.name ) ) {
      return false;
    }
    if ( this.partition == null ) {
      if ( that.partition != null ) {
        return false;
      }
    } else if ( !this.partition.equals( that.partition ) ) {
      return false;
    }
    return true;
  }
  
  @Override
  public String getUniqueId( ) {
    return this.name;
  }
  
  public static FullName getInstance( ServiceConfiguration config, String... parts ) {
    return new ComponentFullName( config, parts );
  }
}
