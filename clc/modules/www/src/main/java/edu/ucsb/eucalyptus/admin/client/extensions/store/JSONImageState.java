package edu.ucsb.eucalyptus.admin.client.extensions.store;

import java.util.List;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;


public class JSONImageState implements ImageState {

    private JSONObject object;

    private JSONImageState(JSONObject object) {
        this.object = object;
    }

    static public ImageState fromString(String data) {
        return fromObject(JSONUtil.parseObject(data));
    }

    static public ImageState fromObject(JSONObject object) {
        return new JSONImageState(object);
    }

    static public List<ImageState> fromObjectArray(JSONValue array) {
        return JSONUtil.adaptArray(array, new JSONUtil.ObjectAdapter<ImageState>() {
            public ImageState adaptObject(JSONObject object) {
                return fromObject(object);
            }
        });
    }

    public String getImageUri() {
        return JSONUtil.asString(object.get("image-uri"));
    }

    public String getErrorMessage() {
        return JSONUtil.asString(object.get("error-message"));
    }

    public Status getStatus() {
        String status = JSONUtil.asString(object.get("status"));
        try {
            return Enum.valueOf(Status.class, status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Status.UNKNOWN;
        }
    }

    public Integer getProgressPercentage() {
        return JSONUtil.asInteger(object.get("progress-percentage"));
    }

    public String getActionUri(Action action) {
        JSONValue actionsValue = object.get("actions");
        if (actionsValue != null) {
            JSONObject actionsObject = actionsValue.isObject();
            if (actionsObject != null) {
                String actionKey = action.toString().toLowerCase().replace('_', '-');
                return JSONUtil.asString(actionsObject.get(actionKey));
            }
        }
        return null;
    }

    public boolean hasAction(Action action) {
        return getActionUri(action) != null;
    }

    public boolean isUpgrade() {
        return JSONUtil.asBoolean(object.get("is-upgrade"), false);
    }

}
