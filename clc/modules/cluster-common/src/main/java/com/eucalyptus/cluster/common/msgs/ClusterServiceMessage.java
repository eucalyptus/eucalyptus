/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE.
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
