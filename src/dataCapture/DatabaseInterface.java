/*
 * DatabaseInterface, Interfaces with the database.
 * Peter Fine, June 2008
 */

package dataCapture;

import java.io.File;
import java.util.Vector;

import javax.swing.JOptionPane;

import rde.sqlite3.*;

public class DatabaseInterface {
	
  	private rdeSqlite3DB myDb;
  	String myExpName;
    
  	public DatabaseInterface(String expName) {
  		myExpName = expName;
  		try {
  			myDb = new rdeSqlite3DB();
  			myDb.open("../Experiments/" + myExpName + ".datafile");
  		} catch (Exception e) { e.printStackTrace(); System.exit(1); }
  	}
  	
  	public void defineExperiment(int noBehaviours, Vector<String> trials, Vector<String>behaviours, 
  								 Vector<String>stimuli, Vector<MetaDataItem> extrametadata) {
  		// First check that the database is empty.
  		try {
			if(myDb.getTableList() != null) {
				System.out.println("ERROR: Tables already found before defineExperiment, quitting.");
				System.exit(1);
			}
		} catch (Exception e) {
			e.printStackTrace(); System.exit(1);
		}
  		
  		// Create the metadata table.
  		try {
			myDb.ExecSelect("CREATE TABLE metadata (id INTEGER PRIMARY KEY, item TEXT, value TEXT, type TEXT);");
		} catch (rdeSQLiteException e1) {
			e1.printStackTrace(); System.exit(1);
		}
  		
  		try {
  			
  			myDb.ExecSQL("INSERT INTO metadata (item, value) VALUES ('noBehaviours', " + noBehaviours + ");");
  			
  			for(int i = 0; i < trials.size(); i++) {
  				myDb.ExecSQL("INSERT INTO metadata (item, value) VALUES ('trialName', '" + trials.get(i) + "');");
  			}
  			for(int i = 0; i < behaviours.size(); i++) {
  				myDb.ExecSQL("INSERT INTO metadata (item, value) VALUES ('behaviour', '" + behaviours.get(i) + "');");
  			}
  			for(int i = 0; i < stimuli.size(); i++) {
  				myDb.ExecSQL("INSERT INTO metadata (item, value) VALUES ('stimuli', '" + stimuli.get(i) + "');");
  			}
  			for(int i = 0; i < extrametadata.size(); i++) {
  				myDb.ExecSQL("INSERT INTO metadata (item, value, type) VALUES ('extrametadata', '" +
  							 extrametadata.get(i).itemName() + "', '" + extrametadata.get(i).dataType() + "');");
  			}
  			
  			// Create the experiment's data table.
  			// Prepare the metadata item table names.
  			String extrametadatacolumns = "";
  			for(int i = 0; i < extrametadata.size(); i++) {
  				String datatypeString = "";
  				if(extrametadata.get(i).dataType().compareTo("DATE") == 0) {
  					datatypeString = "TEXT";
  				}
  				else {
  					datatypeString = extrametadata.get(i).dataType();
  				}
  				extrametadatacolumns += extrametadata.get(i).itemName() + " " + datatypeString + ",";
  			}
  			
  			// Create the table.
	  		myDb.ExecSelect("CREATE TABLE data (id INTEGER PRIMARY KEY, " + extrametadatacolumns + 
	  				 		"trialName TEXT, trialStatus TEXT, behaviour TEXT, stimuli TEXT, behaviourNo INTEGER);");
  			
		} catch (Exception e) { 
			e.printStackTrace();
       	 	JOptionPane.showMessageDialog(null,
     			 	"Database error 1, contact author before continuing.", 
     			 	"error", JOptionPane.ERROR_MESSAGE);
			System.exit(1); }
  	}
  	
  	public static Vector<String> getExperiments() {
	  	
  		// List the files in the experiment directory which have a .exp suffix.
  		String[] fileNames = new File("../Experiments/").list();
  		Vector<String> expNames = new Vector<String>();
  		for(int i = 0; i < fileNames.length; i++) {
  			String[] fileNameComponents = fileNames[i].split("\\.");
  			if(fileNameComponents.length == 2 && fileNameComponents[1].equals("datafile")) {
  				expNames.add(fileNameComponents[0]);
  			}
  		}
  		
	 	return expNames;
  	}
  	
  	public Vector<String> findStringMetadata(String itemType, String resultWanted) {
  		Vector<String> metadataResults = new Vector<String>();
  		try {
  			rdeSqlite3Dataset stringsSet = 
		  		myDb.ExecSelect("SELECT id," + resultWanted + " FROM metadata WHERE item = '" + itemType + "' ORDER BY 1;");
		  	
		 	while(!stringsSet.getEOF())
		  	{
		 		metadataResults.add(stringsSet.getFieldAsString(1));
		 		stringsSet.next();
		  	}
		 	stringsSet.close();
	  	} catch (Exception e) { e.printStackTrace(); System.exit(1); }
	  	
	 	return metadataResults;
  	}
  	
  	public Vector<Integer> findIntMetadata(String itemType, String resultWanted) {
  		Vector<Integer> metadataResults = new Vector<Integer>();
  		try {
  			rdeSqlite3Dataset stringsSet = 
		  		myDb.ExecSelect("SELECT id," + resultWanted + " FROM metadata WHERE item = '" + itemType + "' ORDER BY 1;");
		  	
		 	while(!stringsSet.getEOF())
		  	{
		 		metadataResults.add(stringsSet.getFieldAsInteger(1));
		 		stringsSet.next();
		  	}
		 	stringsSet.close();
	  	} catch (Exception e) { e.printStackTrace(); System.exit(1); }
	  	
	 	return metadataResults;
  	}
  	
  	public Vector<Vector<String> > findStringDataEntries(boolean isDistinct, String resultFields, String criteria) {
  		Vector<Vector<String> > result = new Vector<Vector<String> >();
  		String distinctOpt = isDistinct ? " DISTINCT " : " ";
  		
 		try {
  			rdeSqlite3Dataset stringsSet = 
		  		myDb.ExecSelect("SELECT" + distinctOpt + " " + resultFields + "  FROM data " + criteria + ";");
		  
		 	while(!stringsSet.getEOF())
		  	{
		 		Vector<String> resultLine = new Vector<String>();
		 		for(int i = 0; i < stringsSet.getFieldCount(); i++) {
		 			resultLine.add(stringsSet.getFieldAsString(i));
		 		}
		 		result.add(resultLine);
		 		stringsSet.next();
		  	}
		 	stringsSet.close();
	  	} catch (Exception e) { e.printStackTrace(); System.exit(1); }
	  	
	 	return result;
  	}
  	
  	public void addResults(String coderId, String childId, String trialStatus, 
  							Vector<String> extraMetadataNames, Vector<String> extraMetadataValues,
  							Vector<MainDataEntryScreen> dataEntryTabs) {

  		// Prepare variable and value strings for the unchanging metadata.
  		String unchangingVariableString = "coderId, childId, trialStatus, ";
  		String unchangingValueString = "'" + coderId + "', '" + childId + 
  									   "', '" + trialStatus + "', ";
		for(int i = 0; i < extraMetadataNames.size(); i++) {
			unchangingVariableString += extraMetadataNames.get(i) + ", ";
			unchangingValueString += "'" + extraMetadataValues.get(i) + "', ";
		}
  		
  		// now add the data from each trial.
  		String[] trialNames = findStringMetadata("trialName", "value").toArray(new String[0]);
  		for(int i = 0; i < dataEntryTabs.size(); i++) {
  			
  			String trialVariableString = "trialName, ";
  			String trialValueString = "'" + trialNames[i] + "', ";
  			
  			// Get the data for this trial.
  			Vector<String> behaviours = dataEntryTabs.get(i).getBehaviourData();
  			Vector<String> stimuli = dataEntryTabs.get(i).getStimuliData();
  			Vector<Integer> times = dataEntryTabs.get(i).getTimeData();
  			
  			for(int m = 0; m < behaviours.size(); m++) {
  				// XXX SHOULD THE INTEGER DATATYPES BE INSERTED WITHOUT 's?
  				// XXX SHOULD ANY EMPTY ENTRIES BE BANNED?
  				String dataVariableString = "behaviour, stimuli, behaviourNo";
  				String dataValueString = "'" + behaviours.get(m) + "', '" + 
  											   stimuli.get(m) + "', " + times.get(m);
  				
  				// Insert the data into the data table in the database.
  				String SQLString = "INSERT INTO data (" + 
  					unchangingVariableString +  trialVariableString + dataVariableString +
  					") VALUES (" + 
  					unchangingValueString + trialValueString + dataValueString +
  					");";
  				
  				try {
  					myDb.ExecSQL(SQLString);
  				} catch (Exception e) { 
  					e.printStackTrace();
  		       	 	JOptionPane.showMessageDialog(null,
  		     			 	"Database error 2, contact author before continuing.", 
  		     			 	"error", JOptionPane.ERROR_MESSAGE);
  					System.exit(1); 
  				}
  			}
  		}
	}

  	public int deleteData(String criteria) {
  		int count = 0;
  		try {
  	  		// First count the number of fields which will be deleted.
  			count = myDb.ExecSelect("SELECT id from data " + criteria + ";").getFieldCount();
  			System.out.println(count);
  			// Delete the data.
		  	myDb.ExecSQL("DELETE from data " + criteria + ";");
		  	
	  	} catch (Exception e) { e.printStackTrace(); System.exit(1); }
	  	
	 	return count;
  	}
  	
  	public void closeDatabase() {
  		try {
  			myDb.close();
	  	} catch (Exception e) { e.printStackTrace(); System.exit(1); }
  	}
  	
}