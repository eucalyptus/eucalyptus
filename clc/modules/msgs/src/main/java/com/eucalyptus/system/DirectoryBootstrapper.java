/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.system;

import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.bootstrap.Bootstrap.Stage;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.EventRecord;

@Provides(Empyrean.class)
@RunDuring(Bootstrap.Stage.UnprivilegedConfiguration)
public class DirectoryBootstrapper extends Bootstrapper {
  private static Logger LOG = Logger.getLogger( DirectoryBootstrapper.class );
  public DirectoryBootstrapper( ) {}
  @Override
  public boolean load( ) throws Exception {
    for( BaseDirectory b : BaseDirectory.values( ) ) {
      EventRecord.here( DirectoryBootstrapper.class, EventType.SYSTEM_DIR_CHECK, b.name(), b.toString( ) ).info( );
      b.check( );
    }
    for( SubDirectory s : SubDirectory.values( ) ) {
      EventRecord.here( DirectoryBootstrapper.class, EventType.SYSTEM_DIR_CHECK, s.name(), s.toString( ) ).info( );
      s.check( );
    }
    return true;
  }
  
  @Override
  public boolean start( ) throws Exception {
    return true;
  }
  
  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#enable()
   */
  @Override
  public boolean enable( ) throws Exception {
    return true;
  }

  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#stop()
   */
  @Override
  public boolean stop( ) throws Exception {
    return true;
  }

  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#destroy()
   */
  @Override
  public void destroy( ) throws Exception {}

  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#disable()
   */
  @Override
  public boolean disable( ) throws Exception {
    return true;
  }

  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#check()
   */
  @Override
  public boolean check( ) throws Exception {
    return true;
  }
}
