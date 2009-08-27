/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
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
