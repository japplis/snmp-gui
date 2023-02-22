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

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.File;
import java.util.Properties;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;

/**
 * Menubar for the SNMP GUI application.
 */
public class SnmpGuiMenuBar extends JMenuBar {

    /**
     * Create the menu bar of the Snmp Gui application
     */
    public SnmpGuiMenuBar(final SnmpGui mainFrame, 
      final JTextArea console, final Properties snmpProperties) {

        JMenu file = new JMenu("File");
        JMenu action = new JMenu("Action");
        JMenu help = new JMenu("Help");

        // ------------  File  ------------
        // Edit the snmp properties
        JMenuItem openMib = new JMenuItem("Open MIB...");
        file.add(openMib);
        openMib.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    String previousMibFile = snmpProperties.getProperty("mibfile");
                    File parentDir = new File(previousMibFile).getAbsoluteFile().getParentFile();
                    JFileChooser mibChooser = new JFileChooser(parentDir);
                    int returnVal = mibChooser.showOpenDialog(mainFrame);
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        String selectedFile = mibChooser.getSelectedFile().getAbsolutePath();
                        snmpProperties.setProperty("mibfile", selectedFile);
                        mainFrame.getMibTree().setModel(new DefaultTreeModel(new OIDTreeModel(selectedFile)));
                        PropertiesWindow.saveProperties(snmpProperties, mainFrame, false);
                    }
                }
            }
        );

        // Edit the snmp properties
        JMenuItem properties = new JMenuItem("Edit properties...");
        file.add(properties);
        properties.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    new PropertiesWindow(mainFrame, snmpProperties);
                }
            }
        );

        // Start the SNMP deamon on the port getProperty("port") + 1
        JMenuItem trapDeamon = new JMenuItem("Start trap deamon");
        file.add(trapDeamon);
        trapDeamon.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    JOptionPane.showMessageDialog(mainFrame,
                      "The trap message will be shown in the console");
                    int port = Integer.parseInt(
                      snmpProperties.getProperty("port", "161"));
                    TrapDeamon deamon = 
                      new TrapDeamon(console, port + 1);
                    deamon.startServer();
                }
            }
        );

        file.addSeparator();
        
        // Exit the application
        JMenuItem exit = new JMenuItem("Quit");
        file.add(exit);
        exit.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    System.exit(0);
                }
            }
        );

        // ------------  Action  ------------
        JMenuItem agentAction = new JMenuItem("Agent action...");
        action.add(agentAction);
        agentAction.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    new AgentActionWindow(mainFrame, snmpProperties, console);
                }
                
            }
        );
        JMenuItem habitatAction = new JMenuItem("Habitat action...");
        action.add(habitatAction);
        habitatAction.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    new HabitatActionWindow(mainFrame, snmpProperties, console);
                }
                
            }
        );
        
        // ------------  Help  ------------
        // Help of this tool
        JMenuItem snmpGuiHelp = new JMenuItem("Snmp Gui Help");
        snmpGuiHelp.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    // In the daily build the are.home is set to an odd
                    // value ./..
                    String areHome = System.getProperty("are.home");
                    String helpPage = null;
                    if (areHome != null && new File(areHome).exists()) {
                        helpPage = new File(areHome).getAbsolutePath() + 
                          File.separator + "docs" +  File.separator +
                          "devguide" + File.separator + "ch13.html";
                    } else {
                        helpPage = "README.html";
                    }
                    BrowserControl.displayURL(helpPage); //#snmpgui
                }
            }
        );
        help.add(snmpGuiHelp);
        
        // Tryllian web site
        JMenuItem tryllianWebSite = new JMenuItem("Tryllian Solutions Website");
        help.add(tryllianWebSite);
        tryllianWebSite.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    try {
                        BrowserLauncher.openURL("http://www.tryllian.com/");
                    }
                    catch (IOException ioe) {
                    }
                }
            }
        );
        help.addSeparator();
        
        // Show the about dialogue
        JMenuItem about = new JMenuItem("About...");
        about.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    JOptionPane.showMessageDialog(mainFrame,
                      " SNMP GUI - ADK V3.2\n"+
                      " \u00A9 Copyright 2001-2004 Tryllian Solutions BV.",
                      "About",
                      JOptionPane.INFORMATION_MESSAGE);
                }
            }
        );
        help.add(about);

        add(file);
        //add(action);
        add(help);
    }
}
