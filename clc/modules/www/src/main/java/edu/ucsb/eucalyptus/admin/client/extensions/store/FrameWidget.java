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
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;


public class FrameWidget extends Composite {

    private final String FRAME_TOP_URI = GraphicsUtil.uri("frame-top.png");
    private final String FRAME_LEFT_URI = GraphicsUtil.uri("frame-left.png");
    private final String FRAME_BOTTOM_URI = GraphicsUtil.uri("frame-bottom.png");

    public FrameWidget(Widget content) {
        FlexTable table = new FlexTable();
        table.setWidget(0, 0, new Image(FRAME_TOP_URI));
        table.setWidget(1, 0, new Image(FRAME_LEFT_URI));
        table.setWidget(2, 0, new Image(FRAME_BOTTOM_URI));
        table.setWidget(1, 1, content);
        table.setStyleName("istore-frame-widget");
        table.setCellSpacing(0);
        table.setCellPadding(0);
        FlexTable.FlexCellFormatter formatter = table.getFlexCellFormatter();
        formatter.setColSpan(0, 0, 2);
        formatter.setColSpan(2, 0, 2);
        formatter.setStyleName(0, 0, "istore-top-frame-image");
        formatter.setStyleName(1, 0, "istore-left-frame-image");
        formatter.setStyleName(2, 0, "istore-bottom-frame-image");
        formatter.getElement(1, 0).getStyle().setProperty("background", "url(" + FRAME_LEFT_URI + ") repeat-y");
        initWidget(table);
    }

}
