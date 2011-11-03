/*
 * MetaDataItem. The definition and contents of an item of metadata about
 * an experiment.
 
 * Peter Fine, May 2008
 */ 

package dataCapture;

import java.util.Date;;

public class MetaDataItem {

	private String myItemName;
	private String myDataType;
	private boolean myCanBeDeleted;

    public MetaDataItem(String itemName, String dataType, boolean canBeDeleted) {
    	myItemName = itemName;
    	myDataType = dataType;
    	myCanBeDeleted = canBeDeleted;
    }
    
    public boolean canBeDeleted() {
    	return myCanBeDeleted;
    }
    
    public String toString() {
    	return myItemName + " (" + myDataType + ")";
    }
    
    public String itemName() {
    	return myItemName;
    }
    
    public String dataType() {
    	return myDataType;
    }
}
