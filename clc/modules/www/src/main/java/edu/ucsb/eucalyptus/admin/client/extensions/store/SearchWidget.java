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

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.FlowPanel;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;

import com.google.gwt.core.client.GWT;


public class SearchWidget extends Composite {

    private final String SEARCH_BUTTON_URI = GraphicsUtil.uri("search-button.png");

    TextBox searchBox = new TextBox();
    Image searchButtonImage = new Image(SEARCH_BUTTON_URI);
    
    public SearchWidget() {
        Label titleLabel = new Label("Search");
        titleLabel.setStyleName("istore-section-title");
        searchBox.setStyleName("istore-search-box");
        searchButtonImage.setStyleName("istore-search-button");

        FlowPanel flowPanel = new FlowPanel();
        flowPanel.add(titleLabel);
        flowPanel.add(searchBox);
        flowPanel.add(searchButtonImage);
        flowPanel.setStyleName("istore-search-panel");
        
        initWidget(flowPanel);

        setInProgress(false);

        // Translate the click event into a search event.
        searchButtonImage.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                setInProgress(true);
                SearchWidget.this.fireEvent(new SearchEvent(SearchWidget.this,
                                                            searchBox.getText()));
            }
        });

        // Translate the enter key into a search event too.
        searchBox.addKeyPressHandler(new KeyPressHandler() {
            public void onKeyPress(KeyPressEvent event) {
                if (event.getCharCode() == 13) {
                    setInProgress(true);
                    SearchWidget.this.fireEvent(new SearchEvent(SearchWidget.this,
                                                                searchBox.getText()));
                }
            }
        });
    }

    public void setInProgress(boolean inProgress) {
        if (inProgress) {
            searchButtonImage.addStyleName("istore-search-button-progress");
            searchBox.addStyleName("istore-search-box-progress");
        } else {
            searchButtonImage.removeStyleName("istore-search-button-progress");
            searchBox.removeStyleName("istore-search-box-progress");
        }
        searchBox.setReadOnly(inProgress);
    }

    public <T> void addSearchHandler(SearchHandler<T> handler) {
        addHandler(handler, SearchEvent.getType());
    }

}
