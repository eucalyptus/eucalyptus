package com.eucalyptus.images;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.StorageProperties;
import com.eucalyptus.util.WalrusProperties;
import com.eucalyptus.ws.util.Messaging;

public class StorageProxy {
  private static String getAddress( String name ) {
    return Component.storage.getUri( ).toASCIIString( );
  }
  
  @SuppressWarnings( "unchecked" )
  public static <REPLY> REPLY send( String scName, Object message, Class<REPLY> replyType ) throws EucalyptusCloudException {
    return (REPLY) Messaging.send(StorageProxy.getAddress( scName ), message);
  }

  public static void dispatch( String scName, Object message ) throws EucalyptusCloudException {
    Messaging.dispatch(StorageProxy.getAddress( scName ), message);
  }


}
