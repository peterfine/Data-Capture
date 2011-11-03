/*
 * MainDataEntryScreen, Where the video data is entered.
 * Peter Fine, June 2008
 */

package dataCapture;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;

import java.io.File;
 
public class MainDataEntryScreen extends JPanel
{

	DefaultTableModel myTableModel;
	String[] myBehaviourNames;
	
	public MainDataEntryScreen(String[] behaviours, 
							   String[] stimuli, int noTimeSlots, 
							   DataEntryGui mainGui)
	{
	super(new BorderLayout());
	
	JTable table = new JTable();
	myTableModel = (DefaultTableModel)table.getModel();
	myBehaviourNames = behaviours;
	
	// Append "" to the stimuli names to allow deselection.
	String[] tempStimuli = new String[stimuli.length + 1];
	tempStimuli[0] = "";
	for(int i = 0; i < stimuli.length; i++) {
		tempStimuli[i+1] = stimuli[i];
	}
	stimuli = tempStimuli;

    // Add some columns
    int noCols = noTimeSlots;
    int noRows = behaviours.length;
    for(int i = 0; i < noCols; i++) {
    	myTableModel.addColumn(i);
    }
    for(int i = 0; i < noRows; i++) {
    	myTableModel.addRow(new Vector());
    }
    for(int i = 0; i < noCols; i++) {
    	table.getColumnModel().getColumn(i).setCellEditor(new MyComboBoxEditor(stimuli, i, table));
    	table.getColumnModel().getColumn(i).setHeaderValue(i+1);
    }
    // Set the cell size.
    String longestStimuliName = "";
    for(int i = 0; i < stimuli.length; i++) {
    	if(stimuli[i].length() > longestStimuliName.length()) {
    		longestStimuliName = stimuli[i];
    	}
    }
    int width = new JTextField(longestStimuliName).getPreferredSize().width + 10;
    for(int i = 0; i < table.getColumnCount(); i++) {
    	table.getColumnModel().getColumn(i).setMinWidth(width);
    	table.getColumnModel().getColumn(i).setMaxWidth(width);
    	table.getColumnModel().getColumn(i).setPreferredWidth(width);
    }
    
    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    table.getTableHeader().setReorderingAllowed(false);
  
    // Put the table in a scroll pane and add it to the pane.
    JScrollPane scrollPane = new JScrollPane(table);   
    addRowHeader(scrollPane, table, behaviours); // Use the method provided below...
    add(scrollPane, BorderLayout.CENTER);
    
    JButton saveData = new JButton("Save Results");
    saveData.addActionListener(new SaveDataListener(mainGui));
    
    add(saveData, BorderLayout.EAST);
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    setPreferredSize(new Dimension(Math.min(screenSize.width - 100, width*noCols + 160),
    							   Math.min(screenSize.height - 50, table.getPreferredSize().height) + 40));
    }

    class MyComboBoxEditor extends DefaultCellEditor {

    	// Includes things used by the action listener which enforces max=2 per column items.
    	public MyComboBoxEditor(String[] items, int columnIndex, JTable table) {
            super(new JComboBox(items));
            ((JComboBox)super.getComponent()).addActionListener(new CellActionListener(columnIndex, table));
        }
    }
    
    class CellActionListener implements ActionListener {
    	int myColumnIndex;
    	JTable myTable;
    	public CellActionListener(int columnIndex, JTable table) {
    		myColumnIndex = columnIndex;
    		myTable = table;
    	}
    	
        public void actionPerformed(ActionEvent e) {
        	// Determine if there are more than two behaviours already in this column.
        	int noStimuliForThisSlot = 0;
        	for(int behav = 0; behav < myTableModel.getRowCount(); behav++) {
    			String stimuliName = ((String)(myTableModel.getValueAt(behav, myColumnIndex)));
    			if(stimuliName != null && stimuliName.compareTo("") != 0) {
    				noStimuliForThisSlot++;
    			}
    		}
        	
        	// If more than two, disable this box, as long as there's not already something in it!
        	String stimuliName = (String)((JComboBox)e.getSource()).getSelectedItem();
        	if(noStimuliForThisSlot >= 2 && (stimuliName == null || stimuliName.compareTo("") == 0)) {
        		((JComboBox)e.getSource()).setEnabled(false);
        	} else {
        		((JComboBox)e.getSource()).setEnabled(true);
        	}
        }
    }
    
    // For the save button.
    class SaveDataListener implements ActionListener {

    	private DataEntryGui myMainGui;
    	
    	public SaveDataListener(DataEntryGui mainGui) { 
    		myMainGui = mainGui;
    	}
    	
        public void actionPerformed(ActionEvent e) {
    		myMainGui.dataEntryResultsRecieved();
    	}
    }
    
	public Vector<String> getBehaviourData() {
		
		Vector<String> behaviours = new Vector<String>();
		for(int timeSlot = 0; timeSlot < myTableModel.getColumnCount(); timeSlot++) {
			for(int behav = 0; behav < myTableModel.getRowCount(); behav++) {
				String stimuliName = ((String)(myTableModel.getValueAt(behav, timeSlot)));
				if(stimuliName != null && stimuliName.compareTo("") != 0) {
					behaviours.add(myBehaviourNames[behav]);
				}
			}
		}
		return behaviours;
	}
	
	public Vector<String> getStimuliData() {
		Vector<String> stimuli = new Vector<String>();
		for(int timeSlot = 0; timeSlot < myTableModel.getColumnCount(); timeSlot++) {
			for(int behav = 0; behav < myTableModel.getRowCount(); behav++) {
				String stimuliName = ((String)(myTableModel.getValueAt(behav, timeSlot)));
				if(stimuliName != null && stimuliName.compareTo("") != 0) {
					stimuli.add(stimuliName);
				}
			}
		}
		return stimuli;
	}
	
	public Vector<Integer> getTimeData() {
		Vector<Integer> times = new Vector<Integer>();
		for(int timeSlot = 0; timeSlot < myTableModel.getColumnCount(); timeSlot++) {
			for(int behav = 0; behav < myTableModel.getRowCount(); behav++) {
				String stimuliName = ((String)(myTableModel.getValueAt(behav, timeSlot)));
				if(stimuliName != null && stimuliName.compareTo("") != 0) {
					times.add(timeSlot+1);
				}
			}
		}
		return times;		
	}
	
	public Vector<Integer> getSkippedTimes() {

		Vector<Integer> blanks = new Vector<Integer>();
		// Held in tempBlanks but only added to blanks if a subsequent column has data entered in it.
		Vector<Integer> tempBlanks = new Vector<Integer>(); 		                                                    
		for(int timeSlot = 0; timeSlot < myTableModel.getColumnCount(); timeSlot++) {
			boolean blankTime = true;
			for(int behav = 0; behav < myTableModel.getRowCount(); behav++) {
				String stimuliName = ((String)(myTableModel.getValueAt(behav, timeSlot)));
				if(stimuliName != null && stimuliName.compareTo("") != 0) {
					blankTime = false;
				}
			}
			if(blankTime) {
				tempBlanks.add(timeSlot+1);
			} 
				else {
					// Add tempBlanks to blanks and clear it.
					blanks.addAll(tempBlanks);
					tempBlanks.clear();
				}
		}

		return blanks;	
	}
    
    
    ////////////////////////////////////////////// Following is used to generate the row headers.
    
    /**
     * Define the look/content for a cell in the row header
     * In this instance uses the JTables header properties
     **/
    class RowHeaderRenderer extends JLabel implements ListCellRenderer {
        
        /**
         * Constructor creates all cells the same
         * To change look for individual cells put code in
         * getListCellRendererComponent method
         **/
        RowHeaderRenderer(JTable table) {
            JTableHeader header = table.getTableHeader();
            setOpaque(true);
            setBorder(UIManager.getBorder("TableHeader.cellBorder"));
            setHorizontalAlignment(CENTER);
            setForeground(header.getForeground());
            setBackground(header.getBackground());
            setFont(header.getFont());
        }
        
        /**
         * Returns the JLabel after setting the text of the cell
         **/
        public Component getListCellRendererComponent( JList list,
        Object value, int index, boolean isSelected, boolean cellHasFocus) {
            
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }
    
    private void addRowHeader(JScrollPane scrollPane, JTable table, String[] behaviours) {
    	 class HeaderListModel extends AbstractListModel {
             String headers[];
    		 public HeaderListModel(String[] headerStrings) {headers = headerStrings; }
             public int getSize() { return headers.length; }
             public Object getElementAt(int index) { return headers[index]; }
         };
    	
         HeaderListModel listModel = new HeaderListModel(behaviours); 
         
         // Create single component to add to scrollpane
         JList rowHeader = new JList(listModel);
         rowHeader.setFixedCellWidth(new JList(listModel.headers).getPreferredSize().width);
         rowHeader.setFixedCellHeight(table.getRowHeight());
         rowHeader.setCellRenderer(new RowHeaderRenderer(table));
         
         scrollPane.setRowHeaderView(rowHeader); // Adds row-list left of the table
    }
}
