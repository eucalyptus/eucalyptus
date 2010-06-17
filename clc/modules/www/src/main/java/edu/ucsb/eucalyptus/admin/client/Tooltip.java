package edu.ucsb.eucalyptus.admin.client;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.PopupPanel;

/**
 * A class showing the tooltip using a PopupPanel.
 */
public class Tooltip extends PopupPanel {
	
	public static final int TOOLTIP_DELAY_IN_MILLIS = 800;
		
	private static final String MAIN_STYLE_NAME = "euca-Tooltip";
	
	private Timer timer;
	// A singleton tooltip object
	private static Tooltip tooltip = null;
	
	public static Tooltip getInstance() {
		if (tooltip == null) {
			tooltip = new Tooltip();
		}
		return tooltip;
	}
	
	private Tooltip() {
		super();
		
		this.addStyleName(MAIN_STYLE_NAME);
		this.timer = new Timer() {
			public void run() {
				Tooltip.this.show();
			}
		};
	}
	
	/**
	 * Show the tooltip after a delay.
	 * @param x The x coordinate of the tooltip
	 * @param y The y coordinate of the tooltip
	 * @param delayInMillis The delay in milliseconds
	 * @param html The tooltip contents
	 */
	public void delayedShow(int x, int y, int delayInMillis, String html) {
		this.hide();
		this.timer.cancel();
		this.setPopupPosition(x, y);
		this.setWidget(new HTML(html));
		if (delayInMillis > 0) {
			timer.schedule(delayInMillis);
		} else {
			Tooltip.this.show();
		}
	}
	
	public void hide() {
		super.hide();
		this.timer.cancel();
	}
	
	public void hide(boolean autoClosed) {
		super.hide(autoClosed);
		this.timer.cancel();
	}
}
