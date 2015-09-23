/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.sling.its.utils;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A utilities class to support org.w3c.dom.Node.
 */
public final class XmlNodeUtils
{
    /**
     * Get the a specific child node from its parent node by matching the local
     * name. If not found, return null.
     *
     * @param parentNode
     *          the parent node.
     * @param localNodeName
     *          the local name of the node.
     * @return the node with the matching local name; otherwise, null if not
     * found.
     */
    public static Node getChildNodeByLocalName(final Node parentNode,
        final String localNodeName)
    {
        final NodeList children = parentNode.getChildNodes();
        for (int i = 0; i < children.getLength(); i++)
        {
            final Node child = children.item(i);
            if (child.getLocalName() != null)
            {
                if (child.getLocalName().equals(localNodeName))
                {
                    return child;
                }
            }
        }
        return null;
    }

    /**
     * Private constructor to prevent instantiation of this class.
     */
    private XmlNodeUtils()
    {
        throw new AssertionError("This class is not ment to be instantiated.");
    }
}
