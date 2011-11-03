/*
 * DataAnalysisGui, The gui which allows data to be analysed.
 * Peter Fine, Oct 2008
 */

package dataCapture;

import java.awt.*;
import java.awt.event.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Vector;
import java.util.Date;
import java.math.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;

import dataCapture.EditableStringList.removeEntryListener;

public class DataAnalysisGui extends JPanel {

	private DatabaseInterface myDb;
	private JFrame myFrame;
	private String myExpName;
	
    public DataAnalysisGui(JFrame frame) {
        super();
        myFrame = frame;
    }

    // SELECT EXPERIMENT -------------------------------------------------------------------------------------

    public void constructExperimentSelectorScreen() {
    	setLayout(new BoxLayout(this, FlowLayout.CENTER));

    	// Allow the user to choose the experiment.
    	setTitledBorder(this, "Select an experiment:", 30, 10);
    	String[] experimentNames = DatabaseInterface.getExperiments().toArray(new String[0]);
    	JComboBox experimentSelector = new JComboBox(experimentNames);
    	JButton experimentSelectorButton = new JButton("Begin analysing data");
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
    	
    	private DataAnalysisGui myGui;
    	private JComboBox myNamesBox;
    	public experimentSelectorListener(DataAnalysisGui gui, JComboBox namesBox) {
    		myGui = gui; 
    		myNamesBox = namesBox;
    	}
    	
        public void actionPerformed(ActionEvent e) {
        	myGui.constructAnalysisGui(myNamesBox.getSelectedItem().toString());
        }
    }
    
    /**
     * The experiment has been selected, construct the data analysis gui.
     */
    public void constructAnalysisGui(String expName) {
    	myExpName = expName;
    	myDb = new DatabaseInterface(expName); // Load the database.
    	clearComponents();

    	JTabbedPane tabbedPane = new JTabbedPane();
    	tabbedPane.add("Output Data", getOutputDataTab());
    	tabbedPane.add("Analyse Reliabilities", getReliabilityAnalysisTab());
    	tabbedPane.add("Delete Data", getDeleteDataTab());

    	add(tabbedPane);
    	
        revalidate();
    	repaint();
    	myFrame.pack();
    }
    
    private JPanel getOutputDataTab() {
    	JPanel outputPanel = new JPanel();
    	
    	Vector<SelectionPanel> selectPanels = new Vector<SelectionPanel>();

    	// Add a list of children to select.
    	Vector<String> childNames = getColumnFromVecOfStrVectors(
    		myDb.findStringDataEntries(true, "childId", "WHERE trialStatus = 'final' "), 0);
    	selectPanels.add(new SelectionPanel(childNames, "Select Children:", true, null));

    	// Add a list of trial names to select.
    	Vector<String> trialNames = myDb.findStringMetadata("trialName", "value");
    	selectPanels.add(new SelectionPanel(trialNames, "Select Trials:", true, null));
    	
    	// Add a list of stimuli names to select.
    	Vector<String> stimuliNames = myDb.findStringMetadata("stimuli", "value");
    	selectPanels.add(new SelectionPanel(stimuliNames, "Select Stimuli:", true, null));
    	
    	// Add a list of behaviour names to select.
    	Vector<String> behaviourNames = myDb.findStringMetadata("behaviour", "value");
    	selectPanels.add(new SelectionPanel(behaviourNames, "Select Behaviours:", true, null));
    	
    	// Add a list of metadata names to select.
    	Vector<String> metadataNames = myDb.findStringMetadata("extrametadata", "value");
    	metadataNames.remove("childId"); // Already included above.
    	selectPanels.add(new SelectionPanel(metadataNames, "Select Metadata:", true, null));
    	
    	JPanel selectDataPanel = new JPanel();
    	for(int i = 0; i < selectPanels.size(); i++) {
    		selectDataPanel.add(selectPanels.get(i));
    	}
 
    	JPanel writeDataPanel = new JPanel();
    	writeDataPanel.setLayout(new BoxLayout(writeDataPanel, BoxLayout.Y_AXIS));
    	JButton occurrenceButton = new JButton("Write Occurrence Data");
    	occurrenceButton.addActionListener(new OccurrenceListener(selectPanels));
    	JButton sequenceButton = new JButton("Write Sequence Data");
    	sequenceButton.addActionListener(new SequenceListener(selectPanels));
    	writeDataPanel.add(occurrenceButton);
    	writeDataPanel.add(sequenceButton);
    	
    	outputPanel.add(selectDataPanel);
    	outputPanel.add(writeDataPanel);
    	
    	return outputPanel;
    }
    
    class OccurrenceListener implements ActionListener {
    	Vector<SelectionPanel> mySelectPanels;
    	OccurrenceListener(Vector<SelectionPanel> selectPanels) { mySelectPanels = selectPanels; };
		public void actionPerformed(ActionEvent e) {
			//Prepare the data for the csv file.
			String occString = "childId,";
			
			// Extract the selected data.
			String[] selectedChildren = mySelectPanels.get(0).getSelected();
			String[] selectedTrials = mySelectPanels.get(1).getSelected();
			String[] selectedStimuli = mySelectPanels.get(2).getSelected();
			String[] selectedBehaviours = mySelectPanels.get(3).getSelected();
			String[] selectedMetadata = mySelectPanels.get(4).getSelected();
			
			// Prepare the metadata field names.
			String metadataFields = "";
			for(int i = 0; i < selectedMetadata.length; i++) {
				occString += selectedMetadata[i] + ",";
				metadataFields += selectedMetadata[i];
				metadataFields += (i == (selectedMetadata.length-1)) ? "" : ", ";
			}
			occString += "trial,stimuli,behaviour,occurrences\n";
			
			// For each of these cases, search for and add the data as requested.
			for(int child = 0; child < selectedChildren.length; child++) {
				// The metadata is the same for each child, so get it here.
				Vector<Vector<String> > metadataValues = 
					myDb.findStringDataEntries(true, metadataFields, 
										   	   "WHERE trialStatus = 'final' AND childId = '" +
										   	   selectedChildren[child] + "'");
				if(metadataValues.size() != 1) {
					System.out.println("Serious Error: Metadata not unique per child, quitting.");
					System.exit(1);
				}
				String metadataValuesString = "";
				for(int i = 0; i < metadataValues.get(0).size(); i++) {
					metadataValuesString += metadataValues.get(0).get(i) + ", ";
				}
				
				  for(int trial = 0; trial < selectedTrials.length; trial++) {
			       for(int stim = 0; stim < selectedStimuli.length; stim++) {
			        for(int behav = 0; behav < selectedBehaviours.length; behav++) {
				   
			         // Count the number of occurrences matching this case.
			         String searchCriteria = "WHERE trialStatus = 'final' AND ";
			         searchCriteria += "childId = '" + selectedChildren[child] + "' AND ";
			         searchCriteria += "trialName = '" + selectedTrials[trial] + "' AND ";
				 	 searchCriteria += "stimuli = '" + selectedStimuli[stim] + "' AND ";
				 	 searchCriteria += "behaviour = '" + selectedBehaviours[behav] + "'";
				 
				 	 Vector<String> occs = getColumnFromVecOfStrVectors(
				 		myDb.findStringDataEntries(false, "coderId", searchCriteria), 0);
				 	 	int noOccurrences = occs.size();
				 
				 	 occString += selectedChildren[child] + ", " + metadataValuesString 
				 			    + selectedTrials[trial] + ", " +
				 			  selectedStimuli[stim] + ", " + selectedBehaviours[behav] + ", " +
				 			  noOccurrences + "\n";
			        }
			       }
				  }
			}
			
			//Open a file chooser and write the file if required.
			if(writeToFile((Component)e.getSource(), occString) == false) {
				return;
			}
		}
    }
    
    class SequenceListener implements ActionListener {
    	Vector<SelectionPanel> mySelectPanels;
    	SequenceListener(Vector<SelectionPanel> selectPanels) { mySelectPanels = selectPanels; };
		public void actionPerformed(ActionEvent e) {
			//Prepare the data for the csv file.
			String seqString = "childId,";
			
			// Extract the selected data.
			String[] selectedChildren = mySelectPanels.get(0).getSelected();
			String[] selectedTrials = mySelectPanels.get(1).getSelected();
			String[] selectedStimuli = mySelectPanels.get(2).getSelected();
			String[] selectedBehaviours = mySelectPanels.get(3).getSelected();
			String[] selectedMetadata = mySelectPanels.get(4).getSelected();
			
			// Prepare the metadata field names.
			String metadataFields = "";
			for(int i = 0; i < selectedMetadata.length; i++) {
				seqString += selectedMetadata[i] + ",";
				metadataFields += selectedMetadata[i];
				metadataFields += (i == (selectedMetadata.length-1)) ? "" : ", ";
			}
			seqString += "trial,stimuli,behaviour,time\n";
			// For each of these cases, search for and add the data as requested.
			for(int child = 0; child < selectedChildren.length; child++) {
				// The metadata is the same for each child, so get it here.
				Vector<Vector<String> > metadataValues = 
					myDb.findStringDataEntries(true, metadataFields, 
										   	   "WHERE trialStatus = 'final' AND childId = '" +
										   	   selectedChildren[child] + "'");
				if(metadataValues.size() != 1) {
					System.out.println("Serious Error: Metadata not unique per child, quitting.");
					System.exit(1);
				}
				String metadataValuesString = "";
				for(int i = 0; i < metadataValues.get(0).size(); i++) {
					metadataValuesString += metadataValues.get(0).get(i) + ", ";
				}
				
				  for(int trial = 0; trial < selectedTrials.length; trial++) {
				   
			         // Write the sequence of behaviours.
			         String searchCriteria = "WHERE trialStatus = 'final' AND ";
			         searchCriteria += "childId = '" + selectedChildren[child] + "' AND ";
			         searchCriteria += "trialName = '" + selectedTrials[trial] + "' ORDER BY behaviourNo";
				 
				 	 Vector<Vector<String> > actions =
				 		myDb.findStringDataEntries(false, "behaviourNo, stimuli, behaviour", 
				 								   searchCriteria);
				     for(int act = 0; act < actions.size(); act++) {
				    	 // Check if the stimuli or behaviour are excluded from the search.
				    	 boolean containsStimuli = false;
				    	 for(int stim = 0; stim < selectedStimuli.length; stim++) {
				    		 if(selectedStimuli[stim].equals(actions.get(act).get(1))) {
				    			 containsStimuli = true;
				    		 }
				    	 }
				    	 boolean containsBehaviour = false;
				    	 for(int behav = 0; behav < selectedBehaviours.length; behav++) {
				    		 if(selectedBehaviours[behav].equals(actions.get(act).get(2))) {
				    			 containsBehaviour = true;
				    		 }
				    	 }
				    	 // XXX IF STIM OR BEHAVs ARE EXCLUDED, PUSH DOWN THE TIMES???
				    	 // XXX HEADINGS FOR THIS AND OCC?
				    	 if(containsStimuli && containsBehaviour) {
				    		 seqString += selectedChildren[child] + ", " + metadataValuesString + 
				    		  selectedTrials[trial] + ", " +
				    		  actions.get(act).get(1) + ", " + actions.get(act).get(2) + ", " +
				    		  actions.get(act).get(0) + "\n";
				    	 }
				     }
			        }
			       }
			
			// Write to file if requested.
			if(writeToFile((Component)e.getSource(), seqString) == false) {
				return;
			}
		}
    }
    
    private JPanel getReliabilityAnalysisTab() {
    	JPanel analysisPanel = new JPanel();
    	
    	// Select a child to determine the reliability data from.
    	Vector<String> childNames = getColumnFromVecOfStrVectors(
        		myDb.findStringDataEntries(true, "childId", ""), 0);
    	SelectionPanel childPanel = new SelectionPanel(childNames, "Select Child:", false, null);
    	childPanel.myList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    	childPanel.myList.setSelectedIndex(-1);
    	analysisPanel.add(childPanel);
    	
    	// Choose whether to compute the reliability or practice scores.
    	JPanel statusPanel = new JPanel();
    	statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));
    	JRadioButton reliabilityButton = new JRadioButton("Reliability Trials");
    	JRadioButton practiceButton = new JRadioButton("Practice Trials");
    	ButtonGroup statusGroup = new ButtonGroup();
    	reliabilityButton.setSelected(true); // default to reliability.
    	statusGroup.add(reliabilityButton);
    	statusGroup.add(practiceButton);
    	statusPanel.add(reliabilityButton);
    	statusPanel.add(practiceButton);
    	analysisPanel.add(statusPanel);
    	
    	JPanel buttonPanel = new JPanel();
    	buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
    	setTitledBorder(buttonPanel, "Compute reliabilities:", 0, 2);
    	JButton singleSeqButton = new JButton("Sequence - Selected Child");
    	JButton allSeqButton = new JButton("Sequence - All Children");
    	JButton singleOccButton = new JButton("Occurrence - Selected Child");
    	JButton allOccButton = new JButton("Occurrence - All Children");
    	
    	singleSeqButton.setPreferredSize(new Dimension(140,40));
    	singleSeqButton.addActionListener(new ReliabilityListener(childPanel, reliabilityButton));
    	allSeqButton.addActionListener(new ReliabilityListener(childPanel, reliabilityButton));
    	singleOccButton.addActionListener(new ReliabilityListener(childPanel, reliabilityButton));
    	allOccButton.addActionListener(new ReliabilityListener(childPanel, reliabilityButton));

    	buttonPanel.add(singleSeqButton);
    	buttonPanel.add(allSeqButton);
    	buttonPanel.add(singleOccButton);
    	buttonPanel.add(allOccButton);
    	analysisPanel.add(buttonPanel);
    	
    	return analysisPanel;
    }

    class ReliabilityListener implements ActionListener {
    	SelectionPanel myChildPanel;
    	JRadioButton myReliabilityButton;
    	Vector<Double> myReliabilityScores; // The most recently collected reliability scores.
    	ReliabilityListener(SelectionPanel childPanel, JRadioButton reliabilityButton) {
    		myChildPanel = childPanel;
    		myReliabilityButton = reliabilityButton;
    		myReliabilityScores = new Vector<Double>();
    	}
    	
    	public void actionPerformed(ActionEvent e) {
    		
    		// Determine which button was pressed.

    		boolean isSingleOccurrence = 
    				((JButton) e.getSource()).getText().equals("Occurrence - Selected Child");
    		boolean isAllOccurrence = 
				((JButton)e.getSource()).getText().equals("Occurrence - All Children");
    		boolean isSingleSequence = 
				((JButton)e.getSource()).getText().equals("Sequence - Selected Child");
    		boolean isAllSequence = 
			((JButton)e.getSource()).getText().equals("Sequence - All Children");
    		
    		// Clear the occurrence score record.
    		myReliabilityScores.clear();
    		
    		// Get the results for the selected child.
    		if(isSingleOccurrence || isSingleSequence) {
    			// Determine which child is selected.
    			String childId;
    			if(myChildPanel.myList.getSelectedIndex() < 0) {
    				JOptionPane.showMessageDialog((Component)e.getSource(), 
    						"Please select a child.", 
    						"Unable to compute reliabilities", JOptionPane.ERROR_MESSAGE); 
    				return;
    			} 
    			else {
    				childId = (String)myChildPanel.myList.getSelectedValue();
    			}
    		
    			String childResult = getReliabilityString(isSingleOccurrence || isAllOccurrence, 
    												  	  false, childId, e);
    			
    			if(childResult == null) { return; }
    			if(childResult.equals("")) { childResult = "No comparisons found for this child."; }
    			// Present the result to the screen, if it is a single occurrence.
    			if(isSingleOccurrence) {
    				JOptionPane.showMessageDialog((Component)e.getSource(), 
    						childResult, "Single child occurrence reliability:", 
    						JOptionPane.INFORMATION_MESSAGE);
    				return;
    			}
    			else {
    				// It is a single sequence. 
    				if(childResult.equals("No comparisons found for this child.")) {
        				JOptionPane.showMessageDialog((Component)e.getSource(), 
        						childResult, 
        						"No Data", JOptionPane.ERROR_MESSAGE); 
        				return;    					
    				}
    				//Save it to a file if requested.
    				if(writeToFile((Component)e.getSource(), childResult) == false) {
    					return;
    				}
    			}
    		}
    		else {
    			// The results should be computed for all children with final scores
    			// and written to a file.
    	    	Vector<String> childNames = getColumnFromVecOfStrVectors(
    	        		myDb.findStringDataEntries(true, "childId", "WHERE trialStatus = 'final' "), 0);
    	    	
    	    	Vector<String> noFinalChildrenVec = new Vector<String>();
    	    	Vector<String> noReliabilitiesChildrenVec = new Vector<String>();
    	    	String resultString = "";
    	    	for(int childNo = 0; childNo < childNames.size(); childNo++) {
    	    		
    	    		String childId = childNames.get(childNo);
    	    		String childResult = getReliabilityString(isAllOccurrence, true,
    	    												  childId, e);
    	    		if(childResult.equals("noFinal")) {
    	    			noFinalChildrenVec.add(childId);
    	    		}
    	    		else if(childResult.equals("")) {
    	    			noReliabilitiesChildrenVec.add(childId);
    	    		}
    	    		else {
    	    			resultString += childResult + "\n";
    	    		}
    	    	}
    	    	
    	    	// Check that the no result children are permissible.
    	    	if(noFinalChildrenVec.size() > 0) {
    	    		String noFinalString = "The following children have no final data entered:\n\n";
    	    		for(int i = 0; i < noFinalChildrenVec.size(); i++) {
    	    			noFinalString += noFinalChildrenVec.get(i);
    	    			noFinalString += (i == (noFinalChildrenVec.size()-1)) ? ".\n" : ", ";
    	    		}
    	    		noFinalString += "\nDo you wish to still save the remaining children's data?\n";
    	            int response = JOptionPane.showConfirmDialog ((Component)e.getSource(),
    	            	   noFinalString,"Data Missing",
    	  	               JOptionPane.OK_CANCEL_OPTION,
    	  	               JOptionPane.QUESTION_MESSAGE);
    	  	        if (response == JOptionPane.CANCEL_OPTION) { return; }
    	    	}
    	    	if(noReliabilitiesChildrenVec.size() > 0) {
    	    		String noReliabilityString = "The following children have no reliability data entered:\n\n";
    	    		for(int i = 0; i < noReliabilitiesChildrenVec.size(); i++) {
    	    			noReliabilityString += noReliabilitiesChildrenVec.get(i);
    	    			noReliabilityString += (i == (noReliabilitiesChildrenVec.size()-1)) ? ".\n" : ", ";
    	    		}
    	    		noReliabilityString += "\nDo you wish to still save the remaining children's data?\n";
    	            int response = JOptionPane.showConfirmDialog ((Component)e.getSource(),
    	            		noReliabilityString,"Data Missing",
    	  	               JOptionPane.OK_CANCEL_OPTION,
    	  	               JOptionPane.QUESTION_MESSAGE);
    	  	        if (response == JOptionPane.CANCEL_OPTION) { return; }
    	    	}

    	    	// If occurrence data was collected, compute and include some statistics.
    	    	if(isAllOccurrence) {
    	    		// Get the mean, sd and range of these values.
    	    		double total = 0.0;
    	    		double sqTotal = 0.0;
    	    		double min = 0.0;
    	    		double max = 0.0;
    	    		for(int i = 0; i < myReliabilityScores.size(); i++) {
    	    			double val = myReliabilityScores.get(i) * 100;
    	    			total += val;
    	    			sqTotal += val*val;
    	    			if((i == 0) || val < min) {
    	    				min = val;
    	    			}
    	    			if(val > max) {
    	    				max = val;
    	    			}
    	    		}
    	    		double mean = total / myReliabilityScores.size();
    	    		// XXX SHOULD BE SAMPLE OR POP SD? CHECK ALG.
    	    		double sd = Math.sqrt( sqTotal/myReliabilityScores.size() - mean*mean );
    	    		
    	    		// Round the values to 2 d.p.
    	    		mean = (Math.round(mean * 100) / 100);
    	    		sd = (Math.round(sd * 100) / 100);
    	    		min = (Math.round(min * 100) / 100);
    	    		max = (Math.round(max * 100) / 100);
    	    		resultString += "\nMean = " + mean + ", S.D. = " + sd + ", min = " + min + ", max = " + max + "\n";
    	    	}
    	    	
    	    	// Save the data to a file.
				if(writeToFile((Component)e.getSource(), resultString) == false) {
					return;
				}
    		}

    	}
    	
    	// allChildrenMode assumes no fail errors, and no practice data.
    	private String getReliabilityString(boolean isOccurrence, boolean allChildrenMode,
    										String childId, ActionEvent e) {
    	
	   		// Determine which coders are involved.
			Vector<Vector<String> > finalCoderVec =  
				myDb.findStringDataEntries(true, "coderId", 
						"WHERE trialStatus = 'final' AND childId = '" + childId + "'");
			if(finalCoderVec.size() != 1 || finalCoderVec.get(0).size() != 1) {
				if(allChildrenMode) {
					return "noFinal";
				}
					else {
						JOptionPane.showMessageDialog((Component)e.getSource(), 
		        			"No data has been entered under the 'final' status for this child.", 
		        			"Unable to compute reliabilities", JOptionPane.ERROR_MESSAGE);
						return null;
					}
			}
			String finalCoder = finalCoderVec.get(0).get(0);
			
			// Determine whether reliability or practice data should be included.
			boolean isReliability = allChildrenMode || myReliabilityButton.isSelected();
			
			// Determine which coders have provided practice and reliability data.
			Vector<String> reliabilityCoders = getColumnFromVecOfStrVectors(
					myDb.findStringDataEntries(true, "coderId",
					"WHERE childId = '" + childId + "' AND trialStatus = 'reliability'"), 0);
			Vector<String> practiceCoders = getColumnFromVecOfStrVectors(
					myDb.findStringDataEntries(true, "coderId",
					"WHERE childId = '" + childId + "' AND trialStatus = 'practice'"), 0);
	
			String sequenceString = "";
			String occurrenceString = "";
			Vector <Double> reliabilityVec = new Vector<Double>();
			
			if(isReliability) {
	    		for(int coderANo = 0; coderANo < reliabilityCoders.size(); coderANo++) {
	    			String coderA = reliabilityCoders.get(coderANo);
	    			
	    			// Compare with the final coder.
	    			if(isOccurrence) {
	    				occurrenceString += 
	    					getOccurrenceString(childId, coderA, "reliability", 
	    										finalCoder, "final");
	    					reliabilityVec.add(computeOccurrenceReliability(childId, coderA, finalCoder));
	    			}
	    			else {
	    				sequenceString += getSequenceReliabilityString(
	    													childId, coderA, "reliability", 
	    													finalCoder, "final");
	    			}
	
	    			// Now compare with the remaining reliability coders.
	    			for(int coderBNo = coderANo + 1; coderBNo < reliabilityCoders.size(); coderBNo++) {
	    				String coderB = reliabilityCoders.get(coderBNo);
	        			if(isOccurrence) {
	        				occurrenceString += getOccurrenceString(childId, coderA, "reliability", 
	        														coderB, "reliability");
	        				reliabilityVec.add(computeOccurrenceReliability(childId, coderA, coderB));
	        			}
	        			else {
	        				sequenceString += getSequenceReliabilityString(
	        													childId, coderA, "reliability", 
	        													coderB, "reliability");
	        			}
	    			}
	    		}
	    		// Append these reliability scores to the record.
	    		myReliabilityScores.addAll(reliabilityVec);
			}
			else { // Compute Practices.
	    		for(int coderANo = 0; coderANo < practiceCoders.size(); coderANo++) {
	    			String coderA = practiceCoders.get(coderANo);
	
	    			// Compare with the final coder.
	       			if(isOccurrence) {
	       				occurrenceString += getOccurrenceString(childId, coderA, "practice",
	       														finalCoder, "final");
	    			}
	    			else {
	    				sequenceString += getSequenceReliabilityString(
	    													childId, coderA, "practice", 
	    													finalCoder, "final");
	    			}
				
	    			// Now compare with the reliability coders.
	    			for(int coderBNo = 0; coderBNo < reliabilityCoders.size(); coderBNo++) {
	    				String coderB = reliabilityCoders.get(coderBNo);
	           			if(isOccurrence) {
	           				occurrenceString += getOccurrenceString(childId, coderA, "practice", 
	           														coderB, "reliability");
	        			}
	        			else {
	        				sequenceString += getSequenceReliabilityString(
	        													childId, coderA, "practice", 
	        													coderB, "reliability");
	        			}
	    			}
	    		}
			}
			
			
			
			// Strings have been collected, return the desired result.
			if(isOccurrence) {
				return occurrenceString;
			}
			else {
				return sequenceString;
			}
    	}
    }
    
    private String getOccurrenceString(String childId, String coderA, String statusA, 
    								   String coderB, String statusB) {
    	
    	double result = computeOccurrenceReliability(childId, coderA, coderB);
    	return "Child: " + childId + ", " + coderA + "-" + statusA + " vs " + coderB +
		"-" + statusB + ", Score = " + (Math.round(result * 10000) / 100) + "%\n";
    }
    
    private double computeOccurrenceReliability(String childId, String coder1, String coder2) {

    	Vector<String> trialNames = myDb.findStringMetadata("trialName", "value");
    	Vector<String> stimuliNames = myDb.findStringMetadata("stimuli", "value");
    	Vector<String> behaviourNames = myDb.findStringMetadata("behaviour", "value");
    	
    	// XXX CHECK THIS ALG IS CORRECT.
    	int minCount = 0;
    	int maxCount = 0;
    	// For each trial type:
    	for(int trial = 0; trial  < trialNames.size(); trial++) {
    		// For each stimuli type:
    		for(int stim = 0; stim < stimuliNames.size(); stim++) {
    			// For each behaviour:
    			for(int behav = 0; behav < behaviourNames.size(); behav++) {
    				// Get the number of occurrences for each coder.
    				int coder1No = myDb.findStringDataEntries(false, "coderId",
    					"WHERE childId = '" + childId + "' AND " +
    					"coderId = '" + coder1 + "' AND " + 
    					"trialName = '" + trialNames.get(trial) + "' AND " +
    					"stimuli = '" + stimuliNames.get(stim) + "' AND " +
    					"behaviour = '" + behaviourNames.get(behav) + "'").size();
    				int coder2No = myDb.findStringDataEntries(false, "coderId",
        					"WHERE childId = '" + childId + "' AND " +
        					"coderId = '" + coder2 + "' AND " + 
        					"trialName = '" + trialNames.get(trial) + "' AND " +
        					"stimuli = '" + stimuliNames.get(stim) + "' AND " +
        					"behaviour = '" + behaviourNames.get(behav) + "'").size();
    				minCount += Math.min(coder1No, coder2No);
    				maxCount += Math.max(coder1No, coder2No);
    			}
    		}
    	}
    	
    	return ((double)minCount)/((double)maxCount);
    }
    
    private String getSequenceReliabilityString(String childId, String coder1, String coder1Status, 
    										    String coder2, String coder2Status) {

    	String output = coder1 + ": " + coder1Status + ", " + coder2 + ":" + coder2Status + "\n";
    	
    	Vector<String> trialNames = myDb.findStringMetadata("trialName", "value");
    	
    	// For each trial type:
    	for(int trial = 0; trial  < trialNames.size(); trial++) {
    		// For each time slot in this trial:
    		// Get all the times for each coder and find the maximum.
    		Vector<Vector<String> > coder1Events = 
    			myDb.findStringDataEntries(false, "behaviourNo, stimuli, behaviour",
					"WHERE childId = '" + childId + "' AND " +
					"coderId = '" + coder1 + "' AND " + 
					"trialName = '" + trialNames.get(trial) + "' " +
					"ORDER BY behaviourNo");
    		Vector<Vector<String> > coder2Events = 
    			myDb.findStringDataEntries(false, "behaviourNo, stimuli, behaviour",
					"WHERE childId = '" + childId + "' AND " +
					"coderId = '" + coder2 + "' AND " + 
					"trialName = '" + trialNames.get(trial) + "' " +
					"ORDER BY behaviourNo");
    		
    		// Add these events to the return string.
    		output += trialNames.get(trial) + "\n";
    		output += coder1+","+coder1+","+coder1+","+coder2+","+coder2+","+coder2+"\n";
    		for(int behavNo = 0; behavNo < Math.max(coder1Events.size(), coder2Events.size()); behavNo++) {
    			if(behavNo < coder1Events.size()) {
    				int time = new Integer(coder1Events.get(behavNo).get(0)).intValue();
    				output +=   time + "," +
    							coder1Events.get(behavNo).get(1) + "," +
    							coder1Events.get(behavNo).get(2) + ",";
    			}
    			else {
    				output += ",,,";
    			}
    			if(behavNo < coder2Events.size()) {
    				int time = new Integer(coder2Events.get(behavNo).get(0)).intValue();
    				output += time + "," +
    							coder2Events.get(behavNo).get(1) + "," +
    							coder2Events.get(behavNo).get(2) + "\n";
    			}
    			else {
    				output += ",,\n";
    			}
    		}
    	}
    	return output;
    }
    
    class SelectionPanel extends JPanel {
    	
    	public JList myList;
    	
    	// Note extraComponent can be null.
    	SelectionPanel(Vector<String> listData, String title, boolean showSelectAll,
    				   JComponent extraComponent) {
    		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    		myList = new JList(listData);
    		JScrollPane listScroller = new JScrollPane(myList);
    		listScroller.setPreferredSize(new Dimension(90, 100));
    		JButton selectAllButton = new JButton("Select All");
    		selectAllButton.addActionListener(new SelectAllListener(myList));
    		setTitledBorder(this, title, 0, 2);
    		if(extraComponent != null) { add(extraComponent); }
    		add(listScroller);
    		if(showSelectAll) { add(selectAllButton); }
    	}
    	
    	public String[] getSelected() {
    		String[] selectedValues = new String[myList.getSelectedValues().length];
    		for(int i = 0; i < selectedValues.length; i++) {
    			selectedValues[i] = (myList.getSelectedValues()[i]).toString();
    		}
    		return selectedValues; 		
    	}
    	
    	public void setListEnabled(boolean isEnabled) {
    		if(!isEnabled) {
    			myList.clearSelection();
    		}
    		myList.setEnabled(isEnabled);
    	}
    	
        class SelectAllListener implements ActionListener {
        	
        	JList myList;
        	
        	public SelectAllListener(JList list) {
        		myList = list;
        	}
        	
            public void actionPerformed(ActionEvent e) {
            	int end = myList.getModel().getSize()-1;
                if (end >= 0) {
                	myList.setSelectionInterval(0, end);
                }
            }
        }
    }
    
    private JPanel getDeleteDataTab() {
    	JPanel deletePanel = new JPanel();
    	
    	// Add selectors for the child.
    	Vector<String> childNames = getColumnFromVecOfStrVectors(
        		myDb.findStringDataEntries(true, "childId", ""), 0);
    	SelectionPanel childPanel = new SelectionPanel(childNames, "Select Child:", false, null);
    	childPanel.myList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    	childPanel.myList.setSelectedIndex(-1);
    	deletePanel.add(childPanel);
    	
    	// Add selectors for the coder.
    	JRadioButton coderDeleteMethodButton = new JRadioButton("Select by CoderId");
    	SelectionPanel coderPanel = new SelectionPanel(new Vector<String>(), "", false, 
    												   coderDeleteMethodButton);
    	coderPanel.setListEnabled(false);
    	deletePanel.add(coderPanel);
    	
    	// Add selectors for the trial status.
    	JRadioButton statusDeleteMethodButton = new JRadioButton("Select by Trial Status");
    	SelectionPanel statusPanel = new SelectionPanel(new Vector<String>(), "", false,
    													statusDeleteMethodButton);
    	statusPanel.setListEnabled(false);
    	deletePanel.add(statusPanel);
    	
    	// Add listeners.
    	childPanel.myList.addListSelectionListener(new ChildToDeleteChangeListener(coderPanel, statusPanel));
    	coderDeleteMethodButton.addActionListener(new DeleteMethodListener(coderPanel, statusPanel));
    	statusDeleteMethodButton.addActionListener(new DeleteMethodListener(statusPanel, coderPanel));

    	ButtonGroup methodGroup = new ButtonGroup();
    	methodGroup.add(coderDeleteMethodButton);
    	methodGroup.add(statusDeleteMethodButton);
    	methodGroup.setSelected(new JButton("invisible dummy button").getModel(), true);
    	
    	JButton deleteButton = new JButton("Delete Selected Data");
    	deleteButton.addActionListener(new DeleteDataListener(childPanel, coderPanel, statusPanel));
    	deletePanel.add(deleteButton);
    	
    	return deletePanel;
    }
    
    class ChildToDeleteChangeListener implements ListSelectionListener {
    	SelectionPanel myCoderPanel;
    	SelectionPanel myStatusPanel;
    	ChildToDeleteChangeListener(SelectionPanel coderPanel, SelectionPanel statusPanel) {
    		myCoderPanel = coderPanel;
    		myStatusPanel = statusPanel;
    	}
    	
		public void valueChanged(ListSelectionEvent e) {
			// Update the coder and status boxes based on the selected child.
			String selectedChild = ((JList)e.getSource()).getSelectedValue().toString();
			Vector<String> coderNames = getColumnFromVecOfStrVectors(
		    		myDb.findStringDataEntries(true, "coderId", "WHERE childId = '" + 
		    								   selectedChild + "'"), 0);
			myCoderPanel.myList.setListData(coderNames);
			myCoderPanel.myList.setSelectedIndex(-1);
			Vector<String> trialStatus = getColumnFromVecOfStrVectors(
		    		myDb.findStringDataEntries(true, "trialStatus", "WHERE childId = '" + 
		    								   selectedChild + "'"), 0);
			myStatusPanel.myList.setListData(trialStatus);
			myStatusPanel.myList.setSelectedIndex(-1);
		}
    	
    }
    
	class DeleteMethodListener implements ActionListener {
		SelectionPanel myThisPanel;
		SelectionPanel myOtherPanel;
		DeleteMethodListener(SelectionPanel thisPanel, SelectionPanel otherPanel) {
			myThisPanel = thisPanel;
			myOtherPanel = otherPanel;
		}
		public void actionPerformed(ActionEvent e) {
			myThisPanel.setListEnabled(true);
			myOtherPanel.setListEnabled(false);
		}
	}

	class DeleteDataListener implements ActionListener {
		
		SelectionPanel myChildPanel;
    	SelectionPanel myCoderPanel;
    	SelectionPanel myStatusPanel;
    	DeleteDataListener(SelectionPanel childPanel, 
    					   SelectionPanel coderPanel, 
    					   SelectionPanel statusPanel) {
    		myChildPanel = childPanel;
    		myCoderPanel = coderPanel;
    		myStatusPanel = statusPanel;
    	}
		public void actionPerformed(ActionEvent e) {
			if(myChildPanel.myList.isSelectionEmpty() ||
			   (!myCoderPanel.myList.isEnabled() && !myStatusPanel.myList.isEnabled()) ||
			   (myCoderPanel.myList.isEnabled() && myCoderPanel.myList.isSelectionEmpty()) ||
			   (myStatusPanel.myList.isEnabled() && myStatusPanel.myList.isSelectionEmpty())) {
	        	 JOptionPane.showMessageDialog((Component)e.getSource(), 
	        			 	"Please select a child, and one or more of either the coders or trial statuses.", 
	        			 	"error", JOptionPane.ERROR_MESSAGE);
	        	 return;
			}
			
			String childToDelete = myChildPanel.myList.getSelectedValue().toString();
			String[] codersToDelete = myCoderPanel.getSelected();
			String[] statusToDelete = myStatusPanel.getSelected();
			
			String confirmString = "This will DELETE all data for the childId " + 
									myChildPanel.myList.getSelectedValue();
			if(myCoderPanel.myList.isEnabled()) {
				confirmString += " \n which was entered by the coderId";
				confirmString += codersToDelete.length > 1 ? "s " : " ";
				for(int i = 0; i < codersToDelete.length; i++) {
					confirmString += codersToDelete[i];
					confirmString += i == codersToDelete.length-1 ? "" : ", ";
				}
			} else if(myStatusPanel.myList.isEnabled()) {
				confirmString += " \n under the status";
				confirmString += statusToDelete.length > 1 ? "es " : " ";
				for(int i = 0; i < statusToDelete.length; i++) {
					confirmString += statusToDelete[i];
					confirmString += i == statusToDelete.length-1 ? "" : ", ";
				}
			} else {
	        	 JOptionPane.showMessageDialog((Component)e.getSource(), 
	        			 "Serious deletion tab error, contact author.", 
	        			 	"error", JOptionPane.ERROR_MESSAGE);
	        	 System.exit(1);
			}
			confirmString += "\n\nAre you certain that you want to delete this data?";
			
           	int isOk = JOptionPane.showConfirmDialog((Component)e.getSource(), confirmString,
           			"Confirm Deletion",
    				JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
        	if(isOk != 0) {
        		return;
        	}
        	
        	// Get final confirmation.
        	String isYes = 
        		JOptionPane.showInputDialog((Component)e.getSource(), "If you wish to delete this data," +
        			"\nplease enter 'yes' in the box below.", "Final Confirmation.", 
        			JOptionPane.WARNING_MESSAGE);
        	if(isYes != null && isYes.equalsIgnoreCase("yes")) {
        		int count = 0;
        		if(myCoderPanel.myList.isEnabled()) {
        			for(int i = 0; i < codersToDelete.length; i++) {
        				 count += myDb.deleteData("WHERE childId = '" + childToDelete + "' AND " +
        						"coderId = '" + codersToDelete[i] + "';");
        			}
        		} 
        		else {
        			for(int i = 0; i < statusToDelete.length; i++) {
       				 count += myDb.deleteData("WHERE childId = '" + childToDelete + "' AND " +
       						"trialStatus = '" + statusToDelete[i] + "';");
        			}
        		}

        		String entryString = (count == 1) ? "entry was" : "entries were";
   	        	 JOptionPane.showMessageDialog((Component)e.getSource(), 
	        			 count + " " + entryString + " deleted.",
	        			 "Entries Deleted", JOptionPane.INFORMATION_MESSAGE);
   	        	 // Regenerate the entire analysis gui, to ensure data changes are reflected.
   	        	constructAnalysisGui(myExpName);
        	}
        	else {
	        	JOptionPane.showMessageDialog((Component)e.getSource(), 
	        			"'yes' not entered - not data will be deleted.", 
	        			"Not Deleted", JOptionPane.ERROR_MESSAGE);
	        		return;
        	}
			
		}
	}
	
    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createAndShowGUI() {
    	
        JFrame frame = new JFrame("Data Anaysis");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Create and set up the content pane.
        DataAnalysisGui theGui = new DataAnalysisGui(frame);
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
    
    private boolean writeToFile(Component c, String toWrite) {	
    
	    JFileChooser fc = new JFileChooser(new File("..//OutputFiles"));
		int returnVal = fc.showSaveDialog(c);
		
	    if (returnVal == JFileChooser.APPROVE_OPTION) {
	    	File file = fc.getSelectedFile();
	        if (file.exists ()) {

	            int response = JOptionPane.showConfirmDialog (c,
	              "Overwrite existing file?","Confirm Overwrite",
	               JOptionPane.OK_CANCEL_OPTION,
	               JOptionPane.QUESTION_MESSAGE);
	            if (response == JOptionPane.CANCEL_OPTION) { return false; }
	        }
	        // Write the data to the file.
	        try {		     
	            BufferedWriter out = new BufferedWriter(new FileWriter(file));
	            out.write(toWrite);
	            out.close();
	        } 
	         catch (IOException exception) {
	        		JOptionPane.showMessageDialog(c, "This file could not be created. Perhaps it is already open?",
	        				"File Not Written",
	            			JOptionPane.WARNING_MESSAGE);
	        }
	
	    } 
	    else {
	        return false;
	    }
	    return true;
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
