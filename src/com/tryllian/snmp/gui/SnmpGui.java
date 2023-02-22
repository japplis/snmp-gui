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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.tree.*;

/**
 * SNMP viewer application.
 */
public class SnmpGui extends JFrame {
    JTree mibTree;
    /** 
     * Creates the application
     */
    public SnmpGui(String mibFile) {
        
        setTitle("SNMP GUI (management by numbers)");
        setIconImage(new ImageIcon(getClass().getResource("procman.png")).getImage());
        WindowListener listener = new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                System.exit(0);
            }
        };
        addWindowListener(listener);

        // load the properties
        Properties snmpProperties = new Properties();
        try {
        
            String configDir = System.getProperty("are.configdir");
            if (configDir == null || configDir.equals("")) {
                configDir = ".." + File.separator + ".." + File.separator + 
                  "config";
            }
            File configDirectory = new File(configDir);
            String snmpPropertiesFile = null;
            // Create the components
            if (mibFile.equals("") && configDirectory.exists()) {
                mibFile = configDirectory.getCanonicalPath() +
                  File.separator + "TRYLLIAN-HABITAT.mib";
                snmpPropertiesFile = configDirectory.getCanonicalPath() +
                  File.separator + "snmp.properties";
            } else {
                mibFile = "TRYLLIAN-HABITAT.mib";
                snmpPropertiesFile = "snmp.properties";
            }
            
            FileInputStream fis = new FileInputStream(snmpPropertiesFile);
            snmpProperties.load(fis);
            fis.close();
        }
        catch (IOException ioe) {
            // Ignore them
            ioe.printStackTrace();
        }
        
        // Create the element of the UI.
        JTextArea walkArea = new JTextArea() {
            public void append(String text) {
                super.append(text);
                // scroll to the end
                setCaretPosition(getText().length());
            }
        };
        setJMenuBar(new SnmpGuiMenuBar(this, walkArea, snmpProperties));
        mibTree = new JTree(new OIDTreeModel(mibFile));
        
        // Expand the nodes that only have one child (that is, the tables).
        TreeModel model = mibTree.getModel();
        List paths = new ArrayList();
        paths.add(new TreePath(model.getRoot()));
        while (! paths.isEmpty()) {
            TreePath path = (TreePath) paths.get(0);
            Object parent = path.getLastPathComponent();
            for (int c = 0; c < model.getChildCount(parent); c++) {
                Object child = model.getChild(parent, c);
                if (model.getChildCount(child) == 1) {
                    TreePath childPath = path.pathByAddingChild(child);
                    paths.add (childPath);
                    try {
                        mibTree.expandPath(childPath);
                    }
                    catch (Exception e) {
                        break;
                    }
                }
            }
            paths.remove(0);
        }
        
        
        ControlPanel controlPanel = 
          new ControlPanel(mibTree, snmpProperties, walkArea);

        // Add the UI elements to the frame.
        JSplitPane splitPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPanel.setDividerLocation(300);
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(controlPanel, BorderLayout.NORTH);
        rightPanel.add(new JScrollPane(walkArea), BorderLayout.CENTER);
        splitPanel.setLeftComponent(new JScrollPane(mibTree));
        splitPanel.setRightComponent(rightPanel);
        getContentPane().add(splitPanel);

        pack();
        
        // center the frame
        Dimension dimFrame = getPreferredSize();
        Dimension dimScreen = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(dimScreen.width / 2 - dimFrame.width / 2, 
          dimScreen.height / 2 - dimFrame.height / 2);
        setVisible(true);
    }
    
    public Dimension getPreferredSize() {
        return new Dimension(800, 600);
    }
    
    public JTree getMibTree() {
        return mibTree;
    }
    
    /**
     * Starts the Snmp Gui application
     *
     * @param args the command line arguments. This application can have one
     * argument : the location of the MIB file.
     * The others settings can be configure in the snmp.properties file.
     */
    public static void main(String[] args) {
        new SnmpGui(args.length != 0 ? args[0] : "");
    }
}
