/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.portal.ws;

import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.portal.common.Ec2Reports;
import com.eucalyptus.portal.common.model.Ec2ReportsMessage;
import com.eucalyptus.ws.protocol.BaseQueryJsonBinding;
import com.eucalyptus.ws.protocol.OperationParameter;
import javaslang.control.Option;

@SuppressWarnings( "WeakerAccess" )
@ComponentPart( Ec2Reports.class )
public class Ec2ReportsServiceQueryBinding extends BaseQueryJsonBinding<OperationParameter> {
  public Ec2ReportsServiceQueryBinding(){
    super(
            Ec2ReportsMessage.class,
            Option.some( "application/json" ),
            UnknownParameterStrategy.ERROR,
            OperationParameter.Action );
  }
}
