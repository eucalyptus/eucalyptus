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
