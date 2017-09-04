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
package com.eucalyptus.config;

import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.empyrean.PropertiesService;

/**
 *
 */
@ComponentPart( PropertiesService.class )
public interface PropertiesClient {

  // sync

  DescribePropertiesResponseType describeProperties( DescribePropertiesType request );

  ModifyPropertyValueResponseType modifyProperty( ModifyPropertyValueType request );

  // convenience

  default ModifyPropertyValueResponseType modifyProperty( final String name, final String value ) {
    final ModifyPropertyValueType modifyPropertyValue = new ModifyPropertyValueType( );
    modifyPropertyValue.setName( name );
    modifyPropertyValue.setValue( value );
    return modifyProperty( modifyPropertyValue );
  }

}
