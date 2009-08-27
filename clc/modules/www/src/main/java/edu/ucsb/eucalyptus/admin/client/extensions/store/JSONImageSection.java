package edu.ucsb.eucalyptus.admin.client.extensions.store;

import java.util.List;
import java.util.ArrayList;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONException;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;


public class JSONImageSection implements ImageSection {

    private JSONObject object;

    private JSONImageSection(JSONObject object) {
        this.object = object;
    }

    static public JSONImageSection fromString(String data) {
        return fromObject(JSONUtil.parseObject(data));
    }

    static public JSONImageSection fromObject(JSONObject object) {
        return new JSONImageSection(object);
    }

    static public List<ImageSection> fromObjectArray(JSONValue array) {
        return JSONUtil.adaptArray(array, new JSONUtil.ObjectAdapter<ImageSection>() {
            public ImageSection adaptObject(JSONObject object) {
                return fromObject(object);
            }
        });
    }
 
    public String getTitle() {
        return JSONUtil.asString(object.get("title"));
    }

    public String getSummary() {
        return JSONUtil.asString(object.get("summary"));
    }

    public List<String> getImageUris() {
        JSONValue imagesValue = object.get("image-uris");
        List<String> imageUris = new ArrayList<String>();
        if (imagesValue != null && imagesValue.isArray() != null) {
            JSONArray imagesArray = imagesValue.isArray();
            for (int i = 0; i != imagesArray.size(); i++) {
                String uri = JSONUtil.asString(imagesArray.get(i));
                if (uri != null) {
                    imageUris.add(uri);
                }
            }
        }
        return imageUris;
    }

}
