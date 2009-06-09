package edu.ucsb.eucalyptus.admin.client;

import com.google.gwt.user.client.ui.*;

import java.util.List;
import java.util.ArrayList;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Created by IntelliJ IDEA.
 * User: dmitrii
 * Date: Jun 8, 2009
 * Time: 3:24:29 PM
 * To change this template use File | Settings | File Templates.
 */
public class DownloadsTable extends VerticalPanel {
    private static Label statusLabel = new Label();
    private Grid grid = new Grid ();
/*    private List<DownloadsWeb> DownloadsList = new ArrayList<DownloadsWeb>(); */
    private static String sessionId;

    public DownloadsTable(String sessionId)
    {
        this.sessionId = sessionId;
        this.setSpacing (10);
        this.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        this.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        Label DownloadsHeader = new Label( "Prepackaged Images:" );
        DownloadsHeader.setStyleName ( "euca-section-header" );
        this.add ( DownloadsHeader );
        this.add ( this.grid );
        HorizontalPanel hpanel = new HorizontalPanel ();
        hpanel.setSpacing (5);
        hpanel.add ( this.statusLabel );
        this.statusLabel.setWidth ("250");
        this.statusLabel.setText ("Contacting the server...");
        this.statusLabel.setStyleName ("euca-greeting-pending");
        this.add ( hpanel );
 /*
        EucalyptusWebBackend.App.getInstance().getDownloads(
            this.sessionId, new GetCallback( this ) );

    }

    private void rebuildTable()
	{
		int rows = this.DownloadsList.size() + 1;
		this.grid.clear ();
		this.grid.resize ( rows, 5 );
		this.grid.setVisible (true);
		this.grid.setStyleName( "euca-table" );
		this.grid.setCellPadding( 6 );
		//this.grid.setWidget( 0, 0, new Label( "Enabled" ) );
		this.grid.setWidget( 0, 1, new Label( "Name" ) );
		this.grid.setWidget( 0, 2, new Label( "CPUs" ) );
		this.grid.setWidget( 0, 3, new Label( "Memory (MB)" ) );
		this.grid.setWidget( 0, 4, new Label( "Disk (GB)" ) );
		this.grid.getRowFormatter().setStyleName( 0, "euca-table-heading-row" );
		int row = 1;
		for ( DownloadsWeb download : this.DownloadsList ) {
			addDownloadsEntry (row++, download);
		}
	}

    	private void addDownloadsEntry( int row, DownloadsWeb Downloads )
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

		final Label name_b = new Label ();
		name_b.setText( Downloads.getName() );
		this.grid.setWidget( row, 1, name_b );

		final TextBox cpu_b = new TextBox();
		cpu_b.addChangeListener (new ChangeCallback (this, row));
		cpu_b.setVisibleLength( 2 );
		cpu_b.setText( "" + Downloads.getCpu() );
		this.grid.setWidget( row, 2, cpu_b );
		this.grid.getCellFormatter().setHorizontalAlignment(row, 2, HasHorizontalAlignment.ALIGN_CENTER);

		final TextBox mem_b = new TextBox();
		mem_b.addChangeListener (new ChangeCallback (this, row));
		mem_b.setVisibleLength( 4 );
		mem_b.setText( "" + Downloads.getMemory() );
		this.grid.setWidget( row, 3, mem_b );
		this.grid.getCellFormatter().setHorizontalAlignment(row, 3, HasHorizontalAlignment.ALIGN_CENTER);


		final TextBox disk_b = new TextBox();
		disk_b.addChangeListener (new ChangeCallback (this, row));
		disk_b.setVisibleLength( 4 );
		disk_b.setText( "" + Downloads.getDisk() );
		this.grid.setWidget( row, 4, disk_b );
		this.grid.getCellFormatter().setHorizontalAlignment(row, 4, HasHorizontalAlignment.ALIGN_CENTER);

	}

	public List<DownloadsWeb> getDownloadsList()
	{
		return DownloadsList;
	}

	public void setDownloadsList( final List<DownloadsWeb> DownloadsList )
	{
		this.DownloadsList = DownloadsList;
	}

	public void updateRow (int row)
	{
		DownloadsWeb Downloads = this.DownloadsList.get (row-1); // table has a header row
		Downloads.setCpu    (Integer.parseInt(((TextBox)this.grid.getWidget(row, 2)).getText()));
		Downloads.setMemory (Integer.parseInt(((TextBox)this.grid.getWidget(row, 3)).getText()));
		Downloads.setDisk   (Integer.parseInt(((TextBox)this.grid.getWidget(row, 4)).getText()));
	}
    
	class GetCallback implements AsyncCallback {

		private DownloadsTable parent;

		GetCallback( final DownloadsTable parent )
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
			List<DownloadsWeb> newDownloadsList = (List<DownloadsWeb>) o;
			this.parent.DownloadsList = newDownloadsList;
			this.parent.rebuildTable();
		}
		*/
	}
}
