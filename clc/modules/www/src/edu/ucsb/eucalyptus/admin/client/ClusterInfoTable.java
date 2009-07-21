package edu.ucsb.eucalyptus.admin.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;

import java.util.ArrayList;
import java.util.List;

public class ClusterInfoTable extends VerticalPanel implements ClickListener {

    private static int maxClusters = 1; // TODO: bump this up once we can do more than 1
    private static Label noClusterLabel = new Label();
    private static Label statusLabel = new Label();
    private Grid grid = new Grid ();
    private Button add_button = new Button ( "Add cluster", this );
    private static HTML hint = new HTML ();
    private List<ClusterInfoWeb> clusterList = new ArrayList<ClusterInfoWeb>();
    private SystemConfigWeb systemConfig = new SystemConfigWeb ();
    private static String sessionId;
    private static String warningMessage = "Note: adding a cluster requires synchronization of keys among all nodes, which cannot be done through this interface.  See documentation for details.";

    public ClusterInfoTable(String sessionId)
    {
        this.sessionId = sessionId;
        this.setSpacing (2);
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
        HorizontalPanel hpanel = new HorizontalPanel ();
        hpanel.setSpacing (2);
        hpanel.add ( add_button );
        hpanel.add ( new Button( "Save cluster configuration", new SaveCallback( this ) ) );
        hpanel.add ( this.statusLabel );
        this.statusLabel.setWidth ("250");
        this.statusLabel.setText ("");
        this.statusLabel.setStyleName ("euca-greeting-pending");
        this.add ( hpanel );
        rebuildTable();
        EucalyptusWebBackend.App.getInstance().getClusterList(
                this.sessionId, new GetClusterListCallback( this ) );
        EucalyptusWebBackend.App.getInstance().getSystemConfig(
                this.sessionId, new GetSystemConfigCallback( this ) );
    }

    public void onClick( final Widget widget ) // Add cluster button
    {
        this.clusterList.add (new ClusterInfoWeb ("name", "host", 8774, "/var/lib/eucalyptus/volumes", 50, 500));
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
                if ( ( row % 2 ) == 1 ) {
                    this.grid.getRowFormatter().setStyleName( row, "euca-table-even-row" );
                } else {
                    this.grid.getRowFormatter().setStyleName( row, "euca-table-odd-row" );
                }
                this.grid.setWidget (row, 0, addClusterEntry (row++, cluster));
            }

            if ( row >= maxClusters ) {
                this.add_button.setEnabled (false);
            } else {
                this.add_button.setEnabled (true);
            }
        }
    }

    private Grid addClusterEntry ( int row, ClusterInfoWeb clusterInfo )
    {
        Grid g = new Grid (10, 2);
        g.setStyleName( "euca-table" );
        g.setCellPadding( 4 );

        // row 1
        g.setWidget( 0, 0, new Label( "Name: " ) );
        g.getCellFormatter().setHorizontalAlignment(0, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        final HorizontalPanel namePanel = new HorizontalPanel ();
        namePanel.setSpacing (6);

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
        namePanel.add (new Button ("Delete Cluster", new DeleteCallback( this, row )));
        g.setWidget ( 0, 1, namePanel);

        // row 2
        g.setWidget( 1, 0, new Label( "Host: " ) );
        g.getCellFormatter().setHorizontalAlignment(1, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        final TextBox hb = new TextBox();
        hb.addChangeListener (new ChangeCallback (this, row));
        hb.setVisibleLength( 20 );
        hb.setText( clusterInfo.getHost() );
        hb.addFocusListener (new FocusHandler (this.hint, this.warningMessage));
        g.setWidget ( 1, 1, hb );

        // row 3
        g.setWidget( 2, 0, new Label( "Port: " ) );
        g.getCellFormatter().setHorizontalAlignment(2, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        final TextBox pb = new TextBox();
        pb.addChangeListener (new ChangeCallback (this, row));
        pb.setVisibleLength( 5 );
        pb.setText( "" + clusterInfo.getPort() );
        pb.addFocusListener (new FocusHandler (this.hint, this.warningMessage));
        g.setWidget( 2, 1, pb );

        g.setWidget( 3, 0, new Label( "Storage Interface: " ) );
        g.getCellFormatter().setHorizontalAlignment(3, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        final TextBox sib = new TextBox();
        sib.addChangeListener (new ChangeCallback (this, row));
        sib.setVisibleLength( 10 );
        sib.setText( "" + systemConfig.getStorageInterface());
        sib.addFocusListener (new FocusHandler (this.hint, this.warningMessage));
        g.setWidget( 3, 1, sib );

        // row 4
        g.setWidget( 4, 0, new Label( "Volumes Path:" ) );
        g.getCellFormatter().setHorizontalAlignment(4, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        final TextBox volumesPathBox = new TextBox();
        volumesPathBox.addChangeListener (new ChangeCallback (this, row));
        volumesPathBox.setVisibleLength( 40 );
        volumesPathBox.setText( systemConfig.getStorageVolumesPath() );
        volumesPathBox.addFocusListener (new FocusHandler (this.hint, this.warningMessage));
        g.setWidget( 4, 1, volumesPathBox );

        // row 5
        g.setWidget( 5, 0, new Label( "Max volume size:" ) );
        g.getCellFormatter().setHorizontalAlignment(5, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        final TextBox volumeMaxBox = new TextBox();
        volumeMaxBox.addChangeListener (new ChangeCallback (this, row));
        volumeMaxBox.setVisibleLength( 10 );
        volumeMaxBox.setText( "" + systemConfig.getStorageMaxVolumeSizeInGB());
        volumeMaxBox.addFocusListener (new FocusHandler (this.hint, this.warningMessage));
        final HorizontalPanel volumesMaxPanel = new HorizontalPanel ();
        volumesMaxPanel.add (volumeMaxBox);
        volumesMaxPanel.add (new HTML ("&nbsp; GB"));
        g.setWidget( 5, 1, volumesMaxPanel );

        // row 6
        g.setWidget( 6, 0, new Label( "Disk space reserved for volumes:" ) );
        g.getCellFormatter().setHorizontalAlignment(6, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        final TextBox volumesTotalBox = new TextBox();
        volumesTotalBox.addChangeListener (new ChangeCallback (this, row));
        volumesTotalBox.setVisibleLength( 10 );
        volumesTotalBox.setText( "" + systemConfig.getStorageVolumesTotalInGB());
        volumesTotalBox.addFocusListener (new FocusHandler (this.hint, this.warningMessage));
        final HorizontalPanel volumesTotalPanel = new HorizontalPanel ();
        volumesTotalPanel.add (volumesTotalBox);
        volumesTotalPanel.add (new HTML ("&nbsp; GB"));
        g.setWidget( 6, 1, volumesTotalPanel );


        // row 9
        g.setWidget( 9, 0, new Label( "Maximum of" ) );
        g.getCellFormatter().setHorizontalAlignment(9, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        final TextBox publicAddressesBox = new TextBox();
        publicAddressesBox.addChangeListener (new ChangeCallback (this, row));
        publicAddressesBox.setVisibleLength( 10 );
        publicAddressesBox.setText( "" + systemConfig.getMaxUserPublicAddresses());
        final HorizontalPanel publicAddressesPanel = new HorizontalPanel ();
        publicAddressesPanel.add (publicAddressesBox);
        publicAddressesPanel.add (new HTML ("&nbsp; public IP addresses per user"));
        g.setWidget( 9, 1, publicAddressesPanel );

        // row 8 (yes, swapped with row 7)
        g.setWidget( 8, 0, new Label( "Total of" ) );
        g.getCellFormatter().setHorizontalAlignment(8, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        final TextBox reservedAddressesBox = new TextBox();
        reservedAddressesBox.addChangeListener (new ChangeCallback (this, row));
        reservedAddressesBox.setVisibleLength( 10 );
        reservedAddressesBox.setText( "" + systemConfig.getSystemReservedPublicAddresses());
        final HorizontalPanel reservedAddressesPanel = new HorizontalPanel ();
        reservedAddressesPanel.add (reservedAddressesBox);
        reservedAddressesPanel.add (new HTML ("&nbsp; public IP addresses reserved for instances"));
        reservedAddressesBox.setText(""+systemConfig.getSystemReservedPublicAddresses());
        g.setWidget( 8, 1, reservedAddressesPanel );

        // row 7 (yes, swapped with row 8)
        final HorizontalPanel dynamicAddressesingPanel = new HorizontalPanel ();
        final CheckBox dynamicAddressesCheckbox = new CheckBox ("Enable dynamic public IP address assignment");
        dynamicAddressesingPanel.add( dynamicAddressesCheckbox );
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
        g.setWidget( 7, 1, dynamicAddressesingPanel );


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

    public void updateRow (int row)
    {
        ClusterInfoWeb cluster = this.clusterList.get (row);
        Grid g = (Grid)this.grid.getWidget(row, 0);
        HorizontalPanel p = (HorizontalPanel)g.getWidget(0, 1);
        if (p.getWidget(0) instanceof TextBox) {
            cluster.setName (((TextBox)p.getWidget(0)).getText());
        } else {
            cluster.setName (((Label)p.getWidget(0)).getText());
        }
        cluster.setHost (((TextBox)g.getWidget(1, 1)).getText());
        cluster.setPort (Integer.parseInt(((TextBox)g.getWidget(2, 1)).getText()));
        systemConfig.setStorageInterface(((TextBox)g.getWidget(3, 1)).getText());
        systemConfig.setStorageVolumesPath (((TextBox)g.getWidget(4, 1)).getText());
        p = (HorizontalPanel)g.getWidget(5, 1);
        systemConfig.setStorageMaxVolumeSizeInGB (Integer.parseInt(((TextBox)p.getWidget(0)).getText()));
        p = (HorizontalPanel)g.getWidget(6, 1);
        systemConfig.setStorageVolumesTotalInGB (Integer.parseInt(((TextBox)p.getWidget(0)).getText()));
        p = (HorizontalPanel)g.getWidget(9, 1);
        systemConfig.setMaxUserPublicAddresses(Integer.parseInt(((TextBox)p.getWidget(0)).getText()));
        p = (HorizontalPanel)g.getWidget(8, 1);
        systemConfig.setSystemReservedPublicAddresses(Integer.parseInt(((TextBox)p.getWidget(0)).getText()));
//    systemConfig.setDoDynamicPublicAddresses( !((TextBox)p.getWidget(0)).isEnabled() ? true : false );
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
