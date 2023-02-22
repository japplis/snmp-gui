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

import java.io.IOException;
import java.net.*;
import java.util.Properties;

import org.opennms.protocols.snmp.*;

/**
 * This class has the SnmpSession and communicates with the SNMP agent.
 */
public class SnmpCommander {
    
    // The session to send the snmp get/getnext requests.
    // The given listener will receive the results.
    protected SnmpSession session;
    
    // The session to send the snmp set requests.
    // This is handled transparantly for the given listener.
    protected SnmpSession session2;

    private Properties snmpProperties;

    private SnmpHandler snmpListener;
    
    /** 
     * Create a new SnmpCommander
     * @param snmpProperties the properties to use for the SnmpSession.
     * @param listener the class that should receive the answers.
     */
    public SnmpCommander(Properties snmpProperties, SnmpHandler listener) {
        this.snmpProperties = snmpProperties;
        snmpListener = listener;
        createConnection();
    }
    
    
    public void setHandler(SnmpHandler listener) {
        this.snmpListener = listener;
        session.setDefaultHandler(listener);
    }

    
    private void createConnection() {
        // make the connection with the server
        try {
            // Get the properties for the properties file or use the default ones
            InetAddress server = InetAddress.getByName(
              snmpProperties.getProperty("server", "localhost"));
            int port = 
              Integer.parseInt(snmpProperties.getProperty("port", "161"));
            String publicPassword = 
              snmpProperties.getProperty("public", "public");
            String privatePassword = 
              snmpProperties.getProperty("private", "tryllian");
            
            // Create the session to send ASN messages
            session = new SnmpSession(server);
            session.getPeer().setPort(port);
            session.getPeer().setRetries(2);
            session.getPeer().setTimeout(5000);
            SnmpParameters parameters = new SnmpParameters();
            parameters.setReadCommunity(publicPassword);
            parameters.setWriteCommunity(privatePassword);
            session.getPeer().setParameters(parameters);
            session.setDefaultHandler(snmpListener);

            // This one is used for the ASN set messages
            session2 = new SnmpSession(server);
            session2.getPeer().setPort(port);
            session2.getPeer().setParameters(parameters);
            session2.getPeer().setRetries(2);
            // Ignore the answers for the set
            session2.setDefaultHandler(new SnmpHandler() {
                public void snmpInternalError(SnmpSession snmpSession, int param, 
                  SnmpSyntax snmpSyntax) {
                }
                public void snmpReceivedPdu(SnmpSession snmpSession, int param, 
                  SnmpPduPacket pdu) {
                }
                public void snmpTimeoutError(SnmpSession snmpSession, 
                  SnmpSyntax snmpSyntax) {
                }
            });
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
    
    /**
     * Get the value of an oid.
     */
    public void get(String oid) {
        SnmpVarBind oidBind = new SnmpVarBind(new SnmpObjectId(oid));
        SnmpVarBind[] oids = {oidBind};
        SnmpPduPacket packet = new SnmpPduRequest(SnmpPduPacket.GET, oids);
        session.send(packet);
    }
    
    /**
     * Get the next value of the oid.
     */
    public void getNext(String oid) {
        SnmpVarBind oidBind = new SnmpVarBind(new SnmpObjectId(oid));
        SnmpVarBind[] oids = {oidBind};
        SnmpPduPacket packet = new SnmpPduRequest(SnmpPduPacket.GETNEXT, oids);
        session.send(packet);
    }

    /**
     * Set a new value of the specified oid.
     */
    public void set(String oid, String value, int type) {
        SnmpSyntax argumentValue = null;
        if (type == 0) {
          argumentValue = new SnmpOctetString(value.getBytes());
        } 
        else if (type == 1) {
            argumentValue = new SnmpInt32(Integer.parseInt(value));
        }
        SnmpVarBind argumentBind = 
          new SnmpVarBind(new SnmpObjectId(oid), argumentValue);
        SnmpVarBind[] oids = {argumentBind};
        SnmpPduPacket packet = new SnmpPduRequest(SnmpPduPacket.SET, oids);
        session2.send(packet);
    }
}
