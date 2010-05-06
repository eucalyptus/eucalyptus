package edu.ucsb.eucalyptus.admin.client;

import edu.ucsb.eucalyptus.admin.client.UserGroupEntityList.DataRow;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.HTMLTable.ColumnFormatter;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A panel display the user or group list, including:
 * 1. A header that shows the status of the list;
 * 2. A filter box that is used to filter list content based on the text
 *    on all the columns of the list;
 * 3. A list header shows the column names;
 * 4. The actual list (extends Grid, see UserGroupListPanel);
 */
public class UserGroupEntityPanel extends VerticalPanel {
	
	public interface SelectionChangeHandler {
		public void onSelectionChange();
	}
	
	private class ColumnComparator implements Comparator<DataRow> {
		private int col;
		private boolean inc;
		ColumnComparator(int col, boolean inc) {
			this.col = col;
			this.inc = inc;
		}
		public int compare(DataRow d1, DataRow d2) {
			if (inc) {
				return d1.get(col).compareToIgnoreCase(d2.get(col));
			} else {
				return d2.get(col).compareToIgnoreCase(d1.get(col));
			}
		}
	}
	
	public static final String DEFAULT_GROUP_HEADER = "Groups";
	public static final String DERAULT_USER_HEADER = "Users";
	
	private static final String HEADER_STYLE_NAME = "euca-UserGroupTabPanel-header";
	private static final String ACTION_BAR_STYLE_NAME = "euca-UserGroupEntityPanel-action";
	private static final String FILTER_STYLE_NAME = "euca-UserGroupEntityPanel-filter";
	private static final String SCROLL_STYLE_NAME = "euca-UserGroupEntityPanel-scroll";
	private static final String LIST_HEADER_STYLE_NAME = "euca-UserGroupEntityPanel-list-header";
	private static final String ANCHOR_STYLE_NAME = "euca-UserGroupEntityPanel-anchor";
	private static final String MAIN_STYLE_NAME = "euca-UserGroupEntityPanel";
	
	private UserGroupEntityList list;
	private Label header;
	private TextBox filter;
	
	private List<String> columns;
	private List<DataRow> data;
	
	private String filterString;
	private final List<DataRow> filteredData =
			new ArrayList<DataRow>();
	
	private int sortColumn;
	private boolean sortInc;
	
	private UserGroupControl control;
	
	UserGroupEntityPanel(UserGroupControl control, List<String> cols, ClickHandler addHandler) {
		super();
		
		this.control = control;
		this.columns = cols;
		
		list = new UserGroupEntityList(this, control, cols);
		ScrollPanel scroll = new ScrollPanel(list);
		scroll.setStyleName(SCROLL_STYLE_NAME);
		
		header = new Label();
		header.addStyleName(HEADER_STYLE_NAME);
		
		Widget actionBar = createActionBar(addHandler);
		Widget listHeader = createListHeader(cols);
		
		// Order matters
		this.add(header);
		this.add(actionBar);
		this.add(listHeader);
		this.add(scroll);

		// Tightening the header space
		this.setCellHeight(header, "20px");
		this.setCellHeight(actionBar, "32px");
		this.setCellHeight(listHeader, "20px");

		this.setSpacing(0);
		this.addStyleName(MAIN_STYLE_NAME);
	}
	
	public void setHeaderText(String title) {
		this.header.setText(title);
	}
	
	public UserGroupEntityList getList() {
		return this.list;
	}
	
	protected Widget createListHeader(List<String> cols) {
		int colSize = cols.size();
		Grid headerGrid = new Grid(1, colSize + 1);
		headerGrid.addStyleName(LIST_HEADER_STYLE_NAME);
		headerGrid.setCellPadding(1);
		headerGrid.setCellSpacing(0);
		ColumnFormatter colFormatter = headerGrid.getColumnFormatter();
		colFormatter.setWidth(0, "10px");
		String width = UserGroupEntityList.getColumnWidth(colSize);
		for (int i = 2; i < colSize + 1; i++) {
			colFormatter.setWidth(i, width);
		}
		CheckBox checkbox = new CheckBox();
		checkbox.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
			public void onValueChange(ValueChangeEvent<Boolean> event) {
				if (event.getValue()) {
					UserGroupEntityPanel.this.list.selectAll();
					CheckBox source = (CheckBox) event.getSource();
					source.setValue(false);
					control.onSelectionChange(UserGroupEntityPanel.this);
				}
			}
		});
		headerGrid.setWidget(0, 0, checkbox);
		for (int i = 0; i < colSize; i++) {
			final int col = i;
			Anchor anchor = new Anchor(cols.get(i));
			anchor.addStyleName(ANCHOR_STYLE_NAME);
			anchor.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					sortByColumn(col);
				}
			});
			headerGrid.setWidget(0, col + 1, anchor);
		}
		return headerGrid;
	}
	
	protected Widget createActionBar(ClickHandler handler) {
		HorizontalPanel actionBar = new HorizontalPanel();
		actionBar.addStyleName(ACTION_BAR_STYLE_NAME);
		actionBar.setHorizontalAlignment(ALIGN_CENTER);
		actionBar.setVerticalAlignment(ALIGN_MIDDLE);

		filter = new TextBox();
		filter.addKeyPressHandler(new KeyPressHandler() {
			public void onKeyPress(KeyPressEvent event) {
				if (event.getCharCode() == KeyCodes.KEY_ENTER) {
					filterBy(filter.getText());
					showFiltered();
				}
			}
		});
		filter.addStyleName(FILTER_STYLE_NAME);
		
		Button addButton = new Button("Add", handler);
		Button filterButton = new Button("Filter", new ClickHandler() {
			public void onClick(ClickEvent event) {
				filterBy(filter.getText());
				showFiltered();
			}
		});
		
		actionBar.add(filter);
		actionBar.add(filterButton);
		actionBar.add(addButton);
		
		actionBar.setCellWidth(filterButton, "15px");
		actionBar.setCellWidth(addButton, "15px");
		
		return actionBar;
	}
	
	protected void sortByColumn(int column) {
		if (column == sortColumn) {
			sortInc = !sortInc;
		} else {
			sortInc = true;
		}
		sortColumn = column;
		Collections.sort(filteredData, new ColumnComparator(sortColumn, sortInc));
		list.refresh(filteredData);
	}
	
	private void showFiltered() {
		sortColumn = 0;
		sortInc = true;
		Collections.sort(filteredData, new ColumnComparator(sortColumn, sortInc));
		list.refresh(filteredData);
	}
	
	public void display(List<DataRow> data) {
		this.data = data;
		this.filter.setText("");
		this.filterString = "";
		filteredData.clear();
		filteredData.addAll(data);
		showFiltered();
	}
	
	protected void filterBy(String filterString) {
		if (filterString == null) {
			filterString = "";
		} else {
			filterString = filterString.toLowerCase();
		}
		if (this.filterString != null && !this.filterString.equalsIgnoreCase(filterString)) {
			this.filterString = filterString;
			filteredData.clear();
			if ("".equals(filterString)) {
				filteredData.addAll(data);
			} else {
				for (DataRow row : data) {
					for (int i = 0; i < columns.size(); i++) {
						if (row.get(i).toLowerCase().contains(filterString)) {
							filteredData.add(row);
						}
					}
				}
			}
		}
	}
}
