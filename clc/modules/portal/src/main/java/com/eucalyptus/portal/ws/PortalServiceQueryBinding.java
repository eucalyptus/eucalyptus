/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
package com.eucalyptus.portal.ws;

import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.portal.common.Portal;
import com.eucalyptus.portal.common.model.PortalMessage;
import com.eucalyptus.ws.protocol.BaseQueryJsonBinding;
import com.eucalyptus.ws.protocol.OperationParameter;
import javaslang.control.Option;

/**
 *
 */
@SuppressWarnings( "WeakerAccess" )
@ComponentPart( Portal.class )
public class PortalServiceQueryBinding  extends BaseQueryJsonBinding<OperationParameter> {

  public PortalServiceQueryBinding( ) {
    super(
        PortalMessage.class,
        Option.some( "application/json" ),
        UnknownParameterStrategy.ERROR,
        OperationParameter.Action );
  }

}
