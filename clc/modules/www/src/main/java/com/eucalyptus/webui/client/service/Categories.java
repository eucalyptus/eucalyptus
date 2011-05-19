package com.eucalyptus.webui.client.service;

import java.util.ArrayList;

public class Categories {
  
  public static final String START = "start";
  public static final String CONFIGURATION = "config";
  public static final String ACCOUNT = "account";
  public static final String GROUP = "group";
  public static final String USER = "user";
  public static final String POLICY = "policy";
  public static final String KEY = "key";
  public static final String CERTIFICATE = "cert";
  public static final String IMAGE = "image";
  public static final String VMTYPE = "vmtype";
  public static final String REPORT = "report";
  public static final String DOWNLOADS = "downloads";
  public static final String RIGHTSCALE = "rightscale";
  
  private static final ArrayList<CategoryTag> TAGS = new ArrayList<CategoryTag>( );
  static {
    ArrayList<CategoryItem> list = new ArrayList<CategoryItem>( );
    list.add( new CategoryItem( "Start", "Start guide", "home", QueryBuilder.get( ).start( START ).query( ) ) );
    list.add( new CategoryItem( "Configuration", "System configurations", "config", QueryBuilder.get( ).start( CONFIGURATION ).query( ) ) );
    TAGS.add( new CategoryTag( "System", list ) );
    list = new ArrayList<CategoryItem>( );
    list.add( new CategoryItem( "Account", "Accounts", "dollar", QueryBuilder.get( ).start( ACCOUNT ).query( ) ) );
    list.add( new CategoryItem( "Group", "User groups", "group", QueryBuilder.get( ).start( GROUP ).query( ) ) );
    list.add( new CategoryItem( "User", "Users", "user", QueryBuilder.get( ).start( USER ).query( ) ) );
    list.add( new CategoryItem( "Policy", "Policies", "lock", QueryBuilder.get( ).start( POLICY ).query( ) ) );
    list.add( new CategoryItem( "Key", "Access keys", "key", QueryBuilder.get( ).start( KEY ).query( ) ) );
    list.add( new CategoryItem( "Certificate", "X509 certificates", "sun", QueryBuilder.get( ).start( CERTIFICATE ).query( ) ) );
    TAGS.add( new CategoryTag( "Identity", list ) );
    list = new ArrayList<CategoryItem>( );
    list.add( new CategoryItem( "Image", "Virtual machine images (EMIs)", "image", QueryBuilder.get( ).start( IMAGE ).query( ) ) );
    list.add( new CategoryItem( "VmType", "Virtual machine types", "type", QueryBuilder.get( ).start( VMTYPE ).query( ) ) );
    list.add( new CategoryItem( "Report", "Resource usage report", "report", QueryBuilder.get( ).start( REPORT ).query( ) ) );
    TAGS.add( new CategoryTag( "Resource", list ) );
    list = new ArrayList<CategoryItem>( );
    list.add( new CategoryItem( "Downloads", "Extra downloads", "down", QueryBuilder.get( ).start( DOWNLOADS ).query( ) ) );
    list.add( new CategoryItem( "RightScale", "Register RightScale", "rightscale", QueryBuilder.get( ).start( RIGHTSCALE ).query( ) ) );
    TAGS.add( new CategoryTag( "Miscs", list ) );    
  }
  
  public static ArrayList<CategoryTag> getTags( ) {
    return TAGS;
  }
  
}
