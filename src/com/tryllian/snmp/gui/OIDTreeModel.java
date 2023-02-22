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

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Enumeration;

import pt.ipb.agentapi.mibs.MibModule;
import pt.ipb.agentapi.mibs.MibNode;
import pt.ipb.agentapi.OID;

/**
 * Create a tree model of the MIB file
 */
public class OIDTreeModel extends DefaultMutableTreeNode {

    /**
     * Create a TreeModel (actually a DefaultMutableTreeNode) from a MIB file
     */
    public OIDTreeModel(String filename) {
        try {
            // use the parse included in the agentapi library
            MibModule module = MibModule.load(filename);
            setUserObject(module);
            addMibNode(this, module.getRoot());
            //addMibNode(this, module.getNode(new OID(".1.3.6.1.4.1.9727.1.")));
        }
        catch (Exception mibex) {
            mibex.printStackTrace();
        }
    }
    
    /**
     * Add a new node to a branch
     * @param branch the parent branch to set the node and add the children
     * @param node the node to add to this branch
     * @return the branch created with it's UserObject and its children (if any)
     */
    DefaultMutableTreeNode addMibNode(DefaultMutableTreeNode branch, MibNode node) {
        if (!node.isLeaf()) {
            branch.setUserObject(node);
            for (Enumeration e = node.children(); e.hasMoreElements(); ) {
                MibNode child = (MibNode) e.nextElement();
                branch.add(addMibNode(new DefaultMutableTreeNode(), child));
            }
        }
        else {
            branch.setUserObject(node);
        }
        return branch;
    }
}
