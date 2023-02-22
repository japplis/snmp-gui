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

import org.opennms.protocols.snmp.*;

import java.awt.event.*;
import java.io.IOException;
import java.net.InetAddress;

import javax.swing.*;

/**
 * Listens to the traps and add them in the console.
 *
 * @author Anthony Goubard
 */
public class TrapDeamon {

    private int port;
    private JTextArea console;
    private TrapListener trapReceiver;
    
    /** 
     * Start the SNMP Trap deamon
     * @param console the text area where the error and the trap received 
     * should be printed.
     * @param port the port number that the trap deamon should listen to.
     */
    public TrapDeamon(JTextArea console, int port) {
        this.console = console;
        this.port = port;
        startServer();
    }
    
    /**
     * Starts the server that will listen to the traps
     */
    public void startServer() {
        try {
            trapReceiver = new TrapListener();
            SnmpTrapSession session = new SnmpTrapSession(trapReceiver, port);
        }
        catch (IOException ioe) {
            console.append(
              "\nTRAP ERROR : Can't open the session with localhost : " + 
              ioe.getMessage() + "\n");
        }
    }
    
    /**
     * Listener of the traps
     */
    class TrapListener implements SnmpTrapHandler {
        
        // SNMP v2
        public void snmpReceivedTrap(SnmpTrapSession session, InetAddress agent,
          int port, SnmpOctetString community, SnmpPduPacket pdu) {
            SnmpSyntax time = pdu.getVarBindAt(0).getValue();
            SnmpSyntax text = pdu.getVarBindAt(1).getValue();
            String textValue = new String(((SnmpOctetString) text).getString());
            console.append("\nTRAP RECEIVED : " + text + " at " + time + "\n");
        }
        
        // SNMP v1
        public void snmpReceivedTrap(SnmpTrapSession session, InetAddress agent,
          int port, SnmpOctetString community, SnmpPduTrap pdu)  {
            console.append("\nSNMP V1 RECEIVED : " + 
              pdu.getVarBindAt(0).getValue() + "\n");
        }

        // Error
        public void snmpTrapSessionError(SnmpTrapSession session, 
          int error, Object ref) {
            console.append("\nSNMP ERROR : " + ref + "\n");
        }
    }

    /**
     * You can start also the application in stand alone mode.
     * @param args You can set a parameter which is the trap port the deamon
     * should listen to. (the default is 162).
     */
    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 162;
        JFrame frame = new JFrame();
        JTextArea console = new JTextArea(10, 80);
        TrapDeamon deamon = new TrapDeamon(console, port);
        frame.getContentPane().add(new JScrollPane(console));
        frame.pack();
        frame.setVisible(true);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                System.exit(0);
            }
        });
    }
}
