/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 * <p>
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.portal.ws;

import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.portal.common.Tag;
import com.eucalyptus.portal.common.model.TagMessage;
import com.eucalyptus.ws.protocol.BaseQueryJsonBinding;
import com.eucalyptus.ws.protocol.OperationParameter;
import javaslang.control.Option;

/**
 *
 */
@SuppressWarnings( "WeakerAccess" )
@ComponentPart( Tag.class )
public class TagServiceQueryBinding extends BaseQueryJsonBinding<OperationParameter> {

  public TagServiceQueryBinding( ) {
    super(
        TagMessage.class,
        Option.some( "application/json" ),
        UnknownParameterStrategy.ERROR,
        OperationParameter.Action );
  }

}
