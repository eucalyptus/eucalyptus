package edu.ucsb.eucalyptus.admin.client.extensions.store;

import java.util.List;


public interface ImageInfo {

    String getUri();
    String getIconUri();
    String getTitle();
    String getSummary();
    String getDescriptionHtml();
    String getVersion();
    Integer getSizeInMB();
    List<String> getTags();
    String getProviderTitle();
    String getProviderUri();

}
