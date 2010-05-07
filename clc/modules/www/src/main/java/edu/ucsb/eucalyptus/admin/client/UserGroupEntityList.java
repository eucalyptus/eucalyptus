package edu.ucsb.eucalyptus.admin.client;

import edu.ucsb.eucalyptus.admin.client.UserGroupEntityPanel.SelectionChangeHandler;

import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A list shows entities including users and groups.
 * 
 * With the following components:
 * 1. A grid with each row shows (each in a different column)
 *   a. A checkbox indicating the selection of an item
 *   b. A primary name of the item (the main column)
 *   c. One or more properties of the item (in grey color as a hint)
 * 2. A floating tooltip shows the details of each item.
 * 
 * Behavior:
 * 1. Multiple rows can be selected via the checkbox
 * 2. MouseOver shows highlighted row.
 * 3. A single click selects current row and deselect previous selects.
 */
public class UserGroupEntityList extends Grid {
	
	/**
	 * A data row with multiple columns, which are accessed by index.
	 */
	public interface DataRow {
		public String get(int column);
		public String getStyle(int column);
		public String toTooltip();
	}
	
	public static final String CELL_MAIN_STYLE_NAME = "euca-UserGroupEntityList-main";
	public static final String CELL_OTHER_STYLE_NAME = "euca-UserGroupEntityList-other";
	public static final String CELL_MAIN_DISABLED_STYLE_NAME = "euca-UserGroupEntityList-main-disabled";
	public static final String CELL_MAIN_ADMIN_STYLE_NAME = "euca-UserGroupEntityList-main-admin";
	public static final String CELL_MAIN_SPECIAL_STYLE_NAME = "euca-UserGroupEntityList-main-special";

	// Style names
	private static final String GLOBAL_STYLE_NAME = "euca-UserGroupEntityList";
	private static final String ROW_ROLLOVER_STYLE_NAME = "euca-UserGroupEntityList-rollover";
	private static final String ROW_SELECTED_STYLE_NAME = "euca-UserGroupEntityList-selected";
	
	// The names of the columns 
	private List<String> columnNames;
	
	private final List<CheckBox> checkboxes = new ArrayList<CheckBox>();
	
	private final Set<Integer> selected = new HashSet<Integer>();
	
	private List<DataRow> data;
	
	private UserGroupEntityPanel parent;
	private UserGroupControl control;
	
	UserGroupEntityList(UserGroupEntityPanel parent, UserGroupControl control, List<String> cols) {
		super();
		this.parent = parent;
		this.control = control;
		this.columnNames = cols;
		this.data = null;
		setEventHandlers();
	}
	
	public void refresh(List<DataRow> data) {
		cleanup();
		
		this.data = data;
		// column = number of data columns + 1 (checkbox column)
		resize(data.size(), columnNames.size() + 1);
		setLayout();
		checkboxes.clear();
		selected.clear();
		drawData();
	}
	
	private void cleanup() {
		this.resizeRows(0);
	}
	
	/**
	 * Calculate column width (in percentage) for the list. Assume the main column
	 * is 1.4 times of width of other columns (except the checkbox column, which uses
	 * whatever width it requires). This returns the width (in percentage) of non-main
	 * columns.
	 * @param colSize Total number of columns (minus the checkbox column)
	 * @return The width of the non-main columns.
	 */
	public static String getColumnWidth(int colSize) {
		int colWidthInPercent = (int)(95.0 / ((double)colSize + 0.5));
		return colWidthInPercent + "%";
	}
	
	protected void setLayout() {
		// No padding and spacing
		this.setCellPadding(1);
		this.setCellSpacing(0);
		// Set minimal column widths for the checkbox column and
		// the non-main columns.
		ColumnFormatter colFormatter = this.getColumnFormatter();
		colFormatter.setWidth(0, "10px");
		String width = getColumnWidth(columnNames.size());
		for (int i = 2; i < columnNames.size() + 1; i++) {
			colFormatter.setWidth(i, width);
		}
		// Set overall style
		this.addStyleName(GLOBAL_STYLE_NAME);
	}
	
	private void addRowStyle(int row, String style) {
		CellFormatter formatter = getCellFormatter();
		for (int i = 0; i < columnNames.size() + 1; i++) {
			formatter.addStyleName(row, i, style);
		}
	}
	
	private void removeRowStyle(int row, String style) {
		CellFormatter formatter = getCellFormatter();
		for (int i = 0; i < columnNames.size() + 1; i++) {
			formatter.removeStyleName(row, i, style);
		}
	}
	
	/**
	 * Add a selected row.
	 * @param row The row index
	 */
	private void addSelected(int row) {
		if (!selected.contains(row)) {
			selected.add(row);
			setRowStatus(row, true);
		}
	}
	
	/**
	 * Remove a row from selected set.
	 * @param row
	 */
	private void removeSelected(int row) {
		if (selected.contains(row)) {
			selected.remove(row);
			setRowStatus(row, false);
		}
	}
	
	/**
	 * Set a selected row. This means clearing the selected set
	 * and add the current selected row.
	 * @param row The selected row to set.
	 */
	public void setSelected(int row) {
		if (selected.contains(row)) {
			selected.remove(row);
		} else {
			setRowStatus(row, true);
		}
		// Clear all other selected
		for (int r: selected) {
			setRowStatus(r, false);
		}
		selected.clear();
		selected.add(row);
	}
	
	private void setRowStatus(int row, boolean selected) {
		CheckBox checkbox = checkboxes.get(row);
		if (checkbox.getValue() != selected) {
			checkbox.setValue(selected);
		}
		if (selected) {
			addRowStyle(row, ROW_SELECTED_STYLE_NAME);
		} else {
			removeRowStyle(row, ROW_SELECTED_STYLE_NAME);
		}
	}
	
	protected void setEventHandlers() {
		sinkEvents(Event.ONMOUSEOVER | Event.ONMOUSEOUT | Event.ONMOUSEMOVE);
		this.addHandler(new MouseOutHandler() {
			public void onMouseOut(MouseOutEvent event) {
				int row = getRowForEvent(event);
				if (row >= 0) {
					removeRowStyle(row, ROW_ROLLOVER_STYLE_NAME);
					Tooltip.getInstance().hide();
				}
			}
		}, MouseOutEvent.getType());
		this.addHandler(new MouseOverHandler() {
			public void onMouseOver(MouseOverEvent event) {
				int row = getRowForEvent(event);
				if (row >= 0) {
					addRowStyle(row, ROW_ROLLOVER_STYLE_NAME);
				}
			}
		}, MouseOverEvent.getType());
		this.addHandler(new MouseMoveHandler() {
			public void onMouseMove(MouseMoveEvent event) {
				int row = getRowForEvent(event);
				if (row >= 0) {
					int x = ((Widget)event.getSource()).getAbsoluteLeft() + event.getX() + 15;
					int y = ((Widget)event.getSource()).getAbsoluteTop() + event.getY() + 15;
					Tooltip.getInstance().delayedShow(x, y, Tooltip.TOOLTIP_DELAY_IN_MILLIS,
							data.get(row).toTooltip());
				}
			}
		}, MouseMoveEvent.getType());
		this.addClickHandler(new ClickHandler(){
			public void onClick(ClickEvent event) {
				Cell cell = getCellForEvent(event);
				if (cell != null) {
					int row = cell.getRowIndex();
					int col = cell.getCellIndex();
					if (row >= 0 && col > 0) {
						setSelected(row);
						control.onSelectionChange(parent);
					}
				}
			}
		});
	}
	
	protected int getRowForEvent(DomEvent event) {
		Element td = getEventTargetCell(Event.as(event.getNativeEvent()));
		if (td == null) {
			return -1;
		}
		int row = TableRowElement.as(td.getParentElement()).getSectionRowIndex();
		return row;
	}
	
	/**
	 * Override the onBrowserEvent() to make sure we get the necessary
	 * ONMOUSEOVER and ONMOUSEOUT events.
	 */
	public void onBrowserEvent(Event event) {
		DomEvent.fireNativeEvent(event, this, this.getElement());
	}
	
	private void drawRow(final int row, DataRow text) {
		CheckBox checkbox = new CheckBox();
		checkbox.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
			public void onValueChange(ValueChangeEvent<Boolean> event) {
				if (event.getValue()) {
					addSelected(row);
				} else {
					removeSelected(row);
				}
				control.onSelectionChange(parent);
			}
		});
		checkboxes.add(checkbox);
		this.setWidget(row, 0, checkbox);
		CellFormatter formatter = getCellFormatter();
		for (int i = 0; i < columnNames.size(); i++) {
			int col = i + 1;
			this.setText(row, col, text.get(i));
			formatter.addStyleName(row, col, text.getStyle(i));
		}
	}
	
	protected void drawData() {
		for (int i = 0; i < data.size(); i++) {
			drawRow(i, data.get(i));
		}
	}
	
	public void selectAll() {
		for (int i = 0; i < data.size(); i++) {
			addSelected(i);
		}
	}
	
	public void selectNone() {
		for (int i : selected) {
			setRowStatus(i, false);
		}
		selected.clear();
	}
	
	public List<DataRow> getSelected() {
		List<DataRow> result = new ArrayList<DataRow>();
		if (data != null) {
			for (int row : selected) {
				result.add(data.get(row));
			}
		}
		return result;
	}
}
