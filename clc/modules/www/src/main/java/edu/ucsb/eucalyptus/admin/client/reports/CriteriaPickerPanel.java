package edu.ucsb.eucalyptus.admin.client.reports;

import edu.ucsb.eucalyptus.admin.client.AccountingControl;
import edu.ucsb.eucalyptus.admin.client.util.Observer;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.*;

/**
 * <P>CriteriaPickerPanel is a vertical panel that contains drop-downs for
 * selecting a report criterion and a "group by" criterion.
 */
public class CriteriaPickerPanel
	extends VerticalPanel
	implements Observer, ChangeHandler
{
	private final AccountingControl accountingControl;

	private final HorizontalPanel outerCriterionPanel;
	private final HorizontalPanel innerCriterionPanel;
	private final ListBox outerCriterionBox;
	private final ListBox innerCriterionBox;
	
	public CriteriaPickerPanel( final AccountingControl accountingControl )
	{
		this.accountingControl = accountingControl;

		this.outerCriterionBox = new ListBox();
		this.outerCriterionBox.setStyleName("acct-Button-Report");
		this.outerCriterionBox.setVisibleItemCount(1);
		this.outerCriterionBox.addItem("-(none)-");
		/* It's obviously desirable for this to use InstanceUsageLog.GroupByCriterion
		 * to generate these drop-down values, but that would be complicated because
		 * this resides in the browser.
		 */
		this.outerCriterionBox.addItem("Availability Zone");
		this.outerCriterionBox.addItem("Cluster");
		this.outerCriterionBox.addItem("Account");
		this.outerCriterionBox.setStyleName("acct-HTML");
		this.outerCriterionBox.addChangeHandler(this);
		this.outerCriterionPanel = new HorizontalPanel();
		final Label groupLabel = new Label("Group By: ");
		groupLabel.setStyleName("acct-HTML");
		//this.outerCriterionPanel.setStyleName("acct-HTML");
		this.outerCriterionPanel.add(groupLabel);
		this.outerCriterionPanel.add(outerCriterionBox);
		this.add(outerCriterionPanel);
		
		this.innerCriterionBox = new ListBox();
		this.innerCriterionBox.setStyleName("acct-Button-Report");
		this.innerCriterionBox.setVisibleItemCount(1);
		this.innerCriterionBox.addItem("Cluster");
		this.innerCriterionBox.addItem("Account");
		this.innerCriterionBox.addItem("User");
		this.innerCriterionBox.addChangeHandler(this);
		this.innerCriterionBox.setStyleName("acct-HTML");
		this.innerCriterionPanel = new HorizontalPanel();
		final Label criterionLabel = new Label("Criterion: ");
		criterionLabel.setStyleName("acct-HTML");
		this.innerCriterionPanel.add(criterionLabel);
		this.innerCriterionPanel.add(innerCriterionBox);
		//this.innerCriterionPanel.setStyleName("acct-HTML");
		this.add(innerCriterionPanel);
	}

	@Override
	public void redraw()
	{
	}

	@Override
	public void update()
	{
		
	}

	@Override
	public void onChange(ChangeEvent arg0)
	{
		int outerInd = this.outerCriterionBox.getSelectedIndex();
		int innerInd = this.innerCriterionBox.getSelectedIndex();

		/* Do not allow the user to select a criterion at a greater level
		 * of detail as the "group by" criterion, as it makes no sense.
		 */
		if ((outerInd-2) >= innerInd) {
			this.innerCriterionBox.setSelectedIndex(outerInd - 1);
			innerInd = outerInd - 1;
		}
		this.accountingControl.setCriterionInd(new Integer(innerInd));
		this.accountingControl.setGroupByInd(new Integer(outerInd));
		this.redraw();
	}

}
