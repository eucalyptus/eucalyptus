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
package edu.ucsb.eucalyptus.msgs;

import java.beans.PropertyDescriptor;
import org.springframework.beans.BeanUtils;
import com.eucalyptus.util.Exceptions;

public class EucalyptusData implements BaseData {

  public String toString( ) {
    final StringBuilder builder = new StringBuilder( );
    builder.append( "[" );
    for ( final PropertyDescriptor descriptor : BeanUtils.getPropertyDescriptors(getClass()) ) {
      builder.append( descriptor.getName( ) );
      builder.append( ":" );
      try {
        builder.append( descriptor.getReadMethod( ).invoke( this ) );
      } catch ( final Exception e ) {
      }
    }
    builder.append( "]" );
    return builder.toString( );
  }

  public Object clone( ) {
    try {
      return super.clone( );
    } catch ( CloneNotSupportedException e ) {
      throw Exceptions.toUndeclared( e );
    }
  }

}
