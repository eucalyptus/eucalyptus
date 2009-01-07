package edu.ucsb.eucalyptus.admin.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;

import java.util.*;
import java.lang.*;

public class ClusterInfoTable extends VerticalPanel implements ClickListener {

	private static int maxClusters = 1; // TODO: bump this up once we can do more than 1
	private static Label noClusterLabel = new Label(); 
	private static Label statusLabel = new Label();
	private Grid grid = new Grid ();
	private Button add_button = new Button ( "Add cluster", this );
	private static HTML hint = new HTML ();
	private List<ClusterInfoWeb> clusterList = new ArrayList<ClusterInfoWeb>();
	private static String sessionId;
	private static String warningMessage = "Note: adding a cluster requires synchronization of keys among all nodes, which cannot be done through this interface.  See documentation for details.";
	
	public ClusterInfoTable(String sessionId)
	{
		this.sessionId = sessionId;
		this.setSpacing (15);
		this.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
		this.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		Label clustersHeader = new Label( "Clusters:" );
		clustersHeader.setStyleName ( "euca-section-header" );
		this.add ( clustersHeader );
		this.add ( noClusterLabel );
		this.noClusterLabel.setText ("No clusters specified");
		this.noClusterLabel.setStyleName ("euca-greeting-disabled");
		HorizontalPanel grid_and_hint = new HorizontalPanel ();
		grid_and_hint.add ( this.grid );
		grid_and_hint.add ( this.hint );
		this.hint.setWidth ("180");
		this.add ( grid_and_hint );
		this.grid.setVisible (false);
		HorizontalPanel hpanel = new HorizontalPanel ();
		hpanel.setSpacing (10);
		hpanel.add ( add_button );
		hpanel.add ( new Button( "Save clusters", new SaveCallback( this ) ) );
		hpanel.add ( this.statusLabel );
		this.statusLabel.setWidth ("250");
		this.statusLabel.setText ("");
		this.statusLabel.setStyleName ("euca-greeting-pending");
		this.add ( hpanel );
		rebuildTable();
		EucalyptusWebBackend.App.getInstance().getClusterList( 
			this.sessionId, new GetCallback( this ) );
	}

	public void onClick( final Widget widget ) /* Add cluster button */
	{
		this.clusterList.add (new ClusterInfoWeb ("name", "host", 8774));
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
			int rows = this.clusterList.size() + 1;
			this.noClusterLabel.setVisible (false);
			this.grid.clear ();
			this.grid.resize ( rows, 5 );
			this.grid.setVisible (true);
			this.grid.setStyleName( "euca-table" );
			this.grid.setCellPadding( 6 );
			//this.grid.setWidget( 0, 0, new Label( "Enabled" ) );
			this.grid.setWidget( 0, 1, new Label( "Name" ) );
			this.grid.setWidget( 0, 2, new Label( "Host" ) );
			this.grid.setWidget( 0, 3, new Label( "Port" ) );
			this.grid.getRowFormatter().setStyleName( 0, "euca-table-heading-row" );
			int row = 1;
			for ( ClusterInfoWeb cluster : this.clusterList ) {
				addClusterEntry (row++, cluster);
			}
			if ( row > maxClusters ) {
				this.add_button.setEnabled (false);
			} else {
				this.add_button.setEnabled (true);
			}
		}
	}

	private void addClusterEntry( int row, ClusterInfoWeb clusterInfo )
	{			
		if ( ( row % 2 ) == 1 ) {
			this.grid.getRowFormatter().setStyleName( row, "euca-table-odd-row" );
		} else {
			this.grid.getRowFormatter().setStyleName( row, "euca-table-even-row" );
		}
				
		final CheckBox cb = new CheckBox();
		cb.addClickListener (new ChangeCallback (this, row));
		cb.setChecked( true ); // TODO: get this from server
		//this.grid.setWidget( row, 0, cb );
				
		if (clusterInfo.isCommitted()) {
			this.grid.setWidget ( row, 1, new Label ( clusterInfo.getName() ) );
		} else {
			final TextBox nb = new TextBox();
			nb.addChangeListener (new ChangeCallback (this, row));
			nb.setVisibleLength( 12 );	
			nb.setText( clusterInfo.getName() );
			nb.addFocusListener (new FocusHandler (this.hint, this.warningMessage));		
			this.grid.setWidget ( row, 1, nb );
		}
		
		final TextBox hb = new TextBox();
		hb.addChangeListener (new ChangeCallback (this, row));
		hb.setVisibleLength( 20 );
		hb.setText( clusterInfo.getHost() );
		hb.addFocusListener (new FocusHandler (this.hint, this.warningMessage));
		this.grid.setWidget( row, 2, hb );
		
		final TextBox pb = new TextBox();
		pb.addChangeListener (new ChangeCallback (this, row));
		pb.setVisibleLength( 5 );
		pb.setText( "" + clusterInfo.getPort() );
		pb.addFocusListener (new FocusHandler (this.hint, this.warningMessage));
		this.grid.setWidget( row, 3, pb );
		
		final Button cc = new Button("X", new DeleteCallback( this, row ));
		this.grid.setWidget (row, 4, cc);
	}

	public List<ClusterInfoWeb> getClusterList()
	{
		return clusterList;
	}

	public void setClusterList( final List<ClusterInfoWeb> clusterList )
	{
		this.clusterList = clusterList;
	}

	public void updateRow (int row)
	{
		ClusterInfoWeb cluster = this.clusterList.get (row-1); /* table has a header row */
		cluster.setName (((TextBox)this.grid.getWidget(row, 1)).getText());
		cluster.setHost (((TextBox)this.grid.getWidget(row, 2)).getText());
		cluster.setPort (Integer.parseInt(((TextBox)this.grid.getWidget(row, 3)).getText()));
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
			this.parent.clusterList.remove (this.row-1); /* table has a header row */
			this.parent.rebuildTable();
			this.parent.statusLabel.setText ("Unsaved changes");
			this.parent.statusLabel.setStyleName ("euca-greeting-warning");
		}
	}
	
	class GetCallback implements AsyncCallback {

		private ClusterInfoTable parent;

		GetCallback( final ClusterInfoTable parent )
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
			this.parent.rebuildTable(); /* so the commmitted ones show up */
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
