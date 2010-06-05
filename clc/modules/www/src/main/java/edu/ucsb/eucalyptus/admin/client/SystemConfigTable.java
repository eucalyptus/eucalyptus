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
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the followingdn
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 *
 * Author: Dmitrii Zagorodnov dmitrii@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.admin.client;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;

import edu.ucsb.eucalyptus.admin.client.ClusterInfoTable.GetClusterListCallback;

public class SystemConfigTable extends VerticalPanel {

	private static Label c_status = new Label ();
	private static Label dns_status = new Label ();
	private Grid c_grid = new Grid ();
	private Grid dns_grid = new Grid();
	private static HTML c_hint = new HTML ();
	private static HTML dns_hint = new HTML();
	private SystemConfigWeb SystemConfig = new SystemConfigWeb ();
	private static String sessionId;
	private static TextBox cloudHost_box = new TextBox();
	private static TextBox defaultKernel_box = new TextBox();
	private static TextBox defaultRamdisk_box = new TextBox();
	private static TextBox dnsDomain_box = new TextBox();
	private static TextBox nameserver_box = new TextBox();
	private static TextBox nameserverAddress_box = new TextBox();

	// dmitrii TODO: remove commented out lines once the CSS-based design is confirmed

	public SystemConfigTable(String sessionId)
	{
		this.sessionId = sessionId;
		this.setStyleName("euca-config-component");
		//		this.setSpacing (10);
		this.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
		//		this.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		Label SystemConfigsHeader = new Label( "Cloud configuration:" );
		SystemConfigsHeader.setStyleName ( "euca-section-header" );
		this.add ( SystemConfigsHeader );
		HorizontalPanel c_hpanel = new HorizontalPanel ();
		c_hpanel.add ( this.c_grid );
		c_hpanel.add ( this.c_hint );
		//		c_hint.setWidth ("180");
		this.add ( c_hpanel );
		HorizontalPanel c_hpanel2 = new HorizontalPanel ();
		c_hpanel2.setSpacing (10);
		c_hpanel2.add ( new EucaButton( "Save Configuration", new SaveCallback( this ) ) );
		c_hpanel2.add ( this.c_status );
		this.c_status.setText ("");
		this.c_status.setStyleName ("euca-greeting-pending");
		//		this.c_status.setWidth ("250");
		this.add ( c_hpanel2 );

		Label DNSConfigHeader = new Label( "DNS configuration:" );
		DNSConfigHeader.setStyleName ( "euca-section-header" );
		this.add ( DNSConfigHeader );
		HorizontalPanel dns_hpanel = new HorizontalPanel ();
		dns_hpanel.add ( this.dns_grid );
		dns_hpanel.add ( this.dns_hint );
		//		dns_hint.setWidth ("180");
		this.add ( dns_hpanel );
		HorizontalPanel dns_hpanel2 = new HorizontalPanel ();
		dns_hpanel2.setSpacing (10);
		dns_hpanel2.add ( new EucaButton( "Save Configuration", new SaveCallback( this ) ) );
		dns_hpanel2.add ( this.dns_status );
		this.dns_status.setText ("");
		this.dns_status.setStyleName ("euca-greeting-pending");
		//		this.dns_status.setWidth ("250");
		this.add ( dns_hpanel2 );

		this.rebuildTable ();
		EucalyptusWebBackend.App.getInstance().getSystemConfig(
				this.sessionId, new GetCallback( this ) );
	}

	private void rebuildTable()
	{
		this.c_grid.clear ();
		this.c_grid.resize ( 2, 2 );
		//		this.c_grid.getColumnFormatter().setWidth(0, "190");
		//		this.c_grid.getColumnFormatter().setWidth(1, "260");
		int i = 0;

		// cloud parameters
		this.c_grid.setWidget( i, 0, new Label( "Cloud Host:" ) );
		this.c_grid.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
		cloudHost_box.addChangeListener (new ChangeCallback (this));
		cloudHost_box.setVisibleLength(20);
		cloudHost_box.setText (SystemConfig.getCloudHost()); 
		cloudHost_box.addFocusListener (new FocusHandler (c_hint, "Warning: Changing the Cloud URL will invalidate any existing credentials, and will prevent existing users from accessing the system."));
		this.c_grid.setWidget( i++, 1, cloudHost_box );

		// 2nd row
		this.c_grid.setWidget( i, 0, new Label( "Default kernel:" ) );
		this.c_grid.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
		HorizontalPanel hpanel2 = new HorizontalPanel ();
		hpanel2.setSpacing (0);
		this.c_grid.setWidget( i++, 1, hpanel2 );

		defaultKernel_box.addChangeListener (new ChangeCallback (this));
		defaultKernel_box.setVisibleLength(10);
		defaultKernel_box.setText (SystemConfig.getDefaultKernelId());
		hpanel2.add (defaultKernel_box);

		hpanel2.add ( new HTML ("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; Default ramdisk: &nbsp;"));
		defaultRamdisk_box.addChangeListener (new ChangeCallback (this));
		defaultRamdisk_box.setVisibleLength(10);
		defaultRamdisk_box.setText (SystemConfig.getDefaultRamdiskId());
		hpanel2.add (defaultRamdisk_box);

		// dns params
		this.dns_grid.clear ();
		this.dns_grid.resize ( 2, 2 );
		//		this.dns_grid.getColumnFormatter().setWidth(0, "190");
		//		this.dns_grid.getColumnFormatter().setWidth(1, "260");
		i = 0;

		this.dns_grid.setWidget( i, 0, new Label( "Domain name:" ) );
		this.dns_grid.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
		dnsDomain_box.addChangeListener (new ChangeCallback (this));
		dnsDomain_box.setVisibleLength(20);
		dnsDomain_box.setText (SystemConfig.getDnsDomain());
		this.dns_grid.setWidget( i++, 1, dnsDomain_box );

		this.dns_grid.setWidget( i, 0, new Label( "Nameserver:" ) );
		this.dns_grid.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
		HorizontalPanel dns_hpanel2 = new HorizontalPanel ();
		dns_hpanel2.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
		//dns_hpanel2.add(new Label("Nameserver:"));
		dns_hpanel2.setSpacing (0);
		this.dns_grid.setWidget( i++, 1, dns_hpanel2 );

		nameserver_box.addChangeListener (new ChangeCallback (this));
		nameserver_box.setVisibleLength(20);
		nameserver_box.setText (SystemConfig.getNameserver());
		dns_hpanel2.add (nameserver_box);

		dns_hpanel2.add ( new HTML("&nbsp;&nbsp;&nbsp; IP:  &nbsp;"));
		nameserverAddress_box.addChangeListener (new ChangeCallback (this));
		nameserverAddress_box.setVisibleLength(20);
		nameserverAddress_box.setText (SystemConfig.getNameserverAddress());
		dns_hpanel2.add (nameserverAddress_box);
	}

	public SystemConfigWeb getSystemConfig()
	{
		return SystemConfig;
	}

	public void setSystemConfig( final SystemConfigWeb SystemConfig )
	{
		this.SystemConfig = SystemConfig;
	}

	public void updateStruct ()
	{
		this.SystemConfig.setCloudHost( this.cloudHost_box.getText());
		this.SystemConfig.setDefaultKernelId(this.defaultKernel_box.getText());
		this.SystemConfig.setDefaultRamdiskId(this.defaultRamdisk_box.getText());
		this.SystemConfig.setDnsDomain(this.dnsDomain_box.getText());
		this.SystemConfig.setNameserver(this.nameserver_box.getText());
		this.SystemConfig.setNameserverAddress(this.nameserverAddress_box.getText());
	}

	class ChangeCallback implements ChangeListener, ClickHandler {
		private SystemConfigTable parent;

		ChangeCallback ( final SystemConfigTable parent )
		{
			this.parent = parent;
		}

		public void onChange (Widget sender)
		{
			this.parent.updateStruct ();
			this.parent.c_status.setText ("Unsaved changes");
			this.parent.c_status.setStyleName ("euca-greeting-warning");
			this.parent.dns_status.setText ("Unsaved changes");
			this.parent.dns_status.setStyleName ("euca-greeting-warning");            
		}

		public void onClick (ClickEvent event)
		{
			this.parent.updateStruct ();
			this.parent.c_status.setText ("Unsaved changes");
			this.parent.c_status.setStyleName ("euca-greeting-warning");
			this.parent.dns_status.setText ("Unsaved changes");
			this.parent.dns_status.setStyleName ("euca-greeting-warning");
		}
	}

	class GetCallback implements AsyncCallback {

		private SystemConfigTable parent;

		GetCallback( final SystemConfigTable parent )
		{
			this.parent = parent;
		}

		public void onFailure( final Throwable throwable )
		{
			this.parent.c_status.setText ("Failed to contact server!");
			this.parent.c_status.setStyleName ("euca-greeting-error");
			this.parent.dns_status.setText ("Failed to contact server!");
			this.parent.dns_status.setStyleName ("euca-greeting-error");
		}

		public void onSuccess( final Object o )
		{
			this.parent.c_status.setText ("Loaded configuration from server");
			this.parent.c_status.setStyleName ("euca-greeting-disabled");
			this.parent.dns_status.setText ("Loaded configuration from server");
			this.parent.dns_status.setStyleName ("euca-greeting-disabled");            
			this.parent.SystemConfig = (SystemConfigWeb) o;
			this.parent.rebuildTable();
		}
	}

	class SaveCallback implements AsyncCallback, ClickHandler {

		private SystemConfigTable parent;

		SaveCallback( final SystemConfigTable parent )
		{
			this.parent = parent;
		}

		public void onClick( ClickEvent event )
		{
			this.parent.c_status.setText ("Saving...");
			this.parent.c_status.setStyleName ("euca-greeting-pending");
			this.parent.dns_status.setText ("Saving...");
			this.parent.dns_status.setStyleName ("euca-greeting-pending");            
			EucalyptusWebBackend.App.getInstance().setSystemConfig(
					this.parent.sessionId, this.parent.SystemConfig, this );
		}

		public void onFailure( final Throwable throwable )
		{
			this.parent.c_status.setText ("Failed to save!");
			this.parent.c_status.setStyleName ("euca-greeting-error");
		}

		public void onSuccess( final Object o )
		{
			this.parent.c_status.setText ("Saved configuration to server");
			this.parent.c_status.setStyleName ("euca-greeting-disabled");
			this.parent.dns_status.setText ("Saved configuration to server");
			this.parent.dns_status.setStyleName ("euca-greeting-disabled");            
		}
	}

	class FocusHandler implements FocusListener {
		private HTML parent;
		private String message;

		FocusHandler (final HTML parent, String message)
		{
			this.parent = parent;
			this.message = message;
		}
		public void onLostFocus (Widget sender)
		{
			this.parent.setHTML ("");
			this.parent.setStyleName ("euca-text");
		}
		public void onFocus (Widget sender)
		{
			this.parent.setHTML (message);
			this.parent.setStyleName ("euca-error-hint");
		}
	}
}
