/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.imaging;

import java.util.List;
import java.util.ArrayList;
import java.net.URI;
import java.net.URISyntaxException;

import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.objectstorage.ObjectStorage;
import com.eucalyptus.util.dns.DomainNames;

import org.xbill.DNS.Name;

public class UrlValidator {
  private final List<URI> osgUrls = new ArrayList<URI>();
  
  public UrlValidator() {
    List<ServiceConfiguration> osgs = ServiceConfigurations.list(ObjectStorage.class);
    for(ServiceConfiguration conf:osgs) {
      osgUrls.add( conf.getUri() );
    }
  }
  /**
   * Checks if URL is pointing to a Eucalyptus
   * @param url
   * @return
   */
  public boolean isEucalyptusUrl(String url) {
    URI in = null;
    if (url == null)
      return false;

    try {
      in = new URI(url.startsWith("http") ? url : "http://" + url);
    } catch (URISyntaxException e) {
      return false;
    }
    
    if ( DomainNames.isSystemSubdomain( Name.fromConstantString(in.getHost()+".") ) )
      return true;
    
    for(URI u:osgUrls){
      if (u.getHost().equalsIgnoreCase(in.getHost()))
        return true;
    }
    return false;
  }
}