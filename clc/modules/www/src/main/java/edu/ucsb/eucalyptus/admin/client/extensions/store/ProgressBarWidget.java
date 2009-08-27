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
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.core.client.GWT;


/** Graphic progress bar widget.  This widget works by gradually
 * overlaying an image which has the progress meter fully completed
 * on top of a base image which has the meter completely empty.
 */
public class ProgressBarWidget extends Composite {

    private final Image doneOverlayImage;
    private final int width;
    private final int height;

    ProgressBarWidget(String baseImageUrl, String doneImageUrl, int width, int height) {
        /* We use two panels here.  The external one is relative, while
         * the internal one is absolute, so that we can overlap the two
         * images inside it.  Having the external panel makes paddings
         * and whatnot work as expected. */
        SimplePanel externalPanel = new SimplePanel();
        AbsolutePanel internalPanel = new AbsolutePanel();

        Image baseImage = new Image(baseImageUrl);

        this.width = width;
        this.height = height;

        doneOverlayImage = new Image(doneImageUrl, 0, 0, 0, height);

        internalPanel.setPixelSize(width, height);
        internalPanel.add(baseImage, 0, 0);
        internalPanel.add(doneOverlayImage, 0, 0);

        externalPanel.setWidget(internalPanel);
        externalPanel.setStyleName("istore-progress-bar-widget");

        initWidget(externalPanel);
    }

    public void setPercentage(int percentage) {
        int visibleWidth = (percentage * width) / 100;
        doneOverlayImage.setVisibleRect(0, 0, visibleWidth, height);
    }

}
