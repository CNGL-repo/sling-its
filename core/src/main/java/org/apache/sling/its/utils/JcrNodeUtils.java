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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.jcr.resource.JcrResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utilities class to support javax.jcr.Node.
 */
public final class JcrNodeUtils
{
    /** Logger instance. */
    private static final Logger LOG = LoggerFactory.getLogger(JcrNodeUtils.class);

    /**
     * Get the first child node.
     *
     * @param session
     *          the current session.
     * @param parentPath
     *          the path of the parent node.
     * @return null if no child found; otherwise, return first child.
     */
    public static Node getFirstChild(final Session session, final String parentPath)
    {
        try
        {
            if (session.nodeExists(parentPath))
            {
                final Node parentNode = session.getNode(parentPath);
                final NodeIterator children = parentNode.getNodes();
                if (children.hasNext())
                {
                    return children.nextNode();
                }
            }
        }
        catch (final PathNotFoundException e)
        {
            LOG.error("The following Path was not found: " + parentPath
                + "\nStack Trace :", e);
        }
        catch (final RepositoryException e)
        {
            LOG.error("Failed to access repository. Stack Trace: ", e);
        }

        return null;
    }

    /**
     * Create a node or get the existing node.
     *
     * @param session
     *          Session
     * @param absPath
     *          absolute path
     * @return the node to the absolute path.
     */
    public static Node createNode(final Session session, final String absPath)
    {
        Node node = null;
        try
        {
            node = JcrResourceUtil.createPath(absPath, "nt:unstructured",
                "nt:unstructured", session, false);
            if (node.hasNodes())
            {
                removeChildren(session, absPath);
            }
            session.save();
        }
        catch (final RepositoryException e)
        {
            LOG.error("Failed to access repository. Stack Trace: ", e);
        }
        return node;
    }

    /**
     * Remove all child nodes below parentPath.
     *
     * @param session
     *           current session
     * @param parentPath
     *           parent path of the nodes that needs to be removed
     */
    public static void removeChildren(final Session session, final String parentPath)
    {
        try
        {
            if (session.nodeExists(parentPath))
            {
                final Node parentNode = session.getNode(parentPath);
                final NodeIterator children = parentNode.getNodes();
                while (children.hasNext())
                {
                    final Node child = children.nextNode();
                    child.remove();
                }
                session.save();
            }
        }
        catch (final PathNotFoundException e)
        {
            LOG.error("The following Path was not found: " + parentPath
                + "\nStack Trace :", e);
        }
        catch (final RepositoryException e)
        {
            LOG.error("Failed to access repository. Stack Trace: ", e);
        }
    }

    /**
     * Private constructor to prevent instantiation of this class.
     */
    private JcrNodeUtils()
    {
        throw new AssertionError("This class is not ment to be instantiated.");
    }
}
