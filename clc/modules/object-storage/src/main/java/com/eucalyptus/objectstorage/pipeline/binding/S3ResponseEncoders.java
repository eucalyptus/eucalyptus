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
package com.eucalyptus.objectstorage.pipeline.binding;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import com.eucalyptus.objectstorage.msgs.ObjectStorageResponseType;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import javaslang.Tuple2;

/**
 *
 */
public class S3ResponseEncoders {

  private static final List<S3ResponseEncoder> encoders = new CopyOnWriteArrayList<>( );

  static void register( final S3ResponseEncoder encoder ) {
    encoders.add( encoder );
  }

  static Tuple2<String,byte[]> encode( final BaseMessage message ) throws IOException {
    if ( message instanceof ObjectStorageResponseType ) {
      final ObjectStorageResponseType responseType = (ObjectStorageResponseType) message;
      for ( final S3ResponseEncoder encoder : encoders ) {
        if ( encoder.canEncode( responseType ) ) {
          return encoder.encode( responseType );
        }
      }
    }
    return null;
  }

}
