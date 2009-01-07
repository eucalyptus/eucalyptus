package edu.ucsb.eucalyptus.admin.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;

import java.util.*;
import java.lang.*;

public class SystemConfigTable extends VerticalPanel {

	private static Label c_status = new Label ();
	private static Label w_status = new Label ();
	private Grid c_grid = new Grid ();
	private Grid w_grid = new Grid ();
	private static HTML c_hint = new HTML ();
	private static HTML w_hint = new HTML ();
	private SystemConfigWeb SystemConfig = new SystemConfigWeb ();
	private static String sessionId;
	private static TextBox walrusURL_box = new TextBox();
	private static TextBox walrusPath_box = new TextBox();
	private static TextBox maxBuckets_box = new TextBox();
	private static TextBox maxBucketSize_box = new TextBox();
	private static TextBox maxCacheSize_box = new TextBox();
	private static TextBox defaultKernel_box = new TextBox();
	private static TextBox defaultRamdisk_box = new TextBox();
	
	public SystemConfigTable(String sessionId)
	{
		this.sessionId = sessionId;
		this.setSpacing (10);
		this.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
		this.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		Label SystemConfigsHeader = new Label( "Cloud configuration:" );
		SystemConfigsHeader.setStyleName ( "euca-section-header" );
		this.add ( SystemConfigsHeader );
		HorizontalPanel c_hpanel = new HorizontalPanel ();
		c_hpanel.add ( this.c_grid );
		c_hpanel.add ( this.c_hint );
		c_hint.setWidth ("180");
		this.add ( c_hpanel );
		HorizontalPanel c_hpanel2 = new HorizontalPanel ();
		c_hpanel2.setSpacing (10);
		c_hpanel2.add ( new Button( "Save Configuration", new SaveCallback( this ) ) );
		c_hpanel2.add ( this.c_status );
		this.c_status.setText ("");
		this.c_status.setStyleName ("euca-greeting-pending");
		this.c_status.setWidth ("250");
		this.add ( c_hpanel2 );
		Label WalrusConfigsHeader = new Label( "Walrus configuration:" );
		WalrusConfigsHeader.setStyleName ( "euca-section-header" );
		this.add ( WalrusConfigsHeader );
		HorizontalPanel w_hpanel = new HorizontalPanel ();
		w_hpanel.add ( this.w_grid );
		w_hpanel.add ( this.w_hint );
		w_hint.setWidth ("180");
		this.add ( w_hpanel );
		HorizontalPanel w_hpanel2 = new HorizontalPanel ();
		w_hpanel2.setSpacing (10);
		w_hpanel2.add ( new Button( "Save Configuration", new SaveCallback( this ) ) );
		w_hpanel2.add ( this.w_status );
		this.w_status.setText ("");
		this.w_status.setStyleName ("euca-greeting-pending");
		this.w_status.setWidth ("250");
		this.add ( w_hpanel2 );
		this.rebuildTable ();
		EucalyptusWebBackend.App.getInstance().getSystemConfig( 
			this.sessionId, new GetCallback( this ) );
	}

	private void rebuildTable()
	{
		this.c_grid.clear ();
		this.c_grid.resize ( 2, 2 );
        this.c_grid.getColumnFormatter().setWidth(0, "190");
        this.c_grid.getColumnFormatter().setWidth(1, "260");
        int i = 0;

		// cloud parameters 
        this.c_grid.setWidget( i, 0, new Label( "Walrus URL:" ) );
        this.c_grid.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
		walrusURL_box.addChangeListener (new ChangeCallback (this));
        walrusURL_box.setVisibleLength(55);
		walrusURL_box.setText (SystemConfig.getStorageUrl());
		walrusURL_box.addFocusListener (new FocusHandler (c_hint, 
			"Warning! Changing the Walrus URL will invalidate any certificates created so far as well as any images uploaded into the system."));
        this.c_grid.setWidget( i++, 1, walrusURL_box );
		
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
		
		// walrus params
		this.w_grid.clear ();
		this.w_grid.resize ( 3, 2 );
        this.w_grid.getColumnFormatter().setWidth(0, "190");
        this.w_grid.getColumnFormatter().setWidth(1, "260");
		i = 0;
		
		this.w_grid.setWidget( i, 0, new Label( "Walrus path:" ) );
        this.w_grid.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
		walrusPath_box.addChangeListener (new ChangeCallback (this));
        walrusPath_box.setVisibleLength(55);
		walrusPath_box.setText (SystemConfig.getStoragePath());
		walrusPath_box.addFocusListener (new FocusHandler (w_hint, 
			"Warning! Changing the path may make inaccessible any content uploaded to the old path, including images, kernels, and ramdisks."));
        this.w_grid.setWidget( i++, 1, walrusPath_box );

		// 2nd row
        this.w_grid.setWidget( i, 0, new Label( "Max buckets per user:" ) );
        this.w_grid.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
		HorizontalPanel hpanel = new HorizontalPanel ();
		hpanel.setSpacing (0);
		this.w_grid.setWidget( i++, 1, hpanel );
		
		maxBuckets_box.addChangeListener (new ChangeCallback (this));
        maxBuckets_box.setVisibleLength(10);
		maxBuckets_box.setText (""+SystemConfig.getStorageMaxBucketsPerUser());
		hpanel.add (maxBuckets_box);
		
		hpanel.add ( new HTML ("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; Max bucket size: &nbsp;"));
		maxBucketSize_box.addChangeListener (new ChangeCallback (this));
        maxBucketSize_box.setVisibleLength(10);
		maxBucketSize_box.setText (""+SystemConfig.getStorageMaxBucketSizeInMB());
		maxBucketSize_box.addFocusListener (new FocusHandler (w_hint,
			"You are urged to consult the documentation before changing the default value!"));
		hpanel.add (maxBucketSize_box);
		hpanel.add ( new HTML ("&nbsp; MB"));
		
		// 3rd row
		this.w_grid.setWidget( i, 0, new Label( "Max cache size:" ) );
        this.w_grid.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
		HorizontalPanel hpanel3 = new HorizontalPanel ();
		hpanel3.setSpacing (0);
		this.w_grid.setWidget( i++, 1, hpanel3 );
		
		maxCacheSize_box.addChangeListener (new ChangeCallback (this));
        maxCacheSize_box.setVisibleLength(10);
		maxCacheSize_box.setText ("" + SystemConfig.getStorageMaxCacheSizeInMB());
		maxCacheSize_box.addFocusListener (new FocusHandler (w_hint,
			"You are urged to consult the documentation before changing the default value!"));
		hpanel3.add ( maxCacheSize_box );
		hpanel3.add ( new HTML ("&nbsp; MB"));
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
		this.SystemConfig.setStorageUrl                (this.walrusURL_box.getText());
		this.SystemConfig.setStoragePath               (this.walrusPath_box.getText());
		this.SystemConfig.setStorageMaxBucketsPerUser  (Integer.parseInt(this.maxBuckets_box.getText()));
		this.SystemConfig.setStorageMaxBucketSizeInMB  (Integer.parseInt(this.maxBucketSize_box.getText()));
		this.SystemConfig.setStorageMaxCacheSizeInMB   (Integer.parseInt(this.maxCacheSize_box.getText()));
		this.SystemConfig.setDefaultKernelId           (this.defaultKernel_box.getText());
		this.SystemConfig.setDefaultRamdiskId          (this.defaultRamdisk_box.getText());
	}
	
	class ChangeCallback implements ChangeListener, ClickListener {
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
			this.parent.w_status.setText ("Unsaved changes");
			this.parent.w_status.setStyleName ("euca-greeting-warning");
		}
		
		public void onClick (Widget sender) 
		{
			this.parent.updateStruct ();
			this.parent.c_status.setText ("Unsaved changes");
			this.parent.c_status.setStyleName ("euca-greeting-warning");
			this.parent.w_status.setText ("Unsaved changes");
			this.parent.w_status.setStyleName ("euca-greeting-warning");
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
			this.parent.w_status.setText ("Failed to contact server!");
			this.parent.w_status.setStyleName ("euca-greeting-error");
		}

		public void onSuccess( final Object o )
		{
			this.parent.c_status.setText ("Loaded configuration from server");
			this.parent.c_status.setStyleName ("euca-greeting-disabled");
			this.parent.w_status.setText ("Loaded configuration from server");
			this.parent.w_status.setStyleName ("euca-greeting-disabled");
			this.parent.SystemConfig = (SystemConfigWeb) o;
			this.parent.rebuildTable(); 
		}
	}

	class SaveCallback implements AsyncCallback, ClickListener {

		private SystemConfigTable parent;

		SaveCallback( final SystemConfigTable parent )
		{
			this.parent = parent;
		}

		public void onClick( final Widget widget )
		{
			this.parent.c_status.setText ("Saving...");
			this.parent.c_status.setStyleName ("euca-greeting-pending");
			this.parent.w_status.setText ("Saving...");
			this.parent.w_status.setStyleName ("euca-greeting-pending");
			EucalyptusWebBackend.App.getInstance().setSystemConfig( 
				this.parent.sessionId, this.parent.SystemConfig, this );
		}

		public void onFailure( final Throwable throwable )
		{
			this.parent.c_status.setText ("Failed to save!");
			this.parent.c_status.setStyleName ("euca-greeting-error");
			this.parent.w_status.setText ("Failed to save!");
			this.parent.w_status.setStyleName ("euca-greeting-error");
		}

		public void onSuccess( final Object o )
		{
			this.parent.c_status.setText ("Saved configuration to server");
			this.parent.c_status.setStyleName ("euca-greeting-disabled");
			this.parent.w_status.setText ("Saved configuration to server");
			this.parent.w_status.setStyleName ("euca-greeting-disabled");
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
