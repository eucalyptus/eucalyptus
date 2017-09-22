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
package com.eucalyptus.walrus.msgs;

import java.util.List;
import org.jboss.netty.handler.stream.ChunkedInput;

public class WalrusDataGetResponseType extends WalrusDataResponseType {

  private List<ChunkedInput> dataInputStream;
  private Long byteRangeStart;
  private Long byteRangeEnd;

  public List<ChunkedInput> getDataInputStream( ) {
    return dataInputStream;
  }

  public void setDataInputStream( List<ChunkedInput> dataInputStream ) {
    this.dataInputStream = dataInputStream;
  }

  public Long getByteRangeStart( ) {
    return byteRangeStart;
  }

  public void setByteRangeStart( Long byteRangeStart ) {
    this.byteRangeStart = byteRangeStart;
  }

  public Long getByteRangeEnd( ) {
    return byteRangeEnd;
  }

  public void setByteRangeEnd( Long byteRangeEnd ) {
    this.byteRangeEnd = byteRangeEnd;
  }
}
