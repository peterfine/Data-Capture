/*
 * DataEntryGui, The gui which allows data to be entered.
 * Peter Fine, May 2008
 */

package dataCapture;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.text.NumberFormat;
import java.util.Vector;
import java.util.Date;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;

import dataCapture.EditableStringList.removeEntryListener;

public class DataEntryGui extends JPanel {

	private DatabaseInterface myDb;
	private String myExpName;
	private JFrame myFrame;
		
	private Vector<Component> myExtraMetadataComponents;
	private Vector<String> myExtraMetadataNames;
	private Vector<String> myExtraMetadataTypes;
	private Vector<String> myExtraMetadataValues;
	private String myCoderId;
	private String myChildId;
	private String myStatus;
	
	private ButtonGroup myStatusButtonGroup;
	private JRadioButton myPracticeButton;
	private JRadioButton myReliabilityButton;
	private JRadioButton myFinalButton;
	private JButton myEnterDataButton;
	private boolean myReliaibilitiesAlreadyEnteredForChild;
	private Vector<MainDataEntryScreen> myDataEntryTabs;
	
    public DataEntryGui(JFrame frame) {
        super();
        myFrame = frame;
        myExtraMetadataValues = new Vector<String>();
    }
    // SELECT EXPERIMENT -------------------------------------------------------------------------------------

    public void constructExperimentSelectorScreen() {
    	setLayout(new BoxLayout(this, FlowLayout.CENTER));

    	// Allow the user to choose the experiment.
    	setTitledBorder(this, "Select an experiment:", 30, 10);
    	String[] experimentNames = DatabaseInterface.getExperiments().toArray(new String[0]);
    	JComboBox experimentSelector = new JComboBox(experimentNames);
    	JButton experimentSelectorButton = new JButton("Begin entering data");
       // experimentSelectorButton.setActionCommand(removeEntryString);
        experimentSelectorButton.addActionListener(new experimentSelectorListener(this, experimentSelector));
     //   add(experimentSelectorLabel);
        add(experimentSelector);
        add(experimentSelectorButton);
        
        if(experimentNames.length == 0) {
        	 JOptionPane.showMessageDialog(this, 
        			 	"No experiments have been defined, the program will now close.", 
        			 	"error", JOptionPane.ERROR_MESSAGE);
        	 System.exit(1);
        	
        }
    }

    class experimentSelectorListener implements ActionListener {
    	
    	private DataEntryGui myGui;
    	private JComboBox myNamesBox;
    	public experimentSelectorListener(DataEntryGui gui, JComboBox namesBox) {
    		myGui = gui; 
    		myNamesBox = namesBox;
    	}
    	
        public void actionPerformed(ActionEvent e) {
        	myGui.constructCoderIdScreen(myNamesBox.getSelectedItem().toString());
        }
    }
    // CODER -------------------------------------------------------------------------------------
    /**
     * The experiment has been selected, construct the metadata gui.
     */
    public void constructCoderIdScreen(String expName) {
    	myExpName = expName;
    	myDb = new DatabaseInterface(expName); // Load the database.
    	
    	clearComponents();
    	
    	// Choose the coderId.
    	Vector<String> existingCoders = getColumnFromVecOfStrVectors(myDb.findStringDataEntries(true, "coderId", ""), 0);
    	JComboBox coderBox = new JComboBox(existingCoders.toArray(new String[0]));
    	coderBox.setSelectedIndex(-1);
    	coderBox.addActionListener(new CoderSelectionListener());
    	JTextField coderIdField = new JTextField(10);
    	coderIdField.setMaximumSize(new Dimension(500,35));
    	JButton newCoderButton = new JButton("New Coder");
    	newCoderButton.addActionListener(new NewCoderButtonListener(existingCoders, coderIdField));
    	
    	if(existingCoders.size() > 0) {
    		add(new JLabel("Choose an existing coder name:"));
    		add(coderBox);
    	}
    	add(new JLabel("Add a new coder name:"));
    	add(coderIdField);
    	add(newCoderButton);
    }
    
    class CoderSelectionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
        	// Check that the selected coder was intended, then construct the metadata gui.
        	String coderId = (String)((JComboBox)e.getSource()).getSelectedItem();
           	int isOk = JOptionPane.showConfirmDialog((JComboBox)e.getSource(), "You have chosen the name '" + coderId + "', is that correct?",
    				"Confirm Coder Name",
    				JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
        	if(isOk != 0) {
        		((JComboBox)e.getSource()).setSelectedIndex(-1);
        		return;
        	}
        	
        	myCoderId = coderId;
        	constructMetadataGui();
        }
    }
    
    class NewCoderButtonListener implements ActionListener {
    	
    	private Vector<String> myExistingCoders;
    	private JTextField myCoderIdField;
    	
    	public NewCoderButtonListener(Vector<String> existingCoders, JTextField coderIdField) {
    		myExistingCoders = existingCoders;
    		myCoderIdField = coderIdField;
    	}
    	
        public void actionPerformed(ActionEvent e) {
        	// Check that a unique coderID has been entered.
        	String coderId = myCoderIdField.getText();
        	if(coderId.length() == 0 || !(coderId.matches("\\p{Alnum}+"))) {
          		JOptionPane.showMessageDialog((JComponent)e.getSource(), "Please enter a name. It must not contain spaces or symbols.", 
      										  "Error", JOptionPane.ERROR_MESSAGE);
          		return;
        	}
        	
        	for(int i = 0; i < myExistingCoders.size(); i++) {
        		if(coderId.equalsIgnoreCase(myExistingCoders.get(i))) {
              		JOptionPane.showMessageDialog((JComponent)e.getSource(), 
              				  "That coder already exists. Please select it from the list above.", 
							  "Error", JOptionPane.ERROR_MESSAGE);
              		return;        			
        		}
        	}
        	
        	// A new coder has been specified. Check it's correct, then Load the child selection / metadata entry interface.
        	int isOk = JOptionPane.showConfirmDialog((JComponent)e.getSource(),
        			"You have chosen the coderId '" + coderId + "', is that correct?",
    				"Confirm Coder Name",
    				JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
        	if(isOk != 0) {
        		return;
        	}
        	
        	myCoderId = coderId;
        	constructMetadataGui();
        }	
    }
    	
    public void constructMetadataGui() {
    	
       	clearComponents();
    	
       	// Allow the user to select or add a child.
       	Vector<String> childIds = getColumnFromVecOfStrVectors(myDb.findStringDataEntries(true, "childId", ""), 0);
     
       	JComboBox childBox = new JComboBox(childIds.toArray(new String[0]));
       	childBox.addActionListener(new ChildSelectionListener());
       	childBox.setSelectedIndex(-1);
       	JTextField newChildField = new JTextField(10);
       	JButton newChildButton = new JButton("Add New ChildId");
       	newChildButton.addActionListener(new NewChildButtonListener(childBox, newChildField));

       	add(childBox);
       	add(newChildField);
       	add(newChildButton);
       	
    	// Create components relating to status. These should initially be disabled since no
       	// child has been selected.
        myPracticeButton = new JRadioButton("Practice");
        myReliabilityButton = new JRadioButton("Reliability");
        myFinalButton = new JRadioButton("Final");    

        myStatusButtonGroup = new ButtonGroup();
        myStatusButtonGroup.add(myPracticeButton);
        myStatusButtonGroup.add(myReliabilityButton);
        myStatusButtonGroup.add(myFinalButton);
        
        // Initially disable the buttons since no child is selected.
        myPracticeButton.setEnabled(false);
        myPracticeButton.setText("Practise");
        myPracticeButton.addActionListener(new TypeSelectedListener());
        myReliabilityButton.setEnabled(false);
        myReliabilityButton.setText("Reliability");
        myReliabilityButton.addActionListener(new TypeSelectedListener());
        myFinalButton.setEnabled(false);
        myFinalButton.setText("Final");
        myFinalButton.addActionListener(new TypeSelectedListener());
    	
        add(myPracticeButton);
        add(myReliabilityButton);
        add(myFinalButton);
        
    	// Create components relating to the metadata.
        myExtraMetadataNames = myDb.findStringMetadata("extrametadata", "value");
        myExtraMetadataTypes = myDb.findStringMetadata("extrametadata", "type");
        
        // Remove the coderId and childId extrametadata, which are included seperately.
        int coderIndex = myExtraMetadataNames.indexOf("coderId");
        myExtraMetadataNames.remove(coderIndex);
        myExtraMetadataTypes.remove(coderIndex);
        int childIndex = myExtraMetadataNames.indexOf("childId");
        myExtraMetadataNames.remove(childIndex);
        myExtraMetadataTypes.remove(childIndex);
       
        myExtraMetadataComponents = new Vector<Component>();
        for(int i = 0; i < myExtraMetadataNames.size(); i++) {
        	JComponent tempComponent;
        	if(myExtraMetadataTypes.get(i).compareTo("DATE") == 0) {
        	    SpinnerModel datemodel = new SpinnerDateModel();
        	    JSpinner spinner = new JSpinner(datemodel);
        	    spinner.setEditor(new JSpinner.DateEditor(spinner, "dd MMMMM yyyy"));
        	    spinner.setPreferredSize(new Dimension(200, 25));
        	    tempComponent = spinner;
        	}
        	else if(myExtraMetadataTypes.get(i).compareTo("INTEGER") == 0) {
        		JFormattedTextField tempField = new JFormattedTextField();
        		tempField.setValue(new Integer(0));
        		tempField.setColumns(10);
        		tempComponent = tempField;
    	    }
        	else if(myExtraMetadataTypes.get(i).compareTo("TEXT") == 0) {
        		JTextField tempField = new JTextField("");
        		tempField.setColumns(10);
        		tempComponent = tempField;
        		
        	}
        	else {
        		tempComponent = new JTextField("ERROR IN DEFINING METADATA");
        	}
        	
        	JPanel tempPanel = new JPanel();
        	setTitledBorder(tempPanel, myExtraMetadataNames.get(i) + ":", 0, 2);
        	tempPanel.add(tempComponent);
        	add(tempPanel);
        	myExtraMetadataComponents.add(tempComponent); // Record the component for use when recording the data.
        }
        
        myEnterDataButton = new JButton("Begin entering data");
        myEnterDataButton.addActionListener(new enterDataButtonListener(this));
        myEnterDataButton.setEnabled(false);
        add(myEnterDataButton);

        revalidate();
    	repaint();
    	myFrame.pack();
    }

    class TypeSelectedListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
        	// A type has been selected - enable the data entry button.
        	myEnterDataButton.setEnabled(true);
        }
    }
    class ChildSelectionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
        	String childId = (String)((JComboBox)e.getSource()).getSelectedItem();
        	
        	// Set the selected child.
        	childIsSelected(childId);
        }
    }
    
    class NewChildButtonListener implements ActionListener {
    	
    	private JComboBox myChildIdBox;
    	private JTextField myNewChildIdField;
    	
    	public NewChildButtonListener(JComboBox childIdBox, JTextField newChildIdField) {
    		myChildIdBox = childIdBox;
    		myNewChildIdField = newChildIdField;
    	}
    	
        public void actionPerformed(ActionEvent e) {
        	// Check that a unique childId has been entered.
        	String childId = myNewChildIdField.getText();
        	if(childId.length() == 0 || !(childId.matches("\\p{Alnum}+"))) {
          		JOptionPane.showMessageDialog((JComponent)e.getSource(), "Please enter a child Id. It must not contain spaces or symbols.", 
      										  "Error", JOptionPane.ERROR_MESSAGE);
          		return;
        	}
        	
        	for(int i = 0; i < myChildIdBox.getItemCount(); i++) {
        		if(childId.equalsIgnoreCase((String)myChildIdBox.getItemAt(i))) {
              		JOptionPane.showMessageDialog((JComponent)e.getSource(), 
              				  "That childId already exists. Please select it from the list above.", 
							  "Error", JOptionPane.ERROR_MESSAGE);
              		return;        			
        		}
        	}
        	
        	// A new childId has been specified. Add it to the list and make it selected by default.
        	myChildIdBox.addItem(childId);
        	if(myChildIdBox.getSelectedIndex() != myChildIdBox.getItemCount() - 1) {
        		myChildIdBox.setSelectedIndex(myChildIdBox.getItemCount() - 1);
        	} // The if is because adding an item to an empty list causes an extra selection event.
        }	
    }
    
    class enterDataButtonListener implements ActionListener {
    	
    	private DataEntryGui myGui;
    	private JComboBox myNamesBox;
    	public enterDataButtonListener(DataEntryGui gui) {
    		myGui = gui; 
    	}
    	
        public void actionPerformed(ActionEvent e) {
        	// Check that the correct metadata has been entered.       	
        	myGui.enterDataButtonClicked();
        }
    }
    
	private void childIsSelected(String childId) {
		if(childId == null) {
			return;// nothing was selected, i.e. during initialisation.
		}
		myChildId = childId;
		
		// Determine which trial types are remaining for this coder/child combination:
		// Is there Final data for this child by anyone?
		Vector<String> finalCodersThisChild = getColumnFromVecOfStrVectors(
										myDb.findStringDataEntries(true, "coderId", 
										"WHERE childId='" + childId + "' AND trialStatus='final'"), 0);
				
		boolean finalAlreadyEnteredForChild = finalCodersThisChild.size() > 0 ? true : false;
	    
		// Has this user already entered any data for this child?
		Vector<String> allCodersThisChild = getColumnFromVecOfStrVectors(
									myDb.findStringDataEntries(true, "coderId", 
									"WHERE childId='" + childId + "'"), 0);
		
		boolean coderAlreadyEnteredForChild = allCodersThisChild.contains(myCoderId);
		
		// Are reliabilites already present for this child?
		Vector<String> reliabilityCodersThisChild = getColumnFromVecOfStrVectors(
				myDb.findStringDataEntries(true, "coderId", 
				"WHERE childId='" + childId + "' AND trialStatus='reliability'"), 0);
				
		myReliaibilitiesAlreadyEnteredForChild = reliabilityCodersThisChild.size() > 0 ? true : false;

		// Now disable/enable the components depending on these results.
		// Default to all disabled.
		myFinalButton.setEnabled(false);
		myPracticeButton.setEnabled(false);
		myReliabilityButton.setEnabled(false);
		
		// If the final hasn't been entered, and I haven't completed a practice or reliability...
		// (Note the only way this can be true false is if the final data has been deleted)
		if(!finalAlreadyEnteredForChild && !coderAlreadyEnteredForChild) {
			myFinalButton.setEnabled(true);
		}
		if(finalAlreadyEnteredForChild && !coderAlreadyEnteredForChild) {
			myPracticeButton.setEnabled(true); 
			myReliabilityButton.setEnabled(true);
		}		
		// Unselect all and disable the data entry button.
		myStatusButtonGroup.setSelected(new JButton("invisible dummy button").getModel(), true);
		myEnterDataButton.setEnabled(false);
    }

    
    public void enterDataButtonClicked() {

    	// Determine which trial status has been chosen.
    	if(myFinalButton.isSelected()) {
    		myStatus = "final";
    	} else if(myReliabilityButton.isSelected()) {
    		myStatus = "reliability";
    	} else if(myPracticeButton.isSelected()) {
    		myStatus = "practice";
    	} else {
    		System.out.println("No status selected, quitting.");
    		System.exit(1);
    	}
    	
    	// Get the metadata values (not including coderId and childId).
    	myExtraMetadataValues.clear();
    	for(int i = 0; i < myExtraMetadataComponents.size(); i++) {

    		// Get the component value.
    		if(myExtraMetadataTypes.get(i).compareTo("DATE") == 0) {
    			Date date = ((SpinnerDateModel)(((JSpinner)(myExtraMetadataComponents.get(i))).
    											getModel())).getDate();
    			myExtraMetadataValues.add(date.getDate() + "\\" + date.getMonth() + "\\" + (date.getYear() + 1900));
    		}
    		else if(myExtraMetadataTypes.get(i).compareTo("INTEGER") == 0) {
    			 Integer theInt = (Integer)(((JFormattedTextField)
    						   				(myExtraMetadataComponents.get(i))).getValue());
    			 myExtraMetadataValues.add(theInt.toString());
    		}
    		else if(myExtraMetadataTypes.get(i).compareTo("TEXT") == 0) {
    			myExtraMetadataValues.add(((JTextField)(myExtraMetadataComponents.get(i))).getText());
    		}
    	}
    	
    	// Ensure none of the data values were left blank.
    	for(int i = 0; i < myExtraMetadataValues.size(); i++) {
    		if(myExtraMetadataValues.get(i).equals("")) {
           		JOptionPane.showMessageDialog(this, myExtraMetadataNames.get(i) + " is empty!", 
							  "Error", JOptionPane.ERROR_MESSAGE);
           		return;
    		}
    	}
    	
    	// Warn about previous reliabilities if required.
    	if(myReliaibilitiesAlreadyEnteredForChild && myStatus == "reliability") {
        	int isOk = JOptionPane.showConfirmDialog(this,
        			"Reliability data has already been entered for this child,\n" +
    				"are you sure that you wish to continue?",
    				"Warning",
    				JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
        	if(isOk != 0) return;
    	}

    	// Check with the user if this is correct.
    	String checkString = "Is the following correct?\n\n"; // Move down.
    	String descriptionString = "";
    	
    	checkString += "CoderId: " + myCoderId + "\n" +
    				   "ChildId: " + myChildId + "\n" +
    				   "Status: " + myStatus + "\n";
    	descriptionString += "CoderId: " + myCoderId + ", ChildId: " + myChildId +
    						 ", Status: " + myStatus + ", ";
    	
      	for (int i = 0; i < myExtraMetadataNames.size(); i++) {
    		checkString += myExtraMetadataNames.get(i) + ": " + myExtraMetadataValues.get(i) 
    					   + (i == myExtraMetadataNames.size() - 1 ? "" : "\n");
    		descriptionString += myExtraMetadataNames.get(i) + ": " + myExtraMetadataValues.get(i)
    						     + (i == myExtraMetadataNames.size() - 1 ? "" : ", ");
    	}

    	int isOk = JOptionPane.showConfirmDialog(this,checkString,
    				"Is the following correct?",
    				JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
    	if(isOk != 0) return;

    	// The metadata has been collected - display the database entry screen.
    	// Clear the current window contents.
		clearComponents();
    	
    	// Instantiate the main data entry screen. ///////////////////////////////////////////////////////
    	String[] trialNames = myDb.findStringMetadata("trialName", "value").toArray(new String[0]);
		String[] behaviours = myDb.findStringMetadata("behaviour", "value").toArray(new String[0]);
    	String[] stimuli = myDb.findStringMetadata("stimuli", "value").toArray(new String[0]);
    	int noTimeSlots = myDb.findIntMetadata("noBehaviours", "value").get(0).intValue();
    	
    	JTabbedPane tabbedPane = new JTabbedPane();
    	myDataEntryTabs = new Vector<MainDataEntryScreen>();
    	for(int i = 0; i < trialNames.length; i++) {
    		MainDataEntryScreen screen = 
    						new MainDataEntryScreen(behaviours, stimuli, noTimeSlots, this);
    		myDataEntryTabs.add(screen);
    		tabbedPane.addTab(trialNames[i], screen);
    	}
    	
    	add(tabbedPane);
    	revalidate();
    	repaint();
    	myFrame.pack();
    	myFrame.setTitle(descriptionString);
    }
    
	public void dataEntryResultsRecieved() {

		// Check that each trial has data present.
		// CHECK OTHER CRITERIA AS WELL
		boolean allTrialsPresent = true;
		for(int i = 0; i < myDataEntryTabs.size(); i++) {
			if(myDataEntryTabs.get(i).getBehaviourData().isEmpty()) {
				allTrialsPresent = false;
			}
		}
	 	
		if(!allTrialsPresent) {
       	 	JOptionPane.showMessageDialog(this,
 			 	"Not all trials have data entered, please check each tab.", 
 			 	"error", JOptionPane.ERROR_MESSAGE);
       	 	return;
		}
		
		// Check that there are now blank columns with data occuring after them.
    	String[] trialNames = myDb.findStringMetadata("trialName", "value").toArray(new String[0]);
		String skippedWarningString = "The following time slots have been skipped: \n\n";
		boolean timesWereSkipped = false;
		for(int i = 0; i < myDataEntryTabs.size(); i++) {
			Vector<Integer> skippedTimes = myDataEntryTabs.get(i).getSkippedTimes();
			for(int t = 0; t < skippedTimes.size(); t++) {
				if(t == 0) skippedWarningString += trialNames[i] + ": ";
				skippedWarningString += (skippedTimes.get(t));
				skippedWarningString += t == (skippedTimes.size()-1) ? "\n" : ", ";
				timesWereSkipped = true;
			}
		}
		skippedWarningString += "\nPlease ensure that they contain at least one stimuli.";
		
		if(timesWereSkipped) {
       	 	JOptionPane.showMessageDialog(this, skippedWarningString, "error", JOptionPane.ERROR_MESSAGE);
           	return;			
		}
		
		// Confirm that the data really is to be saved.
    	int isOk = JOptionPane.showConfirmDialog(this, "Are you sure you want to save this data?",
				"Save results",
				JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
    	if(isOk != 0) {
    		return;
    	}		
		
    	// The results are valid and have been accepted. Add them to the database.
    	myDb.addResults(myCoderId, myChildId, myStatus, 
    					myExtraMetadataNames, myExtraMetadataValues,
    					myDataEntryTabs);
    	
    	// Quit.
    	JOptionPane.showInternalMessageDialog(this, "Data Saved. Now Quitting.");
    	myDb.closeDatabase();
    	System.exit(0);
	}

      /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createAndShowGUI() {
    	
        JFrame frame = new JFrame("Data Entry");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Create and set up the content pane.
        DataEntryGui theGui = new DataEntryGui(frame);
        theGui.constructExperimentSelectorScreen();
        theGui.setOpaque(true); //content panes must be opaque
        frame.setContentPane(theGui);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    private void setTitledBorder(JComponent com, String titleString, int innerSpace, int outerSpace) {
    	com.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(outerSpace,outerSpace,outerSpace,outerSpace),
        		BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
        			titleString, TitledBorder.LEFT, TitledBorder.TOP), BorderFactory.createEmptyBorder(innerSpace,innerSpace,innerSpace,innerSpace))));
    }
    
    private void clearComponents() {
       	// clear the previous gui contents. (always remove the top component).
		int noComponents = this.getComponentCount();
    	for(int i = 0; i < noComponents; i++) {
    		remove(this.getComponent(0));
    	}
    	this.setBorder(new EmptyBorder(20,20,20,20));
    }
    
    private Vector<String> getColumnFromVecOfStrVectors(Vector<Vector<String> > vec, int columnNo) {
    	Vector<String> result = new Vector<String>();
    	for(int i = 0; i < vec.size(); i++) {
    		result.add(vec.get(i).get(columnNo));
    	}
    	return result;
    }
    
    public static void main(String[] args) {
    	
			//System.load(new File(new File("t.tmp").getAbsolutePath()).getParentFile().getAbsolutePath() + "\\lib\\sqlite3.dll");
  			//System.load(new File(new File("t.tmp").getAbsolutePath()).getParentFile().getAbsolutePath() + "\\lib\\rdeSQLite3.dll");
    	
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
    	
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
}
