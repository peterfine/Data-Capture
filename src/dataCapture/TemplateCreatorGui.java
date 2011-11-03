/*
 * CreateTemplateGui, The gui which creates template files.
 * Peter Fine, May 2008
 */

package dataCapture;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;

public class TemplateCreatorGui extends JPanel {

	JTextField myExperimentNameField;
	JSpinner myNoBehavioursSpinner;
	JButton mySaveExperimentButton;
	
	JPanel myNamePanel;
	JPanel myNoBehavioursPanel;
	EditableStringList myTrialNamePanel;
	EditableStringList myBehaviourPanel;
	EditableStringList myStimuliPanel;
	EditablePropertyList myExtraMetadataPanel;
    
    public void constructContents() {
    	setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

    	Vector<MetaDataItem> defaultProperties = new Vector<MetaDataItem>();
    	defaultProperties.add(new MetaDataItem("coderId", "TEXT", false));
    	defaultProperties.add(new MetaDataItem("childId", "TEXT", false));
    	    	
        myExperimentNameField = new JTextField(10);
    	JLabel nameLabel = new JLabel("Experiment Name: ");
    	nameLabel.setLabelFor(myExperimentNameField);
    	
    	myNamePanel = new JPanel();
    	myNamePanel.setLayout(new BoxLayout(myNamePanel, BoxLayout.LINE_AXIS));
    	myNamePanel.add(nameLabel);
    	myNamePanel.add(myExperimentNameField);
    	
    	myNoBehavioursSpinner = new JSpinner(new SpinnerNumberModel(20, 1, 5000, 1));
    	JLabel noBehavioursLabel = new JLabel("Maximum number of behaviours per trial: ");
    	noBehavioursLabel.setLabelFor(myNoBehavioursSpinner);
    	
    	myNoBehavioursPanel = new JPanel();
    	myNoBehavioursPanel.setLayout(new BoxLayout(myNoBehavioursPanel, BoxLayout.LINE_AXIS));
    	myNoBehavioursPanel.add(noBehavioursLabel);
    	myNoBehavioursPanel.add(myNoBehavioursSpinner);
    	
        
    	myTrialNamePanel = new EditableStringList("Add Trial Name", "Remove Trial Name", 0);
        myBehaviourPanel = new EditableStringList("Add Behaviour", "Remove Behaviour", 12);
        myStimuliPanel = new EditableStringList("Add Stimuli", "Remove Stimuli", 12);
        myExtraMetadataPanel = new EditablePropertyList("Add Metadata", "Remove MetaData", defaultProperties);
        
        mySaveExperimentButton = new JButton("Save Experiment");
        mySaveExperimentButton.addActionListener(new saveExperimentListener());
        
        add(myNamePanel);
        add(myNoBehavioursPanel);
        add(myTrialNamePanel);
        add(myBehaviourPanel);
        add(myStimuliPanel);
        add(myExtraMetadataPanel);
        add(mySaveExperimentButton);
    }

    class saveExperimentListener implements ActionListener {
    	
        public void actionPerformed(ActionEvent e) {
    
        	// Get the data defining the experiment.
        	String expName = myExperimentNameField.getText();
        	int noBehaviours = ((SpinnerNumberModel)myNoBehavioursSpinner.getModel()).getNumber().intValue();
        	Vector<String> trialNames = myTrialNamePanel.getContents();
        	Vector<String> behaviours = myBehaviourPanel.getContents();
        	Vector<String> stimuli = myStimuliPanel.getContents();
        	Vector<MetaDataItem> extrametadata = myExtraMetadataPanel.getContents();
        	
        	// Validate Experiment.
        	Vector<String> errors = new Vector<String>();
        	if(expName.equalsIgnoreCase("")) {	errors.add("Please enter the experiment name.\n"); }
        	if(trialNames.isEmpty()) { errors.add("Please enter at least one trial name.\n"); }
        	if(behaviours.isEmpty()) { errors.add("Please enter at least one behaviour.\n"); }
        	if(stimuli.isEmpty()) { errors.add("Please enter at least one stimuli.\n"); }

          	if(!errors.isEmpty()) {
          		String[] errorsArray = (String[]) errors.toArray(new String[0]);
        		JOptionPane.showMessageDialog(myBehaviourPanel, errorsArray, "Error", JOptionPane.ERROR_MESSAGE);
        		return;
        	}
        	
        	// Check whether this experiment name has already been defined.
          	boolean alreadyDefined = false;
          	Vector<String> existingExpNames = DatabaseInterface.getExperiments();
          	for(int i = 0; i < existingExpNames.size(); i++) {
          		if(expName.equalsIgnoreCase(existingExpNames.get(i))) {
          			alreadyDefined = true;
          		}
          	}
          	if(alreadyDefined) {
          		JOptionPane.showMessageDialog(myBehaviourPanel, "An experiment named '" + expName + 
          					"' already exists!", "Error", JOptionPane.ERROR_MESSAGE);
        		return;
          	}
          	// Check if the name is alphanumeric.
          	if(!(expName.matches("\\p{Alnum}+"))) {
          		JOptionPane.showMessageDialog(myBehaviourPanel, "The experiment name must not contain spaces or symbols.", 
          					"Error", JOptionPane.ERROR_MESSAGE);
        		return;
          	}
          	
          	// Add to the database.
          	DatabaseInterface newDB = new DatabaseInterface(expName);
          	newDB.defineExperiment(noBehaviours, trialNames, behaviours, stimuli, extrametadata);
          		
          	// Report success.
          	JOptionPane.showMessageDialog(myBehaviourPanel, expName + " sucessfully created. The program will now exit.", "Experiment Created", JOptionPane.INFORMATION_MESSAGE);
          		
          	// Quit the program.
          	newDB.closeDatabase();
          	System.exit(0);
          	     	
        }
    }
    
    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createAndShowGUI() {
    	
        JFrame frame = new JFrame("Template Creator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Create and set up the content pane.
        TemplateCreatorGui theGui = new TemplateCreatorGui();
        theGui.constructContents();
        theGui.setOpaque(true); //content panes must be opaque
        frame.setContentPane(theGui);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
    	
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
}
