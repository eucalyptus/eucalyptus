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

package com.eucalyptus.cluster.callback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.apache.log4j.Logger;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;

import com.eucalyptus.event.EventFailedException;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.records.Logs;

import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.async.BroadcastCallback;

import edu.ucsb.eucalyptus.msgs.ModifyNode;
import edu.ucsb.eucalyptus.msgs.ModifyNodeResponse;


public class ModifyNodeCallback extends
    BroadcastCallback<ModifyNode, ModifyNodeResponse> {

  private static final Logger LOG = Logger.getLogger( ModifyNodeCallback.class );
  private final String stateName;
  private final String nodeName;

  private final ListenerRegistry listener = ListenerRegistry.getInstance();

  public ModifyNodeCallback( final String stateName, final String nodeName) {
    this.stateName = stateName;
    this.nodeName = nodeName;
    
    final ModifyNode msg =
        new ModifyNode( this.stateName, this.nodeName);

    try {
      msg.setUser( Accounts.lookupSystemAdmin() );
    } catch ( AuthException e ) {
      LOG.error( "Unable to find the system user", e );
    }

    this.setRequest( msg );
  }

  @Override
  public void initialize( final ModifyNode msg ) {
  }

  @Override
  public BroadcastCallback<ModifyNode, ModifyNodeResponse> newInstance() {
    return new ModifyNodeCallback( stateName,  nodeName);
  }

  @Override
  public void fireException( Throwable e ) {
    LOG.debug( "Request failed: "
        + LogUtil.subheader( this.getRequest().toString(
        "eucalyptus_ucsb_edu" ) ) );
    Logs.extreme().error( e, e );
  }

  @Override
  public void fire( final ModifyNodeResponse msg ) {
    try {
     
        LOG.debug(msg);
	
    } catch ( Exception ex ) {
      LOG.debug( "Unable to fire describe sensors call back", ex );
    }
  }

}
