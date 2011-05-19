package com.eucalyptus.webui.server;

import java.util.ArrayList;
import com.eucalyptus.webui.client.service.CategoryConstants;
import com.eucalyptus.webui.client.service.CategoryItem;
import com.eucalyptus.webui.client.service.CategoryTag;

public class Categories {
  
  private static final ArrayList<CategoryTag> TAGS = new ArrayList<CategoryTag>( );
  static {
    ArrayList<CategoryItem> list = new ArrayList<CategoryItem>( );
    list.add( new CategoryItem( "Start", "Start guide", "home", QueryBuilder.get( ).start( CategoryConstants.START ).query( ) ) );
    list.add( new CategoryItem( "Configuration", "System configurations", "config", QueryBuilder.get( ).start( CategoryConstants.CONFIGURATION ).query( ) ) );
    TAGS.add( new CategoryTag( "System", list ) );
    list = new ArrayList<CategoryItem>( );
    list.add( new CategoryItem( "Account", "Accounts", "dollar", QueryBuilder.get( ).start( CategoryConstants.ACCOUNT ).query( ) ) );
    list.add( new CategoryItem( "Group", "User groups", "group", QueryBuilder.get( ).start( CategoryConstants.GROUP ).query( ) ) );
    list.add( new CategoryItem( "User", "Users", "user", QueryBuilder.get( ).start( CategoryConstants.USER ).query( ) ) );
    list.add( new CategoryItem( "Policy", "Policies", "lock", QueryBuilder.get( ).start( CategoryConstants.POLICY ).query( ) ) );
    list.add( new CategoryItem( "Key", "Access keys", "key", QueryBuilder.get( ).start( CategoryConstants.KEY ).query( ) ) );
    list.add( new CategoryItem( "Certificate", "X509 certificates", "sun", QueryBuilder.get( ).start( CategoryConstants.CERTIFICATE ).query( ) ) );
    TAGS.add( new CategoryTag( "Identity", list ) );
    list = new ArrayList<CategoryItem>( );
    list.add( new CategoryItem( "Image", "Virtual machine images (EMIs)", "image", QueryBuilder.get( ).start( CategoryConstants.IMAGE ).query( ) ) );
    list.add( new CategoryItem( "VmType", "Virtual machine types", "type", QueryBuilder.get( ).start( CategoryConstants.VMTYPE ).query( ) ) );
    list.add( new CategoryItem( "Report", "Resource usage report", "report", QueryBuilder.get( ).start( CategoryConstants.REPORT ).query( ) ) );
    TAGS.add( new CategoryTag( "Resource", list ) );
    list = new ArrayList<CategoryItem>( );
    list.add( new CategoryItem( "Downloads", "Extra downloads", "down", QueryBuilder.get( ).start( CategoryConstants.DOWNLOADS ).query( ) ) );
    list.add( new CategoryItem( "RightScale", "Register RightScale", "rightscale", QueryBuilder.get( ).start( CategoryConstants.RIGHTSCALE ).query( ) ) );
    TAGS.add( new CategoryTag( "Miscs", list ) );    
  }
  
  public static ArrayList<CategoryTag> getTags( ) {
    return TAGS;
  }
  
}
