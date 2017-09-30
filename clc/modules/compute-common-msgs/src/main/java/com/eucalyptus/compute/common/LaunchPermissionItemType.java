/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.compute.common;

import java.util.Objects;
import com.eucalyptus.util.CompatFunction;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class LaunchPermissionItemType extends EucalyptusData {

  private String userId;
  private String group;

  public LaunchPermissionItemType( ) {
  }

  public LaunchPermissionItemType( final String userId, final String group ) {
    this.userId = userId;
    this.group = group;
  }

  public static LaunchPermissionItemType newUserLaunchPermission( String userId ) {
    return new LaunchPermissionItemType( userId, null );
  }

  public static LaunchPermissionItemType newGroupLaunchPermission( ) {
    return new LaunchPermissionItemType( null, "all" );
  }

  public static CompatFunction<String, LaunchPermissionItemType> forUser( ) {
    return new CompatFunction<String, LaunchPermissionItemType>( ) {
      @Override
      public LaunchPermissionItemType apply( final String userId ) {
        return new LaunchPermissionItemType( userId, null );
      }
    };
  }

  public static CompatFunction<String, LaunchPermissionItemType> forGroup( ) {
    return new CompatFunction<String, LaunchPermissionItemType>( ) {
      @Override
      public LaunchPermissionItemType apply( final String group ) {
        return new LaunchPermissionItemType( null, group );
      }
    };
  }

  public boolean user( ) {
    return this.userId != null;
  }

  public boolean group( ) {
    return this.group != null;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final LaunchPermissionItemType that = (LaunchPermissionItemType) o;
    return Objects.equals( getUserId( ), that.getUserId( ) ) &&
        Objects.equals( getGroup( ), that.getGroup( ) );
  }

  @Override
  public int hashCode( ) {
    return Objects.hash( getUserId( ), getGroup( ) );
  }

  public String getUserId( ) {
    return userId;
  }

  public void setUserId( String userId ) {
    this.userId = userId;
  }

  public String getGroup( ) {
    return group;
  }

  public void setGroup( String group ) {
    this.group = group;
  }
}
