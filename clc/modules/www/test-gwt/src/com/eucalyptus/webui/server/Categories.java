package com.eucalyptus.webui.server;

import java.util.Arrays;
import java.util.List;
import com.eucalyptus.webui.client.service.CategoryItem;
import com.eucalyptus.webui.client.service.CategoryTag;
import com.eucalyptus.webui.client.service.EucalyptusServiceException;
import com.eucalyptus.webui.shared.query.QueryType;

public class Categories {
  
  public static List<CategoryTag> getTags( ) throws EucalyptusServiceException {
    String accountId = "123456";
    String userId = "4567";
    return Arrays.asList( new CategoryTag( "System", 
                                           Arrays.asList( new CategoryItem( "Start", "Start guide", "home",
                                                                            QueryBuilder.get( ).start( QueryType.start ).query( ) ),
                                                          new CategoryItem( "Service Components", "Configuration of service components", "config",
                                                          		              QueryBuilder.get( ).start( QueryType.config ).query( ) ) ) ),
                          new CategoryTag( "Identity",
                                           Arrays.asList( new CategoryItem( "Account", "Accounts", "dollar", 
                                                                            QueryBuilder.get( ).start( QueryType.account ).query( ) ),
                                                          new CategoryItem( "Group", "User groups", "group",
                                                                            QueryBuilder.get( ).start( QueryType.group ).add( "accountid", accountId ).query( ) ),
                                                          new CategoryItem( "User", "Users", "user",
                                                                            QueryBuilder.get( ).start( QueryType.user ).add( "accountid", accountId ).query( ) ),
                                                          new CategoryItem( "Policy", "Policies", "lock",
                                                                            QueryBuilder.get( ).start( QueryType.policy ).add( "userid", userId ).query( ) ),
                                                          new CategoryItem( "Key", "Access keys", "key",
                                                                            QueryBuilder.get( ).start( QueryType.key ).add( "userid", userId ).query( ) ),
                                                          new CategoryItem( "Certificate", "X509 certificates", "sun",
                                                                            QueryBuilder.get( ).start( QueryType.cert ).add( "userid", userId ).query( ) ) ) ),
                          new CategoryTag( "Resource",
                                           Arrays.asList( new CategoryItem( "Image", "Virtual machine images (EMIs)", "image",
                                                                            QueryBuilder.get( ).start( QueryType.image ).query( ) ),
                                                          new CategoryItem( "VmType", "Virtual machine types", "type",
                                                                            QueryBuilder.get( ).start( QueryType.vmtype ).query( ) ),
                                                          new CategoryItem( "Report", "Resource usage report", "report",
                                                                            QueryBuilder.get( ).start( QueryType.report ).query( ) ) ) ),
                          new CategoryTag( "Extras",
                                           Arrays.asList( new CategoryItem( "Downloads", "Extra downloads", "down",
                                                                            QueryBuilder.get( ).start( QueryType.downloads ).query( ) ),
                                                          new CategoryItem( "RightScale", "Register RightScale", "rightscale",
                                                                            QueryBuilder.get( ).start( QueryType.rightscale ).query( ) ) ) ) );
  }
  
}
