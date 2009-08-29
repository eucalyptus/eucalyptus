package com.eucalyptus.ws.util;

import java.util.List;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

import com.eucalyptus.config.ComponentConfiguration;
import com.eucalyptus.util.NetworkUtil;
import com.eucalyptus.ws.RemoteConfigurationException;

public class HeartBeatUtil {

  public static ChannelBuffer getConfigurationBuffer( ComponentConfiguration componentConfig ) throws Exception {
    List<String> possibleAddresses = NetworkUtil.getAllAddresses( );
    String configuration =  "euca.db.password=\neuca.db.port=9001\n";
    configuration += String.format( "euca.%s.name=%s\n", componentConfig.getClass( ).getSimpleName( ).replaceAll( "Configuration", "" ).toLowerCase( ), componentConfig.getName( ) );
    int i = 0;
    for( String s : possibleAddresses ) {
      configuration += String.format("euca.db.host.%d=%s\n",i++,s);
    }
    if( i == 0 ) {
      throw new RemoteConfigurationException("Failed to determine candidate database addresses.");//this should never happen
    }
    return ChannelBuffers.copiedBuffer( configuration.getBytes( ) );
  }

}
