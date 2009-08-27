package edu.ucsb.eucalyptus.admin.client.extensions.store;

import java.util.List;
import java.util.ArrayList;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;


public class JSONImageStoreResponse implements ImageStoreResponse {

    private final boolean hasImageInfos;
    private final boolean hasImageStates;
    private final boolean hasImageSections;

    private final List<ImageInfo> imageInfos;
    private final List<ImageState> imageStates;
    private final List<ImageSection> imageSections;

    private String errorMessage;

    private JSONImageStoreResponse(JSONObject object) {
        final JSONValue imagesValue = object.get("images");
        hasImageInfos = (imagesValue != null);
        imageInfos = JSONImageInfo.fromObjectArray(imagesValue);

        final JSONValue statesValue = object.get("states");
        hasImageStates = (statesValue != null);
        imageStates = JSONImageState.fromObjectArray(statesValue);

        if (object.containsKey("state") && object.isObject() != null) {
            // When we access a state URI or perform an action, we get
            // back an individual state, but we handle it the same way
            // in the UI.
            JSONObject stateObject = object.get("state").isObject();
            imageStates.add(JSONImageState.fromObject(stateObject));
        }

        final JSONValue sectionsValue = object.get("sections");
        hasImageSections = (sectionsValue != null);
        imageSections = JSONImageSection.fromObjectArray(sectionsValue);

        errorMessage = JSONUtil.asString(object.get("error-message"));
    }

    static public ImageStoreResponse fromString(String data) {
        return fromObject(JSONUtil.parseObject(data));
    }

    static public ImageStoreResponse fromObject(JSONObject object) {
        return new JSONImageStoreResponse(object);
    }

    public boolean hasImageInfos() {
        return hasImageInfos;
    }

    public boolean hasImageStates() {
        return hasImageStates;
    }

    public boolean hasImageSections() {
        return hasImageSections;
    }

    public List<ImageInfo> getImageInfos() {
        return imageInfos;
    }

    public List<ImageState> getImageStates() {
        return imageStates;
    }

    public List<ImageSection> getImageSections() {
        return imageSections;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
