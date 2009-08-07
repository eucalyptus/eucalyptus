package edu.ucsb.eucalyptus.admin.client;
import com.google.gwt.user.client.ui.*;

public class EucalyptusKeyboardListener extends KeyboardListenerAdapter {
	
	private Button enterButton = null;
	private Button escapeButton = null;
	
	EucalyptusKeyboardListener (final Button enterButton) 
	{
		this.enterButton = enterButton;
	} 

	EucalyptusKeyboardListener (final Button enterButton, final Button escapeButton) 
	{
		this.enterButton = enterButton;
		this.escapeButton = escapeButton;
	}
			
	public void onKeyPress (Widget sender, char key, int mods) {
		if (KeyboardListener.KEY_ENTER == key && enterButton != null)
			enterButton.click();
		if (KeyboardListener.KEY_ESCAPE == key && escapeButton != null)
			escapeButton.click();
	}
}
