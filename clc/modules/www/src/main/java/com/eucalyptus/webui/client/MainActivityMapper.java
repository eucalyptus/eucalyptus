package com.eucalyptus.webui.client;

import com.eucalyptus.webui.client.activity.AccountActivity;
import com.eucalyptus.webui.client.activity.ApproveActivity;
import com.eucalyptus.webui.client.activity.CertActivity;
import com.eucalyptus.webui.client.activity.ErrorSinkActivity;
import com.eucalyptus.webui.client.activity.GroupActivity;
import com.eucalyptus.webui.client.activity.ImageActivity;
import com.eucalyptus.webui.client.activity.KeyActivity;
import com.eucalyptus.webui.client.activity.LogoutActivity;
import com.eucalyptus.webui.client.activity.ConfigActivity;
import com.eucalyptus.webui.client.activity.PolicyActivity;
import com.eucalyptus.webui.client.activity.RejectActivity;
import com.eucalyptus.webui.client.activity.ReportActivity;
import com.eucalyptus.webui.client.activity.StartActivity;
import com.eucalyptus.webui.client.activity.UserActivity;
import com.eucalyptus.webui.client.activity.VmTypeActivity;
import com.eucalyptus.webui.client.place.AccountPlace;
import com.eucalyptus.webui.client.place.ApprovePlace;
import com.eucalyptus.webui.client.place.CertPlace;
import com.eucalyptus.webui.client.place.ErrorSinkPlace;
import com.eucalyptus.webui.client.place.GroupPlace;
import com.eucalyptus.webui.client.place.ImagePlace;
import com.eucalyptus.webui.client.place.KeyPlace;
import com.eucalyptus.webui.client.place.LogoutPlace;
import com.eucalyptus.webui.client.place.ConfigPlace;
import com.eucalyptus.webui.client.place.PolicyPlace;
import com.eucalyptus.webui.client.place.RejectPlace;
import com.eucalyptus.webui.client.place.ReportPlace;
import com.eucalyptus.webui.client.place.StartPlace;
import com.eucalyptus.webui.client.place.UserPlace;
import com.eucalyptus.webui.client.place.VmTypePlace;
import com.google.gwt.activity.shared.Activity;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.place.shared.Place;

public class MainActivityMapper implements ActivityMapper {
  
  private ClientFactory clientFactory;
  
  public MainActivityMapper( ClientFactory clientFactory ) {
    super( );
    this.clientFactory = clientFactory;
  }
  
  @Override
  public Activity getActivity( Place place ) {
    if ( place instanceof StartPlace ) {
      return new StartActivity( ( StartPlace )place, this.clientFactory );
    } else if ( place instanceof ConfigPlace ) {
      return new ConfigActivity( ( ConfigPlace )place, this.clientFactory );
    } else if ( place instanceof ErrorSinkPlace ) {
      return new ErrorSinkActivity( ( ErrorSinkPlace )place, this.clientFactory );
    } else if ( place instanceof LogoutPlace ) {
      return new LogoutActivity( ( LogoutPlace )place, this.clientFactory );
    } else if ( place instanceof AccountPlace ) {
      return new AccountActivity( ( AccountPlace )place, this.clientFactory );
    } else if ( place instanceof VmTypePlace ) {
      return new VmTypeActivity( ( VmTypePlace )place, this.clientFactory );
    } else if ( place instanceof ReportPlace ) {
      return new ReportActivity( ( ReportPlace )place, this.clientFactory );
    } else if ( place instanceof GroupPlace ) {
      return new GroupActivity( ( GroupPlace )place, this.clientFactory );
    } else if ( place instanceof UserPlace ) {
      return new UserActivity( ( UserPlace )place, this.clientFactory );
    } else if ( place instanceof PolicyPlace ) {
      return new PolicyActivity( ( PolicyPlace )place, this.clientFactory );
    } else if ( place instanceof KeyPlace ) {
      return new KeyActivity( ( KeyPlace )place, this.clientFactory );
    } else if ( place instanceof CertPlace ) {
      return new CertActivity( ( CertPlace )place, this.clientFactory );
    } else if ( place instanceof ImagePlace ) {
      return new ImageActivity( ( ImagePlace )place, this.clientFactory );
    } else if ( place instanceof ApprovePlace ) {
      return new ApproveActivity( ( ApprovePlace )place, this.clientFactory );
    } else if ( place instanceof RejectPlace ) {
      return new RejectActivity( ( RejectPlace )place, this.clientFactory );
    }
    return null;
  }
  
}
