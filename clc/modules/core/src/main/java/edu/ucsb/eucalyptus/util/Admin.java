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
package edu.ucsb.eucalyptus.util;

import com.eucalyptus.bootstrap.Component;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;
import org.apache.log4j.Logger;

import java.lang.reflect.Constructor;

public class Admin {

  private static Logger LOG = Logger.getLogger( Admin.class );

  public static <E extends EucalyptusMessage> E makeMsg( Class<E> clazz, EucalyptusMessage regarding ) {
    try {
      E msg = clazz.newInstance();
      msg.setUserId( Component.eucalyptus.name( ) );
      msg.setEffectiveUserId( Component.eucalyptus.name( ) );
      StackTraceElement elem = new Throwable().fillInStackTrace().getStackTrace()[ 1 ];
      msg.setCorrelationId( String.format( "%s.%s.%s-%s", elem.getClassName(), elem.getMethodName(), elem.getLineNumber(), regarding.getCorrelationId() ) );
      return msg;
    } catch ( Exception e ) {
      LOG.error( e, e );
      return null;
    }
  }

  private static boolean accepts( Class[] formal, Object[] actual ) {
    if ( formal.length != actual.length ) return false;
    boolean ret = true;
    for ( int i = 0; i < actual.length; i++ ) ret &= formal[ i ].isAssignableFrom( actual[ i ].getClass() );
    return ret;
  }

  public static <E extends EucalyptusMessage> E makeMsg( Class<E> clazz, Object... args ) {
    try {
      E msg = null;
      if ( args.length != 0 ) {
        for ( Constructor c : clazz.getConstructors() ) {
          if ( accepts( c.getParameterTypes(), args ) ) {
            msg = ( E ) c.newInstance( args );
          }
        }
      }
      if ( msg == null ) {
        if ( args.length > 0 )
          LOG.error( "Failed to find a constructor for requested message: " + clazz + " with args " + Lists.newArrayList( args ), new Throwable() );
        msg = clazz.newInstance();
      }
      if ( msg.getUserId() == null )
        msg.setUserId( Component.eucalyptus.name( ) );
      msg.setEffectiveUserId( Component.eucalyptus.name( ) );
      StackTraceElement elem = new Throwable().fillInStackTrace().getStackTrace()[ 1 ];
      EucalyptusMessage reMsg = msg;
      msg.setCorrelationId( String.format( "%s.%s.%s-%s", elem.getClassName(), elem.getMethodName(), elem.getLineNumber(), reMsg.getCorrelationId() ) );
      return msg;
    } catch ( Exception e ) {
      LOG.error( e, e );
      return null;
    }
  }
}
