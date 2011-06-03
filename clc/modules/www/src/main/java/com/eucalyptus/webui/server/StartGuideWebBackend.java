package com.eucalyptus.webui.server;

import java.util.ArrayList;
import java.util.Arrays;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.webui.client.service.GuideItem;
import com.eucalyptus.webui.shared.query.QueryType;

public class StartGuideWebBackend {

  private static final Logger LOG = Logger.getLogger( StartGuideWebBackend.class );
  
  public static final String PROFILE = "profile";
  public static final String SERVICE = "service";
  public static final String RESOURCE = "resource";
  public static final String IAM = "iam";

  public static ArrayList<GuideItem> getGuide( User user, String snippet ) {
    if ( SERVICE.equals( snippet ) ) {
      return getServiceGuides( );
    } else if ( IAM.equals( snippet ) ) {
      try {
        return getIamGuides( user );
      } catch ( Exception e ) {
        LOG.error( e, e );
      }
    }
    return new ArrayList<GuideItem>( );
  }

  private static ArrayList<GuideItem> getIamGuides( User user ) throws Exception {
    String accountId = user.getAccount( ).getAccountNumber( );
    return new ArrayList<GuideItem>( Arrays.asList( new GuideItem( "View or change your personal profile details",
                                                                   QueryBuilder.get( ).start( QueryType.user ).add( EuareWebBackend.ID, user.getUserId( ) ).url( ),
                                                                   "" ),
                                                    new GuideItem( "Show all accounts under your management",
                                                                   QueryBuilder.get( ).start( QueryType.account ).url( ),
                                                                   "account" ),
                                                    new GuideItem( "Show your account's groups",
                                                                    QueryBuilder.get( ).start( QueryType.group ).add( EuareWebBackend.ACCOUNTID, accountId ).url( ),
                                                                   "group" ),
                                                    new GuideItem( "Show your account's users",
                                                                   QueryBuilder.get( ).start( QueryType.user ).add( EuareWebBackend.ACCOUNTID, accountId ).url( ),
                                                                   "user" ),
                                                    new GuideItem( "Show the keys you have",
                                                                   QueryBuilder.get( ).start( QueryType.key ).add( EuareWebBackend.USERID, user.getUserId( ) ).url( ),
                                                                   "key" ) ) );
  }

  private static ArrayList<GuideItem> getServiceGuides( ) {
    return new ArrayList<GuideItem>( Arrays.asList( new GuideItem( "View and configure cloud service components",
                                                                   QueryBuilder.get( ).start( QueryType.config ).url( ),
                                                                   "service" ),
                                                    new GuideItem( "View all the images you can access",
                                                                   QueryBuilder.get( ).start( QueryType.image ).url( ),
                                                                   "image" ),
                                                    new GuideItem( "View and configure virtual machine types",
                                                                    QueryBuilder.get( ).start( QueryType.vmtype ).url( ),
                                                                   "vmtype" ),
                                                    new GuideItem( "Generate cloud resource usage report",
                                                                   QueryBuilder.get( ).start( QueryType.report ).url( ),
                                                                   "report" ) ) );
  }
  
}
