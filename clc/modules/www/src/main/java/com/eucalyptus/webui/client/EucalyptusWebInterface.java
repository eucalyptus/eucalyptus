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

package com.eucalyptus.webui.client;

import java.util.logging.Level;
import java.util.logging.Logger;
import com.eucalyptus.webui.client.view.GlobalResources;
import com.eucalyptus.webui.client.view.ShellViewImpl;
import com.google.gwt.activity.shared.ActivityManager;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.place.shared.PlaceHistoryHandler;
import com.google.gwt.user.client.ui.RootLayoutPanel;

public class EucalyptusWebInterface implements EntryPoint {

  private static final Logger LOG = Logger.getLogger( EucalyptusWebInterface.class.getName( ) );
  
  private EucalyptusApp app;
  
  @Override
  public void onModuleLoad( ) {
    // Make sure we catch any uncaught exceptions, for debugging purpose.
    GWT.setUncaughtExceptionHandler( new GWT.UncaughtExceptionHandler( ) {
      public void onUncaughtException( Throwable e ) {
        if ( e != null ) {
          LOG.log( Level.SEVERE, e.getMessage( ), e );
        }
      }
    } );
    GWT.<GlobalResources>create( GlobalResources.class ).buttonCss( ).ensureInjected( );
    // Create ClientFactory using deferred binding so we can
    // replace with different implementations in gwt.xml
    ClientFactory clientFactory = GWT.create( ClientFactory.class );
    // Start
    app = new EucalyptusApp( clientFactory );
    app.start( new AppWidget( RootLayoutPanel.get( ) ) );
  }
  
}
