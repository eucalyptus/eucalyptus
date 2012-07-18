/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.webui.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.eucalyptus.webui.client.service.CategoryItem;
import com.eucalyptus.webui.client.service.CategoryTag;
import com.eucalyptus.webui.client.service.EucalyptusServiceException;
import com.eucalyptus.webui.shared.query.QueryType;

public class Categories {
  
  public static ArrayList<CategoryTag> getTags( ) throws EucalyptusServiceException {
    String accountId = "123456";
    String userId = "4567";
    return new ArrayList<CategoryTag>( Arrays.asList( new CategoryTag( "System", 
                                           new ArrayList<CategoryItem>( Arrays.asList( new CategoryItem( "Start", "Start guide", "home",
                                                                            QueryBuilder.get( ).start( QueryType.start ).query( ) ),
                                                          new CategoryItem( "Service Components", "Configuration of service components", "config",
                                                          		              QueryBuilder.get( ).start( QueryType.config ).query( ) ) ) ) ),
                          new CategoryTag( "Identity",
                                           new ArrayList<CategoryItem>( Arrays.asList( new CategoryItem( "Account", "Accounts", "dollar", 
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
                                                                            QueryBuilder.get( ).start( QueryType.cert ).add( "userid", userId ).query( ) ) ) ) ),
                          new CategoryTag( "Resource",
                                           new ArrayList<CategoryItem>( Arrays.asList( new CategoryItem( "Image", "Virtual machine images (EMIs)", "image",
                                                                            QueryBuilder.get( ).start( QueryType.image ).query( ) ),
                                                          new CategoryItem( "VmType", "Virtual machine types", "type",
                                                                            QueryBuilder.get( ).start( QueryType.vmtype ).query( ) ),
                                                          new CategoryItem( "Report", "Resource usage report", "report",
                                                                            QueryBuilder.get( ).start( QueryType.report ).query( ) ) ) ) ) ) );
  }
  
}
