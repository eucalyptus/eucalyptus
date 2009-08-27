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
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.SimplePanel;


class ImageSectionWidget extends Composite {

    private final String SEPARATOR_URI = GraphicsUtil.uri("image-separator.png");

    private VerticalPanel verticalPanel = new VerticalPanel();
    private VerticalPanel imagesPanel = new VerticalPanel();
    private boolean firstImageWidget = true;

    public ImageSectionWidget(ImageSection imageSection) {
        if (imageSection.getTitle() != null) {
            Label titleLabel = new Label(imageSection.getTitle());
            titleLabel.setStyleName("istore-section-title");
            verticalPanel.add(titleLabel);
        }
        if (imageSection.getSummary() != null) {
            Label summaryLabel = new Label(imageSection.getSummary());
            summaryLabel.setStyleName("istore-section-summary");
            verticalPanel.add(summaryLabel);
        }
        initWidget(verticalPanel);
        setStyleName("istore-image-section-widget");
        imagesPanel.setStyleName("istore-images-panel");
    }

    public void addImageWidget(ImageWidget imageWidget) {
        if (firstImageWidget) {
            firstImageWidget = false;
            verticalPanel.add(new FrameWidget(imagesPanel));
        } else {
            Image separator = new Image(SEPARATOR_URI);
            SimplePanel separatorPanel = new SimplePanel();
            separatorPanel.setWidget(separator);
            imagesPanel.add(separatorPanel);
            separatorPanel.setStyleName("istore-image-separator");
        }
        imagesPanel.add(imageWidget);
    }

}
