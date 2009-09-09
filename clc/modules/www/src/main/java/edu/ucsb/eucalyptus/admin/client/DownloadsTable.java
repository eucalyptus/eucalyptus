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
*    Software License Agreement (BSD License)
* 
*    Copyright (c) 2008, Regents of the University of California
*    All rights reserved.
* 
*    Redistribution and use of this software in source and binary forms, with
*    or without modification, are permitted provided that the following
*    conditions are met:
* 
*      Redistributions of source code must retain the above copyright notice,
*      this list of conditions and the following disclaimer.
* 
*      Redistributions in binary form must reproduce the above copyright
*      notice, this list of conditions and the following disclaimer in the
*      documentation and/or other materials provided with the distribution.
* 
*    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
*    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
*    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
*    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
*    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
*    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
*    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
*    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
*    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
*    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
*    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
*    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
*    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
*******************************************************************************/
package edu.ucsb.eucalyptus.admin.client;

import com.google.gwt.user.client.ui.*;

import java.util.List;
import java.util.ArrayList;

import com.google.gwt.user.client.rpc.AsyncCallback;

// dmitrii TODO: remove commented out lines once the CSS-based design is confirmed

public class DownloadsTable extends VerticalPanel {
    private String theUrl;
    private String theHumanUrl;
    private String theName;
    private int maxEntries;
    private Label DownloadsHeader = new Label();
    private HTML statusLabel = new HTML();
    private Grid grid = new Grid();
    private List<DownloadsWeb> DownloadsList = new ArrayList<DownloadsWeb>();
    private String sessionId;

    public DownloadsTable(String sessionId, String theUrl, String theHumanUrl, String theName, int maxEntries) {
        this.sessionId = sessionId;
		this.setStyleName("euca-config-component");
        this.theUrl = theUrl;
        this.theHumanUrl = theHumanUrl;
        this.theName = theName;
        this.maxEntries = maxEntries;
        this.setSpacing(10);
        //this.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        //this.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        this.DownloadsHeader.setText(this.theName);
        this.DownloadsHeader.setStyleName("euca-section-header");
        this.add(DownloadsHeader);
        this.add(this.grid);
        HorizontalPanel hpanel = new HorizontalPanel();
        hpanel.setSpacing(5);
        hpanel.add(this.statusLabel);
//        this.statusLabel.setWidth("600");
        this.statusLabel.setText("Contacting " + this.theName + " server");
        this.statusLabel.setStyleName("euca-greeting-pending");
        this.add(hpanel);

        EucalyptusWebBackend.App.getInstance().getDownloads(this.sessionId, this.theUrl, new GetCallback(this));

    }

    private void rebuildTable() {
        int rows = this.DownloadsList.size();
        if (rows == 0) {
            this.statusLabel.setHTML("Failed to load the list of images (<a href=\"" + this.theHumanUrl + "\">visit</a> the repository)");
            this.statusLabel.setStyleName("euca-greeting-error");
            this.statusLabel.setVisible(true);
            return;
        }
        // draw the table
        this.statusLabel.setVisible(false);
        if (rows>this.maxEntries) {
            rows = this.maxEntries;
        }
        this.grid.clear();
        this.grid.resize(rows+1, 2); // +1 because of header
        this.grid.setVisible(true);
        this.grid.setStyleName("euca-table");
        this.grid.setCellPadding(6);
        this.grid.setWidget(0, 0, new Label("Name"));
        this.grid.setWidget(0, 1, new Label("Description"));
        this.grid.getRowFormatter().setStyleName(0, "euca-table-heading-row");
        for (int row = 0; row<rows; row++) {
            addDownloadsEntry(row+1, this.DownloadsList.get(row)); // +1 because of header
        }
    }

    private void addDownloadsEntry(int row, DownloadsWeb Downloads) {
        if ((row % 2) == 1) {
            this.grid.getRowFormatter().setStyleName(row, "euca-table-odd-row");
        } else {
            this.grid.getRowFormatter().setStyleName(row, "euca-table-even-row");
        }

        final HTML name_b = new HTML("<a href=\"" + Downloads.getUrl() + "\">" + Downloads.getName() + "</a>");
        this.grid.setWidget(row, 0, name_b);

        final Label description_b = new Label();
        description_b.setText(Downloads.getDescription());
        this.grid.setWidget(row, 1, description_b);
    }

    public List<DownloadsWeb> getDownloadsList() {
        return DownloadsList;
    }

    class GetCallback implements AsyncCallback {

        private DownloadsTable parent;

        GetCallback(final DownloadsTable parent) {
            this.parent = parent;
        }

        public void onFailure(final Throwable throwable) {
            this.parent.statusLabel.setHTML("Failed to reach your server (<a href=\"" + this.parent.theHumanUrl + "\">visit</a> the repository)");
            this.parent.statusLabel.setStyleName("euca-greeting-error");
            this.parent.statusLabel.setVisible(true);
        }

        public void onSuccess(final Object o) {
            List<DownloadsWeb> newDownloadsList = (List<DownloadsWeb>) o;
            this.parent.DownloadsList = newDownloadsList;
            this.parent.rebuildTable();
        }

    }
}
