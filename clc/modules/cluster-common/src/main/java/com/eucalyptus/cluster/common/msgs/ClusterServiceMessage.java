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
package com.eucalyptus.cluster.common.msgs;

import java.util.ArrayList;
import org.jibx.runtime.IMarshallable;
import org.jibx.runtime.IMarshallingContext;
import org.jibx.runtime.IUnmarshallable;
import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.JiBXException;
import com.eucalyptus.cluster.common.ClusterController;
import com.eucalyptus.component.annotation.ComponentMessage;
import com.eucalyptus.empyrean.ServiceId;
import edu.ucsb.eucalyptus.msgs.BaseMessageMarker;

@ComponentMessage( ClusterController.class )
public interface ClusterServiceMessage extends BaseMessageMarker, IMarshallable, IUnmarshallable {

  String getCorrelationId( );

  void setCorrelationId( String correlationId );

  String getUserId( );

  void setUserId( String userId );

  String getStatusMessage( );

  void setStatusMessage( String statusMessage );

  Boolean get_return( );

  void set_return( Boolean returnValue );

  Integer get_epoch( );

  void set_epoch( Integer epoch );

  ArrayList<ServiceId> get_services( );

  void set_services( ArrayList<ServiceId> services );

  ArrayList<ServiceId> get_disabledServices( );

  void set_disabledServices( ArrayList<ServiceId> services );

  ArrayList<ServiceId> get_notreadyServices( );

  void set_notreadyServices( ArrayList<ServiceId> services );

  @Override
  default String JiBX_getName( ) {
    return null;
  }

  @Override
  default void marshal( IMarshallingContext ctx ) throws JiBXException {
  }

  @Override
  default String JiBX_className( ) {
    return null;
  }

  @Override
  default void unmarshal( IUnmarshallingContext ctx ) throws JiBXException {
  }
}
