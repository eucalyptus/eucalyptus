package com.eucalyptus.webui.server;

import java.util.ArrayList;
import java.util.HashMap;
import com.eucalyptus.webui.client.service.CategoryItem;
import com.eucalyptus.webui.client.service.CategoryTag;
import com.eucalyptus.webui.client.service.EucalyptusService;
import com.eucalyptus.webui.client.service.EucalyptusServiceException;
import com.eucalyptus.webui.client.service.LoginUserProfile;
import com.eucalyptus.webui.client.service.Session;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class EucalyptusServiceImpl extends RemoteServiceServlet implements EucalyptusService {

  private static final long serialVersionUID = 1L;

  @Override
  public Session login( String fullname, String password ) throws EucalyptusServiceException {
    return new Session( "FAKESESSIONID" );
  }

  @Override
  public LoginUserProfile getLoginUserProfile( Session session ) throws EucalyptusServiceException {
    return new LoginUserProfile( "admin", "eucalyptus" );
  }

  @Override
  public HashMap<String, String> getSystemProperties( Session session ) throws EucalyptusServiceException {
    HashMap<String, String> prop = Maps.newHashMap( );
    prop.put( "version", "EEE 3.0" );
    return null;
  }

  @Override
  public ArrayList<CategoryTag> getCategory( Session session ) throws EucalyptusServiceException {
    ArrayList<CategoryTag> tags = Lists.newArrayList( );
    ArrayList<CategoryItem> list = Lists.newArrayList( );
    list.add( new CategoryItem( "Start", "Start information", "home", "start:" ) );
    list.add( new CategoryItem( "Service", "System service components", "service", "service:" ) );
    tags.add( new CategoryTag( "System", list ) );
    list = Lists.newArrayList( );
    list.add( new CategoryItem( "Account", "Accounts", "user", "account:" ) );
    list.add( new CategoryItem( "Group", "User groups", "user", "group:" ) );
    list.add( new CategoryItem( "User", "Users", "user", "user:" ) );
    tags.add( new CategoryTag( "Identity", list ) );
    list = Lists.newArrayList( );
    list.add( new CategoryItem( "Image", "Virtual machine images (EMIs)", "image", "image:" ) );
    list.add( new CategoryItem( "Report", "Resource usage report", "report", "report:" ) );
    tags.add( new CategoryTag( "Resource", list ) );
    return tags;
  }
}
