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
 ************************************************************************/

package com.eucalyptus.component;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.util.FullName;
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
    assertThat( componentType, notNullValue( ) );
    
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
    this.relativeId = ( SEP_PATH + Joiner.on( SEP_PATH ).join( pathPartsArray ) ).replaceAll( "^//", "/" );
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
