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

import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import org.opennms.protocols.snmp.*;

/**
 * A class creating and handling a Swing dialog to perform a habitat action.
 */
public class HabitatActionWindow implements SnmpHandler {
    
    final String HSA_ACTION_OID = "1.3.6.1.4.1.9727.1.1.1.4.";
    // Should be equal to HabitatActionAgent.HABITAT_ACTION_OID
    // but I don't want the GUI to depend upon the agent.
    // XXX Figure out a way to get this number from the MIB file
    //     in a politically correct way.
    
    final String COMMAND_OID = HSA_ACTION_OID + "1.0";
    final String PARAM_OID =   HSA_ACTION_OID + "2.0";
    final String RESULT_OID =  HSA_ACTION_OID + "3.0";
    
    private String lastResult = "";
    
    public HabitatActionWindow(JFrame mainFrame,
                               Properties properties,
                               JTextArea console)
    {
        // Create a dialog allowing to set the action and parameter.
        
        final JComboBox actionMenu = new JComboBox(new Object[] {
            "Select an action",
            "Reload Permissions",
            "Resolve JNDI",
            "Create Agent",
            "Habitat Shutdown"
        });
        
        final JLabel paramLabel = new JLabel("Parameter: " );

        final JTextField paramText = new JTextField(
            "Enter the action parameter");
        
        final JCheckBox displayResult = new JCheckBox(
            "Display the result in the console");
        actionMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int action = actionMenu.getSelectedIndex();
                displayResult.setSelected(action == 2 || action == 3);
                switch (action) {
                    case 1:
                    case 4:
                        paramLabel.setEnabled(false);
                        paramText.setEnabled(false);
                        paramText.setText("(no parameter needed)");
                    break;
                    case 2:
                        paramLabel.setEnabled(true);
                        paramText.setEnabled(true);
                        paramText.setText("adk/.:/...");
                        break;
                    case 3:
                        paramLabel.setEnabled(true);
                        paramText.setEnabled(true);
                        paramText.setText("<relative path to DNA file>");
                        paramText.selectAll();
                    break;
                }
            }
        });
        
        final Object[] messages = new Object[] {
            "The easy way to perform a habitat action -",
            "for those who don't master straight SNMP.",
            " ", /* for layout only */
            new JLabel("Action: "),
            actionMenu,
            " ", /* for layout only */
            paramLabel,
            paramText,
            " ", /* for layout only */
            displayResult,
            " ", /* for layout only */
        };
        
        final Object[] options = new String[] {
            "Perform Action",
            "Cancel"
        };
        
        // Display the dialog and get the result.
        
        int result = JOptionPane.showOptionDialog(
            mainFrame,
            messages,
            "Habitat Action Dialog",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null, /* icon */
            options,
            options[0]
        );
        
        int action = actionMenu.getSelectedIndex();
        
        if (result != 0 || action == 0) {
            return;
        }
        
        // Input has been received, and an action must be performed.
        
        SnmpCommander commander = new SnmpCommander(properties, this);
        
        // Clear the previous result (so we can test on the result being empty).
        synchronized (this) {
            commander.set(RESULT_OID, "", 0);
            lastResult = "";            
        }
        
        // Set the action parameter, if required.
        boolean requiresParameter = action == 2 || action == 3;
        if (requiresParameter) {
            String parameter = paramText.getText();
            commander.set(PARAM_OID, parameter, 0);
        }
        
        // Set the command. This makes the SNMP Agent perform the action.
        commander.set(COMMAND_OID, Integer.toString(action), 1);
            
        if (displayResult.isSelected()) {
            
            // Get the result until it is not empty.
            synchronized (this) {
                while ("".equals(lastResult)) {
                    commander.get(RESULT_OID);
                    try {
                        wait(3000);
                    } catch (InterruptedException e) {}
                }
            }
            /*
            StringTokenizer tokenizer = new StringTokenizer(lastResult);
            String[] message = new String[tokenizer.countTokens()];
            for (int i = 0; tokenizer.hasMoreTokens(); i++) {
                message[i] = tokenizer.nextToken();
            }
            JOptionPane.showMessageDialog(
                mainFrame,
                message,
                "Habitat Action Result",
                JOptionPane.INFORMATION_MESSAGE
            );
             */
            console.append(lastResult + "\n\n");
        }
    }

    
    // SnmpHandler interface method
    public void snmpInternalError(SnmpSession snmpSession,
                                  int param, 
                                  SnmpSyntax snmpSyntax)
    {
        synchronized (this) {
            lastResult = "Internal error";
            notifyAll();            
        }
    }

    
    // SnmpHandler interface method
    public void snmpReceivedPdu(SnmpSession snmpSession,
                                int param, 
                                SnmpPduPacket pdu)
    {        
        // check if the message is correct.
        String error = null;
        if (pdu.getCommand() != SnmpPduPacket.RESPONSE) {
            error = "The answer received is incorrect";
        }
        SnmpVarBind answerBind = pdu.getVarBindAt(0);
        SnmpSyntax answer = answerBind.getValue();
        if (! (answer instanceof SnmpOctetString)) {
            error = "Wrong type of answer : " + answer.getClass().getName();
        }
        synchronized (this) {                
            if (error != null) {
                lastResult = error;
            } else {
                lastResult = new String(((SnmpOctetString) answer).getString());
            }
            notifyAll();
        }
    }

    
    // SnmpHandler interface method
    public void snmpTimeoutError(SnmpSession snmpSession, 
                                 SnmpSyntax snmpSyntax)
    {
        synchronized (this) {
            lastResult = "timeout";
            notifyAll();            
        }
    }
}
