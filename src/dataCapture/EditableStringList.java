/*
 * EditableStringList, a component displaying a list which can have items added or removed.
 * Peter Fine, May 2008
 * 
 * Based on ListDemo
 * 
 * Copyright (c) 1995 - 2008 Sun Microsystems, Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Sun Microsystems nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */ 

package dataCapture;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;

/* ListDemo.java requires no other files. */
public class EditableStringList extends JPanel
                      implements ListSelectionListener {
    private JList myList;
    private DefaultListModel myListModel;

    private JButton myRemoveEntryButton;
    private JTextField myNewBehaviourName;
    
    private int myCharLim;

    public EditableStringList(String addEntryString, String removeEntryString, int charLim) {
        super(new BorderLayout());

        myCharLim = charLim;
        myListModel = new DefaultListModel();

        //Create the myList and put it in a scroll pane.
        myList = new JList(myListModel);
        myList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        myList.setSelectedIndex(0);
        myList.addListSelectionListener(this);
        myList.setVisibleRowCount(6);
        JScrollPane listScrollPane = new JScrollPane(myList);
        
        JButton addEntryButton = new JButton(addEntryString);
        AddEntryListener addEntryListener = new AddEntryListener(addEntryButton);
        addEntryButton.setActionCommand(addEntryString);
        addEntryButton.addActionListener(addEntryListener);
        addEntryButton.setEnabled(false);

        myRemoveEntryButton = new JButton(removeEntryString);
        myRemoveEntryButton.setActionCommand(removeEntryString);
        myRemoveEntryButton.addActionListener(new removeEntryListener());
        myRemoveEntryButton.setEnabled(false);

        myNewBehaviourName = new JTextField(10);
        myNewBehaviourName.addActionListener(addEntryListener);
        myNewBehaviourName.getDocument().addDocumentListener(addEntryListener);
        //String name = myListModel.getElementAt(myList.getSelectedIndex()).toString();

        //Create a panel that uses BoxLayout.
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane,
                                           BoxLayout.LINE_AXIS));
        buttonPane.add(myRemoveEntryButton);
        buttonPane.add(Box.createHorizontalStrut(5));
        buttonPane.add(new JSeparator(SwingConstants.VERTICAL));
        buttonPane.add(Box.createHorizontalStrut(5));
        buttonPane.add(myNewBehaviourName);
        buttonPane.add(addEntryButton);
        buttonPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        add(listScrollPane, BorderLayout.CENTER);
        add(buttonPane, BorderLayout.PAGE_END);
    }

    Vector<String> getContents() {
    	Vector<String> contents = new Vector<String>();
    	for(int i = 0; i < myList.getModel().getSize(); i++) {
    		contents.add((String)myList.getModel().getElementAt(i));
    	}
    	return contents;
    }
    
    class removeEntryListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            //This method can be called only if
            //there's a valid selection
            //so go ahead and remove whatever's selected.
            int index = myList.getSelectedIndex();
            myListModel.remove(index);

            int size = myListModel.getSize();

            if (size == 0) { //Nobody's left, disable firing.
                myRemoveEntryButton.setEnabled(false);

            } else { //Select an index.
                if (index == myListModel.getSize()) {
                    //removed item in last position
                    index--;
                }

                myList.setSelectedIndex(index);
                myList.ensureIndexIsVisible(index);
            }
        }
    }

    //This listener is shared by the text field and the add entry button.
    class AddEntryListener implements ActionListener, DocumentListener {
        private boolean alreadyEnabled = false;
        private JButton button;

        public AddEntryListener(JButton button) {
            this.button = button;
        }

        //Required by ActionListener.b
        public void actionPerformed(ActionEvent e) {
            String name = myNewBehaviourName.getText();

            //User didn't type in a unique name...
            if (name.equals("") || alreadyInList(name)) {
                Toolkit.getDefaultToolkit().beep();
                myNewBehaviourName.requestFocusInWindow();
                myNewBehaviourName.selectAll();
                return;
            }
            
            //Check charlim satisfied.
            if((myCharLim > 0) && (name.length() > myCharLim)) {
        		JOptionPane.showMessageDialog(button, "Must be no more than " + myCharLim + " characters long.", 
        									  "Error", JOptionPane.ERROR_MESSAGE);
        		return;
            }

            int index = myList.getSelectedIndex(); //get selected index
            if (index == -1) { //no selection, so insert at beginning
                index = 0;
            } else {           //add after the selected item
                index++;
            }

            myListModel.insertElementAt(myNewBehaviourName.getText(), index);
            //If we just wanted to add to the end, we'd do this:
            //myListModel.addElement(myNewBehaviourName.getText());

            //Reset the text field.
            myNewBehaviourName.requestFocusInWindow();
            myNewBehaviourName.setText("");

            //Select the new item and make it visible.
            myList.setSelectedIndex(index);
            myList.ensureIndexIsVisible(index);
        }

        //This method tests for string equality. You could certainly
        //get more sophisticated about the algorithm.  For example,
        //you might want to ignore white space and capitalization.
        protected boolean alreadyInList(String name) {
            return myListModel.contains(name);
        }

        //Required by DocumentListener.
        public void insertUpdate(DocumentEvent e) {
            enableButton();
        }

        //Required by DocumentListener.
        public void removeUpdate(DocumentEvent e) {
            handleEmptyTextField(e);
        }

        //Required by DocumentListener.
        public void changedUpdate(DocumentEvent e) {
            if (!handleEmptyTextField(e)) {
                enableButton();
            }
        }

        private void enableButton() {
            if (!alreadyEnabled) {
                button.setEnabled(true);
            }
        }

        private boolean handleEmptyTextField(DocumentEvent e) {
            if (e.getDocument().getLength() <= 0) {
                button.setEnabled(false);
                alreadyEnabled = false;
                return true;
            }
            return false;
        }
    }

    //This method is required by ListSelectionListener.
    public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting() == false) {

            if (myList.getSelectedIndex() == -1) {
            //No selection, disable fire button.
                myRemoveEntryButton.setEnabled(false);

            } else {
            //Selection, enable the fire button.
            	myRemoveEntryButton.setEnabled(true);
            }
        }
    }

}
