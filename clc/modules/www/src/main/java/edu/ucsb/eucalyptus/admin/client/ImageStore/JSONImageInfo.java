package edu.ucsb.eucalyptus.admin.client.ImageStore;

import java.util.List;
import java.util.ArrayList;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;


public class JSONImageInfo implements ImageInfo {

    private JSONObject object;

    private JSONImageInfo(JSONObject object) {
        this.object = object;
    }
    
    static public ImageInfo fromString(String data) {
        return fromObject(JSONUtil.parseObject(data));
    }

    static public ImageInfo fromObject(JSONObject object) {
        return new JSONImageInfo(object);
    }

    static public List<ImageInfo> fromObjectArray(JSONValue array) {
        return JSONUtil.adaptArray(array, new JSONUtil.ObjectAdapter<ImageInfo>() {
            public ImageInfo adaptObject(JSONObject object) {
                return fromObject(object);
            }
        });
    }
    
    public String getUri() {
        return JSONUtil.asString(object.get("uri"));
    }

    public String getIconUri() {
        return JSONUtil.asString(object.get("icon-uri"));
    }

    public String getTitle() {
        return JSONUtil.asString(object.get("title"));
    }

    public String getSummary() {
        return JSONUtil.asString(object.get("summary"));
    }

    public String getDescriptionHtml() {
        return JSONUtil.asString(object.get("description-html"));
    }

    public String getVersion() {
        return JSONUtil.asString(object.get("version"));
    }

    public Integer getSizeInMB() {
        return JSONUtil.asInteger(object.get("size-in-mb"));
    }

    public List<String> getTags() {
        ArrayList<String> tags = new ArrayList<String>();
        JSONValue value = object.get("tags");
        if (value != null) {
            JSONArray array = value.isArray();
            if (array != null) {
                for (int i = 0; i != array.size(); i++) {
                    String item = JSONUtil.asString(array.get(i));
                    if (item != null) {
                        tags.add(item);
                    }
                }
            }
        }
        return tags;
    }

    public String getProviderTitle() {
        JSONValue providerValue = object.get("provider");
        if (providerValue != null) {
            JSONObject providerObject = providerValue.isObject();
            if (providerObject != null) {
                return JSONUtil.asString(providerObject.get("title"));
            }
        }
        return null;
    }

    public String getProviderUri() {
        JSONValue providerValue = object.get("provider");
        if (providerValue != null) {
            JSONObject providerObject = providerValue.isObject();
            if (providerObject != null) {
                return JSONUtil.asString(providerObject.get("uri"));
            }
        }
        return null;
    }

}
