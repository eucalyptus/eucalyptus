package com.eucalyptus.upgrade;


/**
 * Any Class implementing this interface will be registered as a candidate for usage during the upgrade procedure.
 * 
 * @author decker
 */
public abstract class AbstractUpgradeScript implements UpgradeScript, Comparable<UpgradeScript> {
	protected int priority;
	
	protected AbstractUpgradeScript(int priority) {
		this.priority = priority;
	}
	
	public int getPriority() {
		return priority;
	}
	
    public int compareTo(UpgradeScript o) {
        if(o.getPriority() == priority)
            return 0 ;
        if(o.getPriority() < priority)
            return 1;
        else
            return -1;
    }
}