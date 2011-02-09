package edu.ucsb.eucalyptus.admin.client.reports;

import java.util.HashMap;
import java.util.Map;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.DecoratedStackPanel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.VerticalPanel;
import edu.ucsb.eucalyptus.admin.client.AccountingControl;
import edu.ucsb.eucalyptus.admin.client.util.Observer;
import edu.ucsb.eucalyptus.admin.client.util.XHTML;

public class ReportList extends DecoratedStackPanel implements Observer {
  private final AccountingControl    controller;
  private VerticalPanel              systemReports;
  private VerticalPanel              resourceReports;
  private VerticalPanel              serviceReports;
  private Map<String, VerticalPanel> groupMap;
  
  public ReportList( AccountingControl controller ) {
    this.ensureDebugId( "ReportList" );
    this.controller = controller;
    this.systemReports = new VerticalPanel( ) {
      {
        ensureDebugId( "systemReports" );
      }
    };
    this.resourceReports = new VerticalPanel( ) {
      {
        ensureDebugId( "resourceReports" );
      }
    };
    this.serviceReports = new VerticalPanel( ) {
      {
        ensureDebugId( "serviceReports" );
      }
    };
    this.groupMap = new HashMap<String, VerticalPanel>( ) {
      {
        put( "system", ReportList.this.systemReports );
        put( "user", ReportList.this.resourceReports );
        put( "service", ReportList.this.serviceReports );
      }
    };
  }
  
  public void redraw( ) {
    this.clear( );
    this.add( this.systemReports, XHTML.headerWithImage( "System Events", AccountingControl.RESOURCES.systemReports( ),
                                                         AccountingControl.RESOURCES.REPORT_BAR_STYLE ), true );
    this.add( this.resourceReports, XHTML.headerWithImage( "Resource Usage", AccountingControl.RESOURCES.resourceReports( ),
                                                           AccountingControl.RESOURCES.REPORT_BAR_STYLE ), true );
    this.add( this.serviceReports, XHTML.headerWithImage( "Registered Components", AccountingControl.RESOURCES.serviceReports( ),
                                                          AccountingControl.RESOURCES.REPORT_BAR_STYLE ), true );
    makeGroupPanel( );
  }

  private void makeGroupPanel( ) {
    for( final String group : this.controller.getGroupMap( ).keySet( ) ) {
      if( !this.groupMap.containsKey( group ) ) {
        this.groupMap.put( group, new VerticalPanel( ){
          {
            ensureDebugId( group + "Reports" );
          }
        } );
      }
      VerticalPanel groupPanel = this.groupMap.get( group );
      groupPanel.clear( );
      groupPanel.setStyleName( AccountingControl.RESOURCES.REPORT_BAR_STYLE );
      if( "service".equals( group ) ) {
        Tree serviceTree = new Tree( ) {{
          setAnimationEnabled( true );
          addSelectionHandler(new SelectionHandler<TreeItem>() {
            @Override
            public void onSelection( SelectionEvent<TreeItem> event ) {
              TreeItem item = event.getSelectedItem( );
//              if( item.getUserObject( ) != null ) {
//                ReportInfo r = (ReportInfo)item.getUserObject( );
//                ReportList.this.controller.setCurrentReport( r );
//                item.setSelected( true );
//                item.setState( true, false );
//              }
            }
          });
        }};
        for ( final ReportInfo info : this.controller.getGroupMap( ).get( group ) ) {
          if( "walrus".equals( info.getComponent( ) ) ) {
            TreeItem walrusItem = serviceTree.addItem( info.getDisplayName( ) );
            walrusItem.setUserObject( info );
          }
        }
        Map<String,TreeItem> clusterItems = new HashMap<String,TreeItem>();
        for ( final ReportInfo info : this.controller.getGroupMap( ).get( group ) ) {
          if( "cluster".equals( info.getComponent( ) ) ) {
            TreeItem clusterRoot = serviceTree.addItem( info.getClusterName( ) );
            clusterItems.put( info.getClusterName( ), clusterRoot );
            TreeItem clusterItem = clusterRoot.addItem( info.getDisplayName( ) );
            clusterItem.setUserObject( info );
          }
        }
        for ( final ReportInfo info : this.controller.getGroupMap( ).get( group ) ) {
          if( "storage".equals( info.getComponent( ) ) && clusterItems.containsKey( info.getClusterName( ) ) ) {
            TreeItem scItem = clusterItems.get( info.getClusterName( ) ).addItem( info.getDisplayName( ) );
            scItem.setUserObject( info );
          }
        }
        for ( final ReportInfo info : this.controller.getGroupMap( ).get( group ) ) {
          if( "node".equals( info.getComponent( ) ) && clusterItems.containsKey( info.getClusterName( ) ) ) {
            TreeItem ncItem = clusterItems.get( info.getClusterName( ) ).addItem( info.getDisplayName( ) );
            ncItem.setUserObject( info );
          }
        }
        groupPanel.add( serviceTree );
      } else {
        for ( ReportInfo info : this.controller.getGroupMap( ).get( group ) ) {
          if ( info.equals( this.controller.getCurrentReport( ) ) ) {
            info.getButton( ).setStyleName( AccountingControl.RESOURCES.ACCT_REPORT_CURRENT_BUTTON );
          } else {
            info.getButton( ).setStyleName( AccountingControl.RESOURCES.ACCT_REPORT_BUTTON );
          }
          groupPanel.add( info.getButton( ) );
        }
      }
    }
  }
  
  @Override
  public void update( )
  {
  }
}
