package edu.ucsb.eucalyptus.admin.client.ImageStore;

import java.util.List;


public interface ImageStoreResponse {
    boolean hasImageInfos();
    boolean hasImageStates();
    boolean hasImageSections();
    List<ImageInfo> getImageInfos();
    List<ImageState> getImageStates();
    List<ImageSection> getImageSections();
    String getErrorMessage();
}
