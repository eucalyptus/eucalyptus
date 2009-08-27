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
