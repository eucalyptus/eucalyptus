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

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;  


public class ImageStoreClient {

    private final String DASHBOARD_URI = "http://localhost:52780/api/dashboard";
    private final String SEARCH_URI = "http://localhost:52780/api/search";
    private final String STATES_URI = "http://localhost:52780/api/states";
    private ImageStoreServiceAsync service = (ImageStoreServiceAsync) GWT.create(ImageStoreService.class);
    
    private final String sessionId;

    private final ImageStoreService.Method GET = ImageStoreService.Method.GET;
    private final ImageStoreService.Method POST = ImageStoreService.Method.POST;

    public ImageStoreClient(String sessionId) {
        this.sessionId = sessionId;
    }

    void getDashboard(AsyncCallback<ImageStoreResponse> callback) {
        service.requestJSON(sessionId, ImageStoreService.Method.GET,
                            DASHBOARD_URI, null,
                            new WrapperCallback(callback));
    }

    void search(String text, AsyncCallback<ImageStoreResponse> callback) {
        ImageStoreService.Parameter[] params = new ImageStoreService.Parameter[1];
        params[0] = new ImageStoreService.Parameter("q", text);
        service.requestJSON(sessionId, GET, SEARCH_URI, params,
                            new WrapperCallback(callback));
    }

    void runAction(ImageState imageState, ImageState.Action action,
                   AsyncCallback<ImageStoreResponse> callback) {
        String actionUri = imageState.getActionUri(action);
        service.requestJSON(sessionId, POST, actionUri, null,
                            new WrapperCallback(callback));
    }

    void getImageStates(List<ImageInfo> imageInfos,
                        AsyncCallback<ImageStoreResponse> callback) {
        ImageStoreService.Parameter[] params = new ImageStoreService.Parameter[imageInfos.size()];
        for (int i = 0; i != imageInfos.size(); i++) {
            String uri = imageInfos.get(i).getUri();
            params[i] = new ImageStoreService.Parameter("image-uri", uri);
        }
        service.requestJSON(sessionId, POST, STATES_URI, params,
                            new WrapperCallback(callback));
    }

    private static class WrapperCallback implements AsyncCallback<String> {

        private AsyncCallback<ImageStoreResponse> userCallback;

        public WrapperCallback(AsyncCallback<ImageStoreResponse> userCallback) {
            this.userCallback = userCallback;
        }

        public void onFailure(Throwable caught) {
            //GWT.log("JSON request failure", caught);
            userCallback.onFailure(caught);
        }

        public void onSuccess(String json) {
            //GWT.log("JSON result: " + json, null);
            ImageStoreResponse response = JSONImageStoreResponse.fromString(json);
            userCallback.onSuccess(response);
        }

    }

}
