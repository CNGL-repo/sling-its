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
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;

import net.sf.okapi.common.Namespaces;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.its.constants.SlingItsConstants;
import org.apache.sling.jcr.resource.JcrResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utilities class to support anything related to the global rules.
 */
public final class ItsRulesUtils
{
    /** Logger instance. */
    private static final Logger LOG = LoggerFactory.getLogger(ItsRulesUtils.class);

    /**
     * Create a global rules node below the root element if it doesn't exist.
     * Need to use rootElement so you don't by accident traverse other pages'
     * head.
     *
     * @param session
     *         current session
     * @param path
     *         the root path of the page
     */
    public static void createItsRulesNode(final Session session, final String path)
    {
        try
        {
            final Node rootElement = JcrNodeUtils.getFirstChild(session, path);
            if (rootElement != null)
            {
                final NodeIterator headNodes = JcrResourceUtil.query(
                    session,
                    "SELECT * FROM [nt:base] as t WHERE ISDESCENDANTNODE(["
                        + rootElement.getPath() + "]) AND name(t) LIKE 'head%'",
                    Query.JCR_SQL2).getNodes();
                Node headNode = null;
                if (headNodes.hasNext())
                {
                    headNode = headNodes.nextNode();
                }
                else
                {
                    final NodeIterator rootElementChildren = session.getNode(
                        rootElement.getPath()).getNodes();
                    if (rootElementChildren.hasNext())
                    {
                        headNode = rootElement.addNode("head(1)", "nt:unstructured");
                        rootElement.orderBefore(headNode.getName(),
                            rootElementChildren.nextNode().getName());
                    }
                }
                final Node rulesNode = headNode.addNode("rules(1)", "nt:unstructured");
                rulesNode.setProperty(SlingItsConstants.NODE_PREFIX,
                    Namespaces.ITS_NS_PREFIX);
                rulesNode.setProperty("version", "2.0");
                session.save();
            }
        }
        catch (final RepositoryException e)
        {
            LOG.error(
                "Unable to access repository to access or create node. Stack Trace: ", e);
        }
    }

    /**
     * Remove the old global rules for this resourceType if it exist and create
     * new rules for this resourceType.
     *
     * @param session
     *          the current session
     * @param resourceType
     *          resourceType
     */
    public static void createGlobalRulesNode(final Session session,
        final String resourceType)
    {
        try
        {
            if (StringUtils.isNotBlank(resourceType))
            {
                for (final String key : SlingItsConstants.getGlobalRules().keySet())
                {
                    final String path = SlingItsConstants.getGlobalRules().get(key)
                        + resourceType;
                    final NodeIterator children = JcrResourceUtil.query(
                        session,
                        "SELECT * FROM [nt:base] as t WHERE ISCHILDNODE([" + path
                            + "]) AND t.[node-prefix] LIKE '%' ORDER BY name(t) ASC",
                        Query.JCR_SQL2).getNodes();
                    while (children.hasNext())
                    {
                        children.nextNode().remove();
                    }
                    JcrResourceUtil.createPath(path, "nt:unstructured",
                        "nt:unstructured", session, false);
                }
            }
        }
        catch (final RepositoryException e)
        {
            LOG.error(
                "Unable to access repository to access or create node. Stack Trace: ", e);
        }
    }

    /**
     * Private constructor to prevent instantiation of this class.
     */
    private ItsRulesUtils()
    {
        throw new AssertionError("This class is not ment to be instantiated.");
    }
}
