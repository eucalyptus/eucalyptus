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
 *    or without modification, are permitted provided that the following
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
package edu.ucsb.eucalyptus.admin.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;

import java.util.ArrayList;
import java.util.List;

// dmitrii TODO: remove commented out lines once the CSS-based design is confirmed

public class ClusterInfoTable extends VerticalPanel implements ClickListener {

	private static String warningMessage = "Note: adding a cluster requires synchronization of keys among all nodes, which cannot be done through this interface.  See documentation for details.";
	private static int maxClusters = 4096; //arbitrary
	private Label noClusterLabel = new Label();
	private Label statusLabel = new Label();
	private Grid grid = new Grid ();
	private Button add_button = new Button ( "Register cluster", this );
	private HTML hint = new HTML ();
	private List<ClusterInfoWeb> clusterList = new ArrayList<ClusterInfoWeb>();
	private List<StorageInfoWeb> storageList = new ArrayList<StorageInfoWeb>();
	private SystemConfigWeb systemConfig = new SystemConfigWeb ();
	private String sessionId;
	private int numStorageParams;

	public ClusterInfoTable(String sessionId)
	{
		this.sessionId = sessionId;
		this.setStyleName("euca-config-component");
		this.setSpacing (2);
		this.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
		//		this.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		Label clustersHeader = new Label( "Clusters:" );
		clustersHeader.setStyleName ( "euca-section-header" );
		this.add ( clustersHeader );
		this.noClusterLabel.setText ("No clusters registered");
		this.noClusterLabel.setStyleName ("euca-greeting-disabled");
		HorizontalPanel grid_and_hint = new HorizontalPanel ();
		grid_and_hint.add ( this.grid );
		grid_and_hint.add ( this.hint );
		this.hint.setWidth ("100");
		this.add ( grid_and_hint );
		HorizontalPanel hpanel = new HorizontalPanel ();
		hpanel.setSpacing (2);
		hpanel.add ( add_button );
		hpanel.add ( new Button( "Save cluster configuration", new SaveCallback( this ) ) );
		hpanel.add ( this.statusLabel );
		//		this.statusLabel.setWidth ("250");
		this.statusLabel.setText ("");
		this.statusLabel.setStyleName ("euca-greeting-pending");
		this.add ( hpanel );
		rebuildTable();
		EucalyptusWebBackend.App.getInstance().getClusterList(
				this.sessionId, new GetClusterListCallback( this ) );
		EucalyptusWebBackend.App.getInstance().getSystemConfig(
				this.sessionId, new GetSystemConfigCallback( this ) );
		EucalyptusWebBackend.App.getInstance().getStorageList(
				this.sessionId, new GetStorageListCallback(this));
	}

	public void onClick( final Widget widget ) // Add cluster button
	{
		this.clusterList.add (new ClusterInfoWeb ("cluster-name", "cc-host", 8774, 10, 4096));
		//these values are just defaults
		this.storageList.add (new StorageInfoWeb("sc-name", "sc-host", 8773, new ArrayList<String>()));
		this.rebuildTable();
		this.statusLabel.setText ("Unsaved changes");
		this.statusLabel.setStyleName ("euca-greeting-warning");
	}

	private void rebuildTable()
	{
		if (this.clusterList.isEmpty()) {
			this.grid.setVisible (false);
			this.noClusterLabel.setVisible (true);
			this.add_button.setEnabled (true);

		} else {
			this.noClusterLabel.setVisible (false);
			this.grid.clear ();
			this.grid.resize ( this.clusterList.size(), 1 );
			this.grid.setVisible (true);
			this.grid.setStyleName( "euca-table" );
			this.grid.setCellPadding( 2 );

			int row = 0;
			for ( ClusterInfoWeb cluster : this.clusterList ) {
				/*// big yellow block looks kinda weird
				if ( ( row % 2 ) == 1 ) {
					this.grid.getRowFormatter().setStyleName( row, "euca-table-even-row" );
				} else {
					this.grid.getRowFormatter().setStyleName( row, "euca-table-odd-row" );
				}*/
				StorageInfoWeb storageInfo = this.storageList.get(row);
				this.grid.setWidget (row, 0, addClusterEntry (row++, cluster, storageInfo));
			}

			if ( row >= maxClusters ) {
				this.add_button.setEnabled (false);
			} else {
				this.add_button.setEnabled (true);
			}
		}
	}

	private Grid addClusterEntry ( int row, ClusterInfoWeb clusterInfo, final StorageInfoWeb storageInfo)
	{
		final ArrayList<String> storageParams = storageInfo.getStorageParams();
		numStorageParams = storageParams.size()/4;
		Grid g = new Grid (8 +  numStorageParams, 2);
		g.setStyleName( "euca-table" );
		if (row > 0) {
			g.setStyleName( "euca-nonfirst-cluster-entry" );
		}
		g.setCellPadding( 4 );

		int i = 0; // row 1
		g.setWidget( i, 0, new HTML( "<b>Name:</b>" ) );
		g.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
		final HorizontalPanel namePanel = new HorizontalPanel ();
		namePanel.setSpacing (0);

		if (clusterInfo.isCommitted()) {
			namePanel.add (new Label ( clusterInfo.getName() ));
		} else {
			final TextBox nb = new TextBox();
			nb.addChangeListener (new ChangeCallback (this, row));
			nb.setVisibleLength( 12 );
			nb.setText( clusterInfo.getName() );
			nb.addFocusListener (new FocusHandler (this.hint, this.warningMessage));
			namePanel.add ( nb );
		}
		namePanel.add (new Button ("Deregister Cluster", new DeleteCallback( this, row )));
		g.setWidget ( i, 1, namePanel);

		i++; // next row
		g.setWidget( i, 1, new Label( "Cluster Controller" ));

		i++; // next row
		g.setWidget( i, 0, new Label( "Host:" ) );
		g.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
		final TextBox hb = new TextBox();
		hb.addChangeListener (new ChangeCallback (this, row));
		hb.setVisibleLength( 20 );
		hb.setText( clusterInfo.getHost() );
		hb.addFocusListener (new FocusHandler (this.hint, this.warningMessage));
		g.setWidget ( i, 1, hb );

		i++; // next row
/*		g.setWidget( i, 0, new Label( "Port:" ) );
		g.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
		final TextBox pb = new TextBox();
		pb.addChangeListener (new ChangeCallback (this, row));
		pb.setVisibleLength( 5 );
		pb.setText( "" + clusterInfo.getPort() );
		pb.addFocusListener (new FocusHandler (this.hint, this.warningMessage));
		g.setWidget( i, 1, pb );*/

		final TextBox reservedAddressesBox = new TextBox(); // declare here, set up after the checkbox later

		final CheckBox dynamicAddressesCheckbox = new CheckBox ();
		g.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
		g.setWidget (i, 0, dynamicAddressesCheckbox );
		if (systemConfig.isDoDynamicPublicAddresses()) {
			dynamicAddressesCheckbox.setChecked(true);
			reservedAddressesBox.setEnabled(false);
		} else {
			dynamicAddressesCheckbox.setChecked(false);
			reservedAddressesBox.setEnabled(true);
		}
		dynamicAddressesCheckbox.addClickListener (new ClickListener() {
			public void onClick( Widget sender )
			{
				if (((CheckBox)sender).isChecked()) {
					reservedAddressesBox.setEnabled(false);
					systemConfig.setDoDynamicPublicAddresses( true );
				} else {
					reservedAddressesBox.setEnabled(true);
					systemConfig.setDoDynamicPublicAddresses( false );
				}
			}
		});
		g.setWidget( i, 1, new Label ("Dynamic public IP address assignment") );

		i++; // next row
		g.setWidget( i, 0, new Label( "Reserve for assignment" ) );
		g.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
		reservedAddressesBox.addChangeListener (new ChangeCallback (this, row));
		reservedAddressesBox.setVisibleLength( 5 );
		reservedAddressesBox.setText( "" + systemConfig.getSystemReservedPublicAddresses());
		final HorizontalPanel reservedAddressesPanel = new HorizontalPanel ();
		reservedAddressesPanel.setSpacing(4);
		reservedAddressesPanel.add (reservedAddressesBox);
		reservedAddressesPanel.add (new HTML ("public IP addresses"));
		reservedAddressesBox.setText(""+systemConfig.getSystemReservedPublicAddresses());
		g.setWidget( i, 1, reservedAddressesPanel );

		i++; // next row
		g.setWidget( i, 0, new Label( "Maximum of" ) );
		g.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
		final TextBox publicAddressesBox = new TextBox();
		publicAddressesBox.addChangeListener (new ChangeCallback (this, row));
		publicAddressesBox.setVisibleLength( 5 );
		publicAddressesBox.setText( "" + systemConfig.getMaxUserPublicAddresses());
		final HorizontalPanel publicAddressesPanel = new HorizontalPanel ();
		publicAddressesPanel.setSpacing(4);
		publicAddressesPanel.add (publicAddressesBox);
		publicAddressesPanel.add (new HTML ("public IP addresses per user"));
		g.setWidget( i, 1, publicAddressesPanel );

		i++;
		g.setWidget( i, 0, new Label( "Use VLAN tags" ) );
		g.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
		final TextBox minVlanBox = new TextBox();
		minVlanBox.addChangeListener (new ChangeCallback (this, row));
		minVlanBox.setVisibleLength( 4 );
		minVlanBox.setText(String.valueOf(clusterInfo.getMinVlans()));
		final TextBox maxVlanBox = new TextBox();
		maxVlanBox.addChangeListener (new ChangeCallback (this, row));
		maxVlanBox.setVisibleLength( 4 );
		maxVlanBox.setText(String.valueOf(clusterInfo.getMaxVlans()));
		final HorizontalPanel vlanPanel = new HorizontalPanel ();
		vlanPanel.setSpacing(4);
		vlanPanel.add (minVlanBox);
		vlanPanel.add (new HTML ("through"));
		vlanPanel.add (maxVlanBox);
		g.setWidget( i, 1, vlanPanel );


		i++; // next row
		g.setWidget( i, 1, new Label( "Storage Controller" ));

		for(int paramidx = 0; paramidx < numStorageParams; ++paramidx) {
			i++; // next row
			if ("KEYVALUE".equals(storageParams.get(4 * paramidx))) {
				g.setWidget( i, 0, new Label(storageParams.get(4*paramidx + 1) + ": ") );
				g.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
				final TextBox propTextBox = new TextBox();
				propTextBox.addChangeListener (new ChangeCallback (this, row));
				propTextBox.setVisibleLength( 30 );
				propTextBox.setText(storageParams.get(4*paramidx + 2));
				propTextBox.addFocusListener (new FocusHandler (this.hint, this.warningMessage));
				g.setWidget( i, 1, propTextBox );
			} else if ("PASSWORD".equals(storageParams.get(4 * paramidx))) {
				g.setWidget( i, 0, new Label(storageParams.get(4*paramidx + 1) + ": ") );
				g.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
				final TextBox propTextBox = new PasswordTextBox();
				propTextBox.addChangeListener (new ChangeCallback (this, row));
				propTextBox.setVisibleLength( 30 );
				propTextBox.setText(storageParams.get(4*paramidx + 2));
				propTextBox.addFocusListener (new FocusHandler (this.hint, this.warningMessage));
				g.setWidget( i, 1, propTextBox );
			}	else if("BOOLEAN".equals(storageParams.get(4 * paramidx))) {

				final int index = paramidx;
				final CheckBox propCheckbox = new CheckBox ();
				g.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
				g.setWidget( i, 0, propCheckbox );
				if (Boolean.parseBoolean(storageParams.get(4*index + 2))) {
					propCheckbox.setChecked(true);
				} else {
					propCheckbox.setChecked(false);
				}
				propCheckbox.addClickListener (new ClickListener() {
					public void onClick( Widget sender )
					{
						if (((CheckBox)sender).isChecked()) {
							storageParams.set(4 * index + 2,  String.valueOf(true) );
						} else {
							storageParams.set(4 * index + 2,  String.valueOf(false) );
						}
					}
				});
				g.setWidget( i, 1, new Label (storageParams.get(paramidx * 4 + 1)) );
			}
		}

		return g;
	}

	public List<ClusterInfoWeb> getClusterList()
	{
		return clusterList;
	}

	public void setClusterList( final List<ClusterInfoWeb> clusterList )
	{
		this.clusterList = clusterList;
	}

	public List<StorageInfoWeb> getStorageList()
	{
		return storageList;
	}

	public void setStorageList( final List<StorageInfoWeb> storageList )
	{
		this.storageList = storageList;
	}

	public void updateRow (int row)
	{
		ClusterInfoWeb cluster = this.clusterList.get (row);
		StorageInfoWeb storage = this.storageList.get(row);
		Grid g = (Grid)this.grid.getWidget(row, 0);
		HorizontalPanel p = (HorizontalPanel)g.getWidget(0, 1);
		if (p.getWidget(0) instanceof TextBox) {
			cluster.setName (((TextBox)p.getWidget(0)).getText());
			storage.setName (((TextBox)p.getWidget(0)).getText());
		} else {
			cluster.setName (((Label)p.getWidget(0)).getText());
			storage.setName (((Label)p.getWidget(0)).getText());
		}

		// CC section
		cluster.setHost (((TextBox)g.getWidget(2, 1)).getText());
		//cluster.setPort (Integer.parseInt(((TextBox)g.getWidget(3, 1)).getText()));
		p = (HorizontalPanel)g.getWidget(4, 1);
		systemConfig.setSystemReservedPublicAddresses(Integer.parseInt(((TextBox)p.getWidget(0)).getText()));
		p = (HorizontalPanel)g.getWidget(5, 1);
		systemConfig.setMaxUserPublicAddresses(Integer.parseInt(((TextBox)p.getWidget(0)).getText()));
		p = (HorizontalPanel)g.getWidget(6, 1);
		cluster.setMinVlans(Integer.parseInt(((TextBox)p.getWidget(0)).getText()));
		cluster.setMaxVlans(Integer.parseInt(((TextBox)p.getWidget(2)).getText()));
		//7 is SC label
		// SC section
		int widgetStartIndex = 8;
		ArrayList<String> storageParams = storage.getStorageParams();
		for(int i = 0; i < numStorageParams; ++i) {
			if(storageParams.get(4*i).startsWith("KEYVALUE"))
				storageParams.set(4*i + 2, ((TextBox)g.getWidget(widgetStartIndex + i, 1)).getText());
		}
	}

	public void MarkCommitted ()
	{
		for ( ClusterInfoWeb cluster : this.clusterList ) {
			cluster.setCommitted ();
		}
	}

	class ChangeCallback implements ChangeListener, ClickListener {
		private ClusterInfoTable parent;
		private int row;

		ChangeCallback ( final ClusterInfoTable parent, final int row )
		{
			this.parent = parent;
			this.row = row;
		}

		public void onChange (Widget sender)
		{
			this.parent.updateRow (this.row);
			this.parent.statusLabel.setText ("Unsaved changes");
			this.parent.statusLabel.setStyleName ("euca-greeting-warning");
		}

		public void onClick (Widget sender)
		{
			this.parent.updateRow (this.row);
			this.parent.statusLabel.setText ("Unsaved changes");
			this.parent.statusLabel.setStyleName ("euca-greeting-warning");
		}
	}

	class DeleteCallback implements ClickListener {

		private ClusterInfoTable parent;
		private int row;

		DeleteCallback( final ClusterInfoTable parent, final int row )
		{
			this.parent = parent;
			this.row = row;
		}

		public void onClick( final Widget widget )
		{
			this.parent.clusterList.remove (this.row);
			this.parent.storageList.remove(this.row);
			this.parent.rebuildTable();
			this.parent.statusLabel.setText ("Unsaved changes");
			this.parent.statusLabel.setStyleName ("euca-greeting-warning");
		}
	}

	class GetClusterListCallback implements AsyncCallback {

		private ClusterInfoTable parent;

		GetClusterListCallback( final ClusterInfoTable parent )
		{
			this.parent = parent;
		}

		public void onFailure( final Throwable throwable )
		{
			this.parent.statusLabel.setText ("Failed to contact server!");
			this.parent.statusLabel.setStyleName ("euca-greeting-error");
		}

		public void onSuccess( final Object o )
		{
			List<ClusterInfoWeb> newClusterList = (List<ClusterInfoWeb>) o;
			this.parent.statusLabel.setText ("Clusters up to date");
			this.parent.statusLabel.setStyleName ("euca-greeting-disabled");
			this.parent.clusterList = newClusterList;
			this.parent.MarkCommitted();
			this.parent.rebuildTable();
		}
	}

	class GetStorageListCallback implements AsyncCallback {

		private ClusterInfoTable parent;

		GetStorageListCallback( final ClusterInfoTable parent )
		{
			this.parent = parent;
		}

		public void onFailure( final Throwable throwable )
		{
			this.parent.statusLabel.setText ("Failed to contact server!");
			this.parent.statusLabel.setStyleName ("euca-greeting-error");
		}

		public void onSuccess( final Object o )
		{
			List<StorageInfoWeb> newStorageList = (List<StorageInfoWeb>) o;
			this.parent.statusLabel.setText ("Clusters up to date");
			this.parent.statusLabel.setStyleName ("euca-greeting-disabled");
			this.parent.storageList = newStorageList;
			this.parent.MarkCommitted();
			this.parent.rebuildTable();
		}
	}

	class GetSystemConfigCallback implements AsyncCallback {

		private ClusterInfoTable parent;

		GetSystemConfigCallback ( final ClusterInfoTable parent )
		{
			this.parent = parent;
		}

		public void onFailure( final Throwable throwable )
		{
			this.parent.statusLabel.setText ("Failed to contact server!");
			this.parent.statusLabel.setStyleName ("euca-greeting-error");
		}

		public void onSuccess( final Object o )
		{
			this.parent.systemConfig = (SystemConfigWeb) o;
			this.parent.rebuildTable();
		}
	}

	class SaveCallback implements AsyncCallback, ClickListener {

		private ClusterInfoTable parent;

		SaveCallback( final ClusterInfoTable parent )
		{
			this.parent = parent;
		}

		public void onClick( final Widget widget )
		{
			this.parent.statusLabel.setText ("Saving...");
			this.parent.statusLabel.setStyleName ("euca-greeting-pending");
			EucalyptusWebBackend.App.getInstance().setClusterList(
					this.parent.sessionId, this.parent.clusterList, this );
			EucalyptusWebBackend.App.getInstance().setSystemConfig(
					this.parent.sessionId, this.parent.systemConfig, this );
			EucalyptusWebBackend.App.getInstance().setStorageList(
					this.parent.sessionId, this.parent.storageList, this );
		}

		public void onFailure( final Throwable throwable )
		{
			this.parent.statusLabel.setText ("Failed to save!");
			this.parent.statusLabel.setStyleName ("euca-greeting-error");
		}

		public void onSuccess( final Object o )
		{
			this.parent.statusLabel.setText ("Saved clusters to server");
			this.parent.statusLabel.setStyleName ("euca-greeting-disabled");
			this.parent.MarkCommitted ();
			this.parent.rebuildTable(); // so the commmitted ones show up
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
