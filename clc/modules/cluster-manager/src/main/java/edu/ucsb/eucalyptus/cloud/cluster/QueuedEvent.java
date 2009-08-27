/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package edu.ucsb.eucalyptus.cloud.cluster;

import com.eucalyptus.ws.client.Client;
import org.apache.log4j.Logger;

public class QueuedEvent<TYPE> {
  private static Logger LOG = Logger.getLogger( QueuedEvent.class );

  private QueuedEventCallback<TYPE> callback;
  private TYPE event;

  public static <T> QueuedEvent<T> make( final QueuedEventCallback callback, final T event ) {
    return new QueuedEvent<T>(callback,event);
  }

  public QueuedEvent( final QueuedEventCallback callback, final TYPE event )
  {
    this.callback = callback;
    this.event = event;
  }

  public QueuedEventCallback getCallback()
  {
    return callback;
  }

  public TYPE getEvent()
  {
    return event;
  }

  public void trigger( Client cluster )
  {
    try
    {
      this.callback.process( cluster, this.event );
    }
    catch ( Exception e )
    {
      LOG.error( e );
      LOG.debug( e, e );
    }
  }

  @Override
  public boolean equals( final Object o )
  {
    if ( this == o ) return true;
    if ( !( o instanceof QueuedEvent ) ) return false;

    QueuedEvent that = ( QueuedEvent ) o;

    if ( !callback.equals( that.callback ) ) return false;
    if ( !event.equals( that.event ) ) return false;

    return true;
  }

  @Override
  public int hashCode()
  {
    int result = callback.hashCode();
    result = 31 * result + event.hashCode();
    return result;
  }
}
