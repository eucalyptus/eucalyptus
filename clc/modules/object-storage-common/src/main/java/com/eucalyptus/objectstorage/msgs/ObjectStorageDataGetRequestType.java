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
package com.eucalyptus.objectstorage.msgs;

import java.util.Map;
import org.jboss.netty.channel.Channel;

public class ObjectStorageDataGetRequestType extends ObjectStorageDataRequestType {

  protected Channel channel;
  private Map<String, String> responseHeaderOverrides;

  public ObjectStorageDataGetRequestType( ) {
  }

  public ObjectStorageDataGetRequestType( String bucket, String key ) {
    super( bucket, key );
  }

  public Channel getChannel( ) {
    return channel;
  }

  public void setChannel( Channel channel ) {
    this.channel = channel;
  }

  public Map<String, String> getResponseHeaderOverrides( ) {
    return responseHeaderOverrides;
  }

  public void setResponseHeaderOverrides( Map<String, String> responseHeaderOverrides ) {
    this.responseHeaderOverrides = responseHeaderOverrides;
  }
}
