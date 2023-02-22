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
 * A class creating and handling a Swing dialog to perform an agent action.
 */
public class AgentActionWindow implements SnmpHandler {
    
    final String NAME_WALK_OID  = ".1.3.6.1.4.1.9727.1.4.1.1.1";
    final String OID_WALK_OID   = ".1.3.6.1.4.1.9727.1.4.1.1.2";

    private String lastResult = "";

    
    public AgentActionWindow(JFrame mainFrame,
                             Properties properties,
                             JTextArea console)
    {
        // Prepare a nice Agent Action dialog.
        
        // The agents are displayed by name, as this is easier for us humans.
        // The mapping of name to OID is done internally (by index).
        List names = getAgentNames(properties);
        List oids  = getAgentOIDs(properties);
        
        names.add(0, "Select an agent here");
        oids.add(0, "Filler" /* to keep name and OID indices the same */);
        final JComboBox agentMenu = new JComboBox(names.toArray());
        
        final JComboBox actionMenu = new JComboBox(new Object[] {
            "Select an action here",
            "Send Message",
            "Move",
            "Checkpoint",
            "Suspend",
            "Die",
            "Clone",
            "Update",
            "Register",
            "Unregister",
        });
        
        final JLabel paramLabel = new JLabel("Parameter: " );
        
        final JTextField paramText = new JTextField(
            "Enter the action parameter");
        
        final JCheckBox displayResult = new JCheckBox(
            "Display the action result in the console");
        
        actionMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int action = actionMenu.getSelectedIndex();
                switch (action) {
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                        paramLabel.setEnabled(false);
                        paramText.setEnabled(false);
                        paramText.setText("(no parameter needed)");
                    break;
                    default:
                        paramLabel.setEnabled(true);
                        paramText.setEnabled(true);
                }
                switch (action) {
                    case 1:
                        paramText.setText("<perf> <lang> <subj> [<args>]");
                    break;
                    case 2:
                        paramText.setText("<habitatname>");
                    break;
                    case 7:
                        paramText.setText("<relative path to DNA file>");
                    break;
                    case 8:
                        paramText.setText("<servicename>");
                    break;
                    case 9:
                        paramText.setText("<servicename>");
                    break;
                }
                switch (action) {
                    default:
                        displayResult.setSelected(true);
                        displayResult.setEnabled(true);
                    break;
                    case 5:
                        displayResult.setSelected(false);
                        displayResult.setEnabled(false);
                    break;
                }
            }
        });
        
        final Object[] messages = new Object[] {
            "The easy way to perform an agent action -",
            "for those who don't master straight SNMP.",
            " ", /* for layout only */
            new JLabel("Agent: "),
            agentMenu,
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
        
        // We got all the dialog stuff now.
        // Display the dialog and get the result.
        
        int result = JOptionPane.showOptionDialog(
            mainFrame,
            messages,
            "Agent Action Dialog",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null, /* icon */
            options,
            options[0]
        );
        
        int agent  = agentMenu.getSelectedIndex();
        int action = actionMenu.getSelectedIndex();
        
        if (result != 0 || agent == 0 || action == 0) {
            return;
        }
                
        // Create OIDs for the command, paramter and result,
        // based on the OID we got from mapping the selected name to an OID.
        StringBuffer temp = new StringBuffer((String) oids.get(agent));
        final int digitToBeReplacedPosition = 26;
        temp.setCharAt(digitToBeReplacedPosition, '6');
        final String COMMAND_OID = new String(temp);
        temp.setCharAt(digitToBeReplacedPosition, '7');
        final String PARAM_OID   = new String(temp);
        temp.setCharAt(digitToBeReplacedPosition, '8');
        final String RESULT_OID  = new String(temp);
        
        SnmpCommander commander = new SnmpCommander(properties, this);
        
        // Clear the previous result (so we can test on the result being empty).
        synchronized(this) {
            commander.set(RESULT_OID, "", 0);
            lastResult = "";
        }
        
        // Set the action parameter, if required.
        boolean requiresParameter = action == 1
                                 || action == 2
                                 || action == 7
                                 || action == 8
                                 || action == 9;
        if (requiresParameter) {
            String parameter = paramText.getText();
            commander.set(PARAM_OID, parameter, 0);
        }
        
        // Set the command. This makes the SNMP Agent perform the action.
        commander.set(COMMAND_OID, Integer.toString(action), 1);
        
        // Wait for and display the result, if desired.
        if (displayResult.isSelected()) {
            
            // Get the result until it is not empty (we did make it so).
            // Note: this would fail horribly when tried for a nonexistent
            //       result, such as one for an agent we just asked to die.
            synchronized (this) {
                while ("".equals(lastResult)) {
                    commander.get(RESULT_OID);
                    try {
                        this.wait(3000);
                    } catch (InterruptedException e) {}
                }
            }
            console.append(lastResult + "\n\n");
        }
    }
    
    
    private List getAgentNames(Properties properties) {
        SnmpCommander commander = new SnmpCommander(properties, null);
        Handler handler = new Handler(commander, NAME_WALK_OID);
     
        // return handler.getList();
        
        // The above commented-out return statement would be fine, if only
        // we wouldn't have to work around a Swing issue here first...
        // If the name in the list is not unique, the index of the first
        // occurrence of the name in the popup menu will be returned,
        // which is not necessarily the one we actually selected.
        // So we'd better name sure the names *are* unique - even if agent
        // names are not.
        // To do this invisibly to the user, we add spaces after the name
        // when needed to make the name unique.
        List result = handler.getList();
        int i = 0;
        for (ListIterator it = result.listIterator(); it.hasNext(); i++) {
            String name = (String) it.next();
            while (result.indexOf(name) != i) {
                // (still) not unique... add a(nother) space to the name...
                name = name + " ";
                it.set(name);
            }
        }
        return result;
    }
    
    
    private List getAgentOIDs(Properties properties) {
        SnmpCommander commander = new SnmpCommander(properties, null);
        Handler handler = new Handler(commander, OID_WALK_OID);
        return handler.getList();
    }
    
    
    
    // SnmpHandler interface method
    public void snmpInternalError(SnmpSession snmpSession,
                                  int param, 
                                  SnmpSyntax snmpSyntax)
    {
        synchronized (this) {
            lastResult = "Internal error";
            this.notifyAll();            
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
            this.notifyAll();
        }
    }
    
    
    // SnmpHandler interface method
    public void snmpTimeoutError(SnmpSession snmpSession, 
                                 SnmpSyntax snmpSyntax)
    {
        synchronized (this) {
            lastResult = "timeout";
            this.notifyAll();            
        }
    }
    
    /**
     * A convenience SnmpHandler to get a row in a table returned as a List.
     */
    private class Handler implements SnmpHandler {
        
        private SnmpCommander commander;
        private String startOID;
        private List result = new ArrayList();
        
        /**
         * @param commander the SnmpCommander instance to use
         * @param startOID the OID to start the walk from (without ".0").
         */
        public Handler(SnmpCommander commander, String startOID) {
            this.commander = commander;
            this.startOID = startOID;
        }
        
        /**
         * Main method of this class. Sets this handler as the handler for
         * the commander, initiates getting the result list, waits until it's
         * filled and returns it.
         *
         * @return the List as aquired by succesive getNext requests
         */
        public List getList() {
            commander.setHandler(this);
            synchronized (result) {
                result.clear();
            }
            // Note: assumes the list will always be non-empty for success.
            // This is true, as the SNMP Agent itself is present.
            while (result.isEmpty()) {
                commander.getNext(startOID + ".0");
                synchronized (result) {
                    try {
                        result.wait(3000);
                    } catch (InterruptedException e) {}
                    
                }
            }
            return result;
        }
        
        
        /**
         * Handles a received getNext reply by adding an item to the list
         * and issuing a getNext request (where appropriate).
         * When the list has been filled, notifies the waiting method.
         */
        public void snmpReceivedPdu(SnmpSession snmpSession,
                                        int param, 
                                        SnmpPduPacket pdu)
        {
            synchronized (result) {
                if (pdu.getCommand() != SnmpPduPacket.RESPONSE) {
                    result.add("<non-response message reseived>");
                    result.notifyAll();
                    return;
                }
                SnmpVarBind answerBind = pdu.getVarBindAt(0);
                String answerOID = answerBind.getName().toString();                    
                if (! answerOID.startsWith(startOID)) {
                    result.notifyAll();
                    return;
                }
                SnmpSyntax answer = answerBind.getValue();
                String answerValue;
                if (answer instanceof SnmpEndOfMibView) {
                    result.notifyAll();
                    return;
                }
                else if (answer instanceof SnmpOctetString) {
                    answerValue
                        = new String(((SnmpOctetString) answer).getString());
                }
                else {
                    answerValue = answer.toString();
                }
                result.add(answerValue);
                commander.getNext(answerOID);                
            }
        }
            
        public void snmpTimeoutError(SnmpSession snmpSession, 
                                         SnmpSyntax snmpSyntax)
        {
            synchronized (result) {
                result.add("<timeout>");
                result.notifyAll();
            }
        }
            
        public void snmpInternalError(SnmpSession snmpSession,
                                          int param, 
                                          SnmpSyntax snmpSyntax)
        {
            synchronized (result) {
                result.add("<internal error>");
                result.notifyAll();
            }
        }
    }
}
