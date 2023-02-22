/*
* Copyright 2005 Tryllian Solutions B.V. 
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this Lesser; see the file COPYING.LIB.  If not, write to
 * the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA 02110-1301, USA.
 *
 * See license.html for more information.
 */
package com.tryllian.snmp.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import pt.ipb.agentapi.mibs.MibNode;
import org.opennms.protocols.snmp.*;

/**
 * Main panel of the SNMP GUI that show the values of the OIDs and the buttons
 * to get or set the values.
 * This class listens when the user click on the tree, click on the buttons
 * or when the SNMP agent sends an answer.
 */
public class ControlPanel extends JPanel 
                         implements TreeSelectionListener,
                                    ActionListener,
                                    DocumentListener,
                                    SnmpHandler
{
    // The tree where the user chooses the OID (displayed outside this panel).
    private JTree tree;
      
    // The console used for the errors and the walk results.
    private JTextArea console;
    
    // The GUI elements of this panel.
    private JPanel displayPanel;
    private JTextField jtfOID;
    private JTextField jtfOIDNumber;
    private JTextField jtfAccess;
    private JTextField jtfValue;
    private JTextField jtfType;
    private JComboBox setType;
    private JTextArea jtaDescription;
    private JButton getButton;
    private JButton getNextButton;
    private JButton setButton;
    private JButton walkButton;
    private JButton stopButton;
    private JButton clearButton;
    
    private int rowCount = 0;
    
    // The class that interacts with the SNMP agent.
    private SnmpCommander snmpCommand;
    
    // Status flags for the walk procedure
    private boolean walking = false;
    private boolean stopped = false;
    private String walkOID = "";
    
    /** 
     * Creates the ControlPanel 
     */
    public ControlPanel(JTree tree, Properties snmpProperties, 
      JTextArea console) {
        this.tree = tree;
        this.console = console;
        setLayout(new BorderLayout());
        createUI();
        tree.addTreeSelectionListener(this);
        snmpCommand = new SnmpCommander(snmpProperties, this);
    }
    
    /**
     * Creates the gui and layout it.
     */
    private void createUI() {
        // Text fields part
        displayPanel = new JPanel(new GridBagLayout());
        jtfOID = new JTextField();
        jtfOID.setEditable(false);
        addLine("OID", jtfOID);
        jtfOIDNumber = new JTextField();
        addLine("OID number", jtfOIDNumber);
        jtfAccess = new JTextField();
        jtfAccess.setEditable(false);
        addLine("Access", jtfAccess);
        jtfType = new JTextField();
        jtfType.setEditable(false);
        addLine("Type", jtfType);

        setType = new JComboBox();
        setType.addItem("OctetString");
        setType.addItem("Int32");
        addLine("Set type", setType);
        jtfValue = new JTextField();
        addLine("Value", jtfValue);

        jtaDescription = new JTextArea(4, 30);
        //addLine("Description", new JScrollPane(jtaDescription));
        JPanel jpDescription = new JPanel(new BorderLayout());
        jpDescription.add(new JLabel(" Description   "), BorderLayout.WEST);
        jpDescription.add(new JScrollPane(jtaDescription), BorderLayout.CENTER);
        
        // buttons part
        JPanel buttonPanel = new JPanel(new GridLayout(2, 3, 10, 10));
        getButton = new JButton("GET");
        getNextButton = new JButton("GET NEXT");
        setButton = new JButton("SET");
        walkButton = new JButton("WALK");
        stopButton = new JButton("STOP");
        clearButton = new JButton("CLEAR");
        stopButton.setEnabled(false);
        getButton.addActionListener(this);
        getNextButton.addActionListener(this);
        setButton.addActionListener(this);
        walkButton.addActionListener(this);
        stopButton.addActionListener(this);
        clearButton.addActionListener(this);
        buttonPanel.add(getButton);
        buttonPanel.add(getNextButton);
        buttonPanel.add(setButton);
        buttonPanel.add(walkButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(clearButton);

        add(displayPanel, BorderLayout.NORTH);
        add(jpDescription, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        jtfOIDNumber.getDocument().addDocumentListener(this);
    }

    public void changedUpdate(DocumentEvent e){
        oidEdited();
    }
    
    public void insertUpdate(DocumentEvent e) {
        oidEdited();
    }
    
    public void removeUpdate(DocumentEvent e) {
        oidEdited();
    }
    
    private void oidEdited() {
        // The user edited the OID - unless we add any checking here,
        // we'll have to assume the OID is gettable, settable and walkable.
        getButton.setEnabled(true);
        getNextButton.setEnabled(true);
        setButton.setEnabled(true);
        walkButton.setEnabled(true);
    }
    
    /**
     * Add a new line to the panel.
     */
    private void addLine(String description, JComponent component) {

        JLabel label = new JLabel(description);

        // Add to layout
        displayPanel.add(label, new GridBagConstraints(0, rowCount, 1, 1,
          0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
          new Insets(5, 5, 5, 10), 0, 0));
        if (component instanceof JTextArea) {
            displayPanel.add(component, new GridBagConstraints(1, rowCount, 1, 1,
              1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
              new Insets(0, 0, 0, 5), 0, 0));
        } else {
            displayPanel.add(component, new GridBagConstraints(1, rowCount, 1, 1,
              1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
              new Insets(0, 0, 0, 5), 0, 0));
        }
        rowCount++;
    }
    
    /**
     * Handle a change of the selection in the OID Tree.
     */
    public void valueChanged(TreeSelectionEvent event) {
        TreeSelectionModel selectionModel = 
          ((JTree) event.getSource()).getSelectionModel();
        TreePath path = selectionModel.getSelectionPath();
        if (path == null) {
            return;
        }
        DefaultMutableTreeNode treeNode =
          (DefaultMutableTreeNode)(path.getLastPathComponent());
        MibNode node = (MibNode)treeNode.getUserObject();
        
        boolean hasSyntax = node.getSyntax() != null;
        boolean isInTable = node.getOIDString().indexOf("Table") >= 0;
        boolean isSettable = node.getAccessStr().indexOf("rite") >= 0;
        String type = hasSyntax
                    ? node.getSyntax().getDescription()
                    : "OBJECT IDENTIFIER";
        boolean hasValue = hasSyntax
                      && ! (type.indexOf("SEQUENCE") >= 0)
                      && ! (type.indexOf("Entry") >= 0);
                        
        
        // refresh the value of the ControlPanel
        jtfOID.setText(node.getOIDString());
        jtfOIDNumber.setText(node.getNumberedOIDString() + ".0");
        jtfAccess.setText(node.getAccessStr());
        jtfType.setText(type);
        jtfValue.setText("");
        String description = node.getDescription();
        // Trim the enclosing "" when setting the description text.
        jtaDescription.setText(description != null
                            ? description.substring(1, description.length() - 1)
                            : "");
        
        // Enable/disable button states as is appropriate for the selection.
        getButton.setEnabled(hasValue && ! isInTable);
        getNextButton.setEnabled(hasValue);
        setButton.setEnabled(hasValue && isSettable);
        walkButton.setEnabled(hasValue && isInTable);
        
        // Simulate a button click if appropriate.
        if (hasValue) {
            (isInTable ? walkButton : getButton).doClick();
        }
    }
    
    /**
     * This method is invoked when the user click on one of the buttons.
     */
    public void actionPerformed(ActionEvent event) {
        String textSource = ((JButton)event.getSource()).getText();
        if (textSource.equals("GET")) {
            String oid = jtfOIDNumber.getText();
            snmpCommand.get(oid);
        }
        else if (textSource.equals("GET NEXT")) {
            String oid = jtfOIDNumber.getText();
            snmpCommand.getNext(oid);
            jtfOID.setText("");
            jtfAccess.setText("");
            jtfType.setText("");
            jtaDescription.setText("");
            // We do not wish to show the previous OID selected.
            // Ideally, we'd select the correct one,
            // but for now, clearing the selection avoids confusion just fine.
            tree.clearSelection();
        }
        else if (textSource.equals("SET")) {
            String oid = jtfOIDNumber.getText();
            String value = jtfValue.getText();
            int type = setType.getSelectedIndex();
            snmpCommand.set(oid, value, type);
        }
        else if (textSource.equals("WALK")) {
            walking = true;
            stopped = false;
            walkButton.setEnabled(false);
            stopButton.setEnabled(true);
            String oid = jtfOIDNumber.getText();
            walkOID = oid.substring(0, oid.length() - 2);
            walk(oid);
        }
        else if (textSource.equals("STOP")) {
            stopped = true;
        }
        else if (textSource.equals("CLEAR")) {
            console.setText("");
        }
    }
    
    /**
     * Get the next oid and value for the walk procedure.
     */
    public void walk(String oid) {
        snmpCommand.getNext(oid);
    }
    
    /**
     * Stop the walk.
     */
    public void stop() {
        walking = false;
        walkButton.setEnabled(true);
        stopButton.setEnabled(false);
        console.append("\n");
    }
    
    // Answers from the SNMP agent
    /**
     * An error occured with SNMP.
     */
    public void snmpInternalError(SnmpSession snmpSession, int param, 
      SnmpSyntax snmpSyntax) {
        console.append("\nSNMP error\n");
        stop();
    }
    
    /**
     * We received an answer from the SNMP agent.
     */
    public void snmpReceivedPdu(SnmpSession snmpSession, int param, 
                                SnmpPduPacket pdu)
    {
        // check if the message is correct.
        String error = null;
        if (pdu.getCommand() != SnmpPduPacket.RESPONSE) {
            error = "The answer received is incorrect";
        }
        SnmpVarBind answerBind = pdu.getVarBindAt(0);
        SnmpSyntax answer = answerBind.getValue();
        if (! (answer instanceof SnmpOctetString) &&
            ! (answer instanceof SnmpCounter64) &&
            ! (answer instanceof SnmpTimeTicks) &&
            ! (answer instanceof SnmpObjectId) &&
            ! (answer instanceof SnmpInt32))
        {
            error = "Wrong type of answer : " + answer.getClass().getName();
        }
        if (error != null) {
            if (! (answer instanceof SnmpEndOfMibView)) {
                console.append("\nError : " + error + "\n");
            }
            stop();
            return;
        }

        // Refresh the textfields with the new values
        String answerOID = answerBind.getName().toString();

        String answerValue = "";
        if (answer instanceof SnmpOctetString) {
            answerValue = new String(((SnmpOctetString) answer).getString());
        }
        else if (answer instanceof SnmpCounter64 ||
                 answer instanceof SnmpObjectId ||
                 answer instanceof SnmpInt32)
        {
             answerValue = answer.toString();
        }
        
        if (answerValue.equals("")) {
            answerValue = "(empty)";
        }

        if (walking) {
            if (stopped || !answerOID.startsWith(walkOID)) {
                stop();
            }
            else {
                console.append(answerValue + "\n");
                walk(answerOID);
            }
            return;
        }
        
        jtfOIDNumber.setText(answerOID);
        jtfValue.setText(answerValue);
    }
    
    /**
     * The answer didn't come in the expected time.
     */
    public void snmpTimeoutError(SnmpSession snmpSession, 
      SnmpSyntax snmpSyntax) {
        console.append("\nTime out!\n");
        stop();
    }
}
