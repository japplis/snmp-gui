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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JOptionPane;

/**
 * Dialog that ask the user for the basic SNMP properties.
 *
 * @author Anthony Goubard
 * @version 1.0 5-7-2002
 */
public class PropertiesWindow {
    
    private JTextField server;
    private JTextField port;
    private JTextField publicPassword;
    private JTextField privatePassword;
    
    /** 
     * Show the dialogue with the SNMP Properties and save the properties.
     * @param mainFrame the top main frame
     * @param properties the SNMP properties
     */
    public PropertiesWindow(JFrame mainFrame, Properties properties) {
        
        // Create the message
        Object[] message = new Object[8];
        message[0] = "Snmp server : ";
        server = new JTextField(properties.getProperty("server"));
        message[1] = server;
        message[2] = "Snmp port : ";
        port = new JTextField(properties.getProperty("port"));
        message[3] = port;
        message[4] = "Public password : ";
        publicPassword = new JTextField(properties.getProperty("public"));
        message[5] = publicPassword;
        message[6] = "Private password : ";
        privatePassword = new JTextField(properties.getProperty("private"));
        message[7] = privatePassword;
        
        Object[] options = { "OK", "CANCEL" };
        
        // Show the dialogue
        int result = JOptionPane.showOptionDialog(mainFrame, message, 
          "Snmp properties", JOptionPane.DEFAULT_OPTION, 
          JOptionPane.QUESTION_MESSAGE, 
          null, options, options[0]);
        
        if (result == 1 || result == JOptionPane.CANCEL_OPTION ||
          result == JOptionPane.CLOSED_OPTION) {
            return;
        }
        
        // Set the new properties
        properties.setProperty("server", server.getText());
        properties.setProperty("port", port.getText());
        properties.setProperty("public", publicPassword.getText());
        properties.setProperty("private", privatePassword.getText());
        
        // Save the properties in the file
        saveProperties(properties, mainFrame, true);
    }
    
    public static void saveProperties(Properties snmpProperties, JFrame mainFrame, boolean showRestart) {
        try {
            String configDir = System.getProperty("are.configdir");
            if (configDir == null || configDir.equals("")) {
                configDir = ".." + File.separator + ".." + File.separator + 
                  "config";
            }
            File configDirectory = new File(configDir);
            String snmpPropertiesFile = null;
            if (configDirectory.exists()) {
                snmpPropertiesFile = configDirectory.getCanonicalPath() +
                  File.separator + "snmp.properties";
            } else {
                snmpPropertiesFile = "snmp.properties";
            }

            FileOutputStream propertiesOutputStream = 
              new FileOutputStream(snmpPropertiesFile);
            snmpProperties.store(propertiesOutputStream, "SNMP properties file.");
            propertiesOutputStream.close();
            if (showRestart) {
                JOptionPane.showMessageDialog(mainFrame,
                  "SNMP properties saved\n" +
                  "You need to restart the application to use the new settings", 
                  "Warning",
                  JOptionPane.WARNING_MESSAGE);
            }
        } catch (IOException ioe) {
            JOptionPane.showMessageDialog(mainFrame,
              "Couldn't save the properties file\n" +
              ioe.getMessage(), "Error",
              JOptionPane.ERROR_MESSAGE);
        }
    }
}
