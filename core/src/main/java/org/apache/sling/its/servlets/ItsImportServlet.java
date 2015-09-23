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
package org.apache.sling.its.servlets;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.xml.XMLConstants;

import net.sf.okapi.common.Namespaces;
import net.sf.okapi.common.exceptions.OkapiBadFilterParametersException;
import net.sf.okapi.filters.its.html5.HTML5Filter;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.its.constants.SlingItsConstants;
import org.apache.sling.its.utils.DocumentUtils;
import org.apache.sling.its.utils.ItsRulesUtils;
import org.apache.sling.its.utils.JcrNodeUtils;
import org.apache.sling.its.utils.ValueUtils;
import org.apache.sling.its.utils.XmlNodeUtils;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.jcr.resource.JcrResourceUtil;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.its.IProcessor;
import org.w3c.its.ITSEngine;
import org.w3c.its.ITraversal;

@Component(immediate = true, metatype = true, name = "org.apache.sling.its.servlets.ItsImportServlet", label = "%servlet.get.name", description = "%servlet.get.description")
@Service(Servlet.class)
@Properties({
        @Property(name = "service.description", value = "ITS Import Servlet"),
        @Property(name = "service.vendor", value = "Adobe Systems"),

        // Generic handler for all get requests
        @Property(name = "sling.servlet.methods", value = "POST", propertyPrivate = true),
        @Property(name = "sling.servlet.paths", value = "/bin/its/import", propertyPrivate = true) })
public class ItsImportServlet extends SlingAllMethodsServlet
{
    /** UID for serialization. */
    private static final long serialVersionUID = 5983619887988477737L;
    /** Logger instance. */
    private static final Logger LOG = LoggerFactory.getLogger(ItsImportServlet.class);
    /** The current session. */
    private Session session;
    /** Holds the path and the number of iteration of that element in the given path. */
    private Map<String, Integer> counterMap;
    /** If current doc or external doc contains global rules.*/
    private boolean hasGlobalRules;

    /**
     * Gets automatically invoked when servlet is started.
     *
     * @param ctx
     *            the component context
     */
    protected void activate(final ComponentContext ctx)
    {

    }

    /**
     * Gets automatically invoked when service is stopped.
     *
     * @param ctx
     *            the component context
     */
    protected void deactivate(final ComponentContext ctx)
    {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void init() throws ServletException
    {
        super.init();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest,
     * org.apache.sling.api.SlingHttpServletResponse)
     */
    @Override
    protected final void doGet(final SlingHttpServletRequest request,
        final SlingHttpServletResponse response) throws ServletException, IOException
    {
        response.getWriter().write("Please use POST.");
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest, 
     * org.apache.sling.api.SlingHttpServletResponse)
     */
    @Override
    protected final void doPost(final SlingHttpServletRequest request,
        final SlingHttpServletResponse response) throws ServletException, IOException
    {
        // assert that path and resourceType was provided.
        final String targetPath = request.getParameter("path");
        if (targetPath == null)
        {
            response.getWriter().write(
                "500: Target path required. Please add a valid 'path' parameter.");
            LOG.error("Target path required. Please add a valid 'path' parameter.");
            return;
        }

        this.session = request.getResourceResolver().adaptTo(Session.class);
        this.counterMap = new HashMap<String, Integer>();
        this.hasGlobalRules = false;

        // get the document.
        final File file = File.createTempFile("input",
            StringUtils.EMPTY + System.currentTimeMillis());
        file.deleteOnExit();
        final Document doc = DocumentUtils.getDocument(
            request.getRequestParameter("file"), file);
        if (request.getRequestParameters("externalFile") != null && doc != null)
        {
            final String resourceType = DocumentUtils.getResourceType(doc);
            // create new rules node for this resourceType.
            ItsRulesUtils.createGlobalRulesNode(this.session, resourceType);
            for (final RequestParameter requestParameter : request.getRequestParameters("externalFile"))
            {
                final File externalRulesFile = new File(file.getParent() + File.separator
                    + requestParameter.getFileName());
                externalRulesFile.deleteOnExit();
                final Document externalDoc = DocumentUtils.getDocument(requestParameter,
                    externalRulesFile);
                store("/", resourceType, externalDoc, externalRulesFile, true);
            }
        }

        if (doc != null)
        {
            JcrNodeUtils.createNode(this.session, targetPath);
            final String resourceType = DocumentUtils.getResourceType(doc);
            if (request.getRequestParameters("externalFile") == null)
            {
                // create new rules node for this resourceType.
                ItsRulesUtils.createGlobalRulesNode(this.session, resourceType);
            }
            store(targetPath, resourceType, doc, file, false);
        }

        if (!this.hasGlobalRules)
        {
            ItsRulesUtils.createItsRulesNode(this.session, targetPath);
        }
    }

    /**
     * Get the attributes and call output to set the attributes into
     * jackrabbit.
     *
     * @param element
     *           an Element from the Document object.
     * @param path
     *           jackrabbit path to store the attributes
     */
    private void setAttributes(final Element element, final String path)
    {
        if (element.hasAttributes())
        {
            final NamedNodeMap map = element.getAttributes();

            final ArrayList<String> list = new ArrayList<String>();
            for (int i = 0; i < map.getLength(); i++)
            {
                list.add(((Attr) map.item(i)).getNodeName());
            }
            Collections.sort(list);

            for (final String attrName : list)
            {
                final Attr attr = (Attr) map.getNamedItem(attrName);
                output(path, attr, null);
            }
        }
    }

    /**
     * Gets the iteration of this node at its current level. For example, under
     * the body node, there could be two span tags as its children. The first
     * span tag will be span(1) while the second will be span(2). This is
     * needed so we won't override the first tag.
     *
     * @param relPath
     *         relative path for local mark up. absolute path for global rules.
     * @return counterValue
     *            iteration of this node name at its current level.
     */
    private Integer getCounter(final String relPath)
    {
        Integer counterValue = this.counterMap.get(relPath);
        counterValue = (counterValue != null) ? counterValue + 1 : 1;
        this.counterMap.put(relPath, counterValue);
        return counterValue;
    }

    /**
     * When it is a closing tag or an element node has no children, path and/or
     * globalPath needs to go back one level.
     *
     * @param relPath
     *         the relative path
     * @return relPath
     *         parent path of the relative path
     */
    private String backTrack(final String relPath)
    {
        final int n = relPath.lastIndexOf('/');
        if (n > -1)
        {
            return relPath.substring(0, n);
        }
        return relPath;
    }

    /**
     * If element has child elements, don't process them and skip those nodes.
     *
     * @param element
     *         current element
     * @param itsEng
     *         the ITSEngine
     */
    private void skipChildren(final Element element, final ITraversal itsEng)
    {
        if (element.hasChildNodes())
        {
            Node node;
            while ((node = itsEng.nextNode()) != null)
            {
                if (node.getNodeType() == Node.ELEMENT_NODE)
                {
                    if (itsEng.backTracking()
                        && StringUtils.equals(node.getLocalName(), element.getLocalName()))
                    {
                        break;
                    }
                }
            }
        }
    }

    /**
     * Store the global rule.
     *
     * @param element
     *         an Element from the Document object.
     * @param resourceType
     *         resource type
     * @param itsEng
     *         the ITSEngine
     */
    private void storeGlobalRule(final Element element, final String resourceType,
        final ITraversal itsEng)
    {
        if (StringUtils.isNotBlank(resourceType))
        {
            String globalPath = SlingItsConstants.getGlobalRules().get(
                element.getLocalName())
                + resourceType;
            if (element.getPrefix() != null)
            {
                globalPath += String.format("/%s(%d)", element.getLocalName(),
                    getCounter(globalPath + "/" + element.getLocalName()));
                element.setAttribute(SlingItsConstants.NODE_PREFIX, element.getPrefix());
            }
            else
            {
                globalPath += String.format("/%s(%d)", element.getNodeName(),
                    getCounter(globalPath + "/" + element.getNodeName()));
            }

            output(globalPath, null, null);
            if (element.getLocalName().equals("param"))
            {
                output(globalPath, null, element.getTextContent());
            }
            else if (element.getLocalName().equals(SlingItsConstants.ITS_LOCNOTE_RULE)
                && element.hasChildNodes())
            {
                final Element locNoteElement = (Element) XmlNodeUtils.getChildNodeByLocalName(
                    element, SlingItsConstants.ITS_LOCNOTE);
                if (locNoteElement != null)
                {
                    element.setAttribute(SlingItsConstants.ITS_NOTE,
                        locNoteElement.getTextContent());
                }
            }
            setAttributes(element, globalPath);
        }
        skipChildren(element, itsEng);
    }

    /**
     * Store the element and its attribute. The child node of global rules are
     * specially handled so they will not be traversed.
     *
     * @param path
     *         the target path
     * @param resourceType
     *         the resourceType
     * @param doc
     *         the document
     * @param file
     *        the file.
     * @param isExternalDoc
     *         true if this is for storing global rules for external documents
     */
    private void store(String path, final String resourceType, final Document doc,
        final File file, final boolean isExternalDoc)
    {
        final ITraversal itsEng = applyITSRules(doc, file, null, false);
        itsEng.startTraversal();
        Node node;
        while ((node = itsEng.nextNode()) != null)
        {
            switch (node.getNodeType())
            {
                case Node.ELEMENT_NODE:
                    final Element element = (Element) node;
                    // Use !backTracking() to get to the elements only once
                    // and to include the empty elements (for attributes).
                    if (itsEng.backTracking())
                    {
                        if (!SlingItsConstants.getGlobalRules().containsKey(
                            element.getLocalName()))
                        {
                            path = backTrack(path);
                        }
                    }
                    else
                    {
                        if (element.isSameNode(doc.getDocumentElement())
                            && !isExternalDoc)
                        {
                            path += "/" + element.getNodeName();
                            output(path, null, null);
                            setAttributes(element, path);
                        }
                        else if (SlingItsConstants.getGlobalRules().containsKey(
                            element.getLocalName()))
                        {
                            storeGlobalRule(element, resourceType, itsEng);
                        }
                        else if (!isExternalDoc
                            && !SlingItsConstants.getGlobalRules().containsKey(
                                element.getLocalName())
                            && !(element.getParentNode().getLocalName().equals(
                                SlingItsConstants.ITS_RULES) && element.getParentNode().getPrefix() != null))
                        {
                            if (element.getLocalName().equals(SlingItsConstants.ITS_RULES)
                                && element.getPrefix() != null)
                            {
                                this.hasGlobalRules = true;
                            }
                            if (element.getPrefix() != null)
                            {
                                path += String.format("/%s(%d)", element.getLocalName(),
                                    getCounter(path + "/" + element.getLocalName()));
                                element.setAttribute(SlingItsConstants.NODE_PREFIX,
                                    element.getPrefix());
                            }
                            else if (element.getNodeName().equals("link")
                                && StringUtils.endsWith(element.getAttribute("rel"),
                                    "-rules"))
                            {
                                path += String.format("/%s(%d)",
                                    SlingItsConstants.ITS_RULES, getCounter(path + "/"
                                        + SlingItsConstants.ITS_RULES));
                                final String prefix = StringUtils.substringBefore(
                                    element.getAttribute("rel"), "-rules");
                                element.setAttribute(SlingItsConstants.NODE_PREFIX,
                                    prefix);
                                element.setAttributeNS(
                                    XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
                                    SlingItsConstants.XMLNS + prefix,
                                    Namespaces.ITS_NS_URI);
                                element.setAttributeNS(
                                    XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:h",
                                    Namespaces.HTML_NS_URI);
                                element.setAttributeNS(
                                    XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:jcr",
                                    NamespaceRegistry.NAMESPACE_JCR);
                                this.hasGlobalRules = true;
                            }
                            else
                            {
                                path += String.format("/%s(%d)", element.getNodeName(),
                                    getCounter(path + "/" + element.getNodeName()));
                            }
                            output(path, null, null);
                            setAttributes(element, path);
                            if (!element.hasChildNodes()) // Empty elements:
                            {
                                path = backTrack(path);
                            }
                        }
                    }
                    break;
                case Node.TEXT_NODE:
                    if (StringUtils.isNotBlank(node.getNodeValue()) && !isExternalDoc)
                    {
                        path += String.format("/%s(%d)",
                            SlingItsConstants.TEXT_CONTENT_NODE, getCounter(path + "/"
                                + SlingItsConstants.TEXT_CONTENT_NODE));
                        output(path, null, node.getNodeValue());
                        path = backTrack(path);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Creates the jcr node and appends the necessary properties.
     *
     * @param absPath
     *         absolute path of the node.
     * @param attr
     *         attribute of the element
     * @param textContent
     *        text content of the element.
     */
    private void output(final String absPath, final Attr attr, final String textContent)
    {
        javax.jcr.Node node = null;
        try
        {
            if (this.session.itemExists(absPath) && attr == null && textContent == null)
            {
                node = (javax.jcr.Node) this.session.getItem(absPath);
                node.remove();
            }
            node = JcrResourceUtil.createPath(absPath, "nt:unstructured",
                "nt:unstructured", this.session, false);

            if (textContent != null)
            {
                node.setProperty(SlingItsConstants.TEXT_CONTENT, textContent);
            }

            if (attr != null)
            {
                if (attr.getNodeName().startsWith(XMLConstants.XMLNS_ATTRIBUTE))
                {
                    node.setProperty(attr.getLocalName(), attr.getNodeValue());
                    if (node.hasProperty(SlingItsConstants.NAMESPACE_DECLARATION))
                    {
                        final ArrayList<String> prefixes = new ArrayList<String>(
                            ValueUtils.convertToArrayList(node.getProperty(
                                SlingItsConstants.NAMESPACE_DECLARATION).getValues()));
                        if (!prefixes.contains(attr.getLocalName()))
                        {
                            prefixes.add(attr.getLocalName());
                            node.setProperty(SlingItsConstants.NAMESPACE_DECLARATION,
                                prefixes.toArray(new String[prefixes.size()]));
                        }
                    }
                    else
                    {
                        node.setProperty(SlingItsConstants.NAMESPACE_DECLARATION,
                            new String[] { attr.getLocalName() });
                    }
                }
                else if (StringUtils.equals(attr.getNodeName(),
                    SlingItsConstants.XML_PRIMARY_TYPE_PROP)
                    || StringUtils.equals(attr.getNodeName(),
                        SlingItsConstants.HTML_PRIMARY_TYPE_PROP))
                {
                    node.setPrimaryType(attr.getNodeValue());
                }
                else if (StringUtils.equals(attr.getNodeName(),
                    SlingItsConstants.HTML_RESOURCE_TYPE_PROP))
                {
                    node.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
                        attr.getNodeValue());
                }
                else
                {
                    node.setProperty(attr.getNodeName(), attr.getNodeValue());
                }
            }
            this.session.save();
        }
        catch (final RepositoryException e)
        {
            LOG.error(
                "Unable to access repository to access or create node. Stack Trace: ", e);
        }
    }

    /**
     * Get the (optional) HTML5 and (optional)  external rules and apply the
     * ITS rules to the input file.
     *
     * @param doc
     *         Document
     * @param inputFile
     *         input file
     * @param rulesFile
     *         external rules file
     * @param isHTML5
     *         true if input file is HTML5
     * @return ITSEngine
     *            the ITSEngine
     */
    private static ITraversal applyITSRules(final Document doc, final File inputFile,
        final File rulesFile, final boolean isHTML5)
    {
        // Create the ITS engine
        final ITSEngine itsEng = new ITSEngine(doc, inputFile.toURI(), isHTML5, null);

        // For HTML5: load the default rules
        if (isHTML5)
        {
            final URL url = HTML5Filter.class.getResource("strict.fprm");
            try
            {
                itsEng.addExternalRules(url.toURI());
            }
            catch (final URISyntaxException e)
            {
                throw new OkapiBadFilterParametersException(
                    "Cannot load strict default parameters.");
            }
        }

        // Add any external rules file(s)
        if (rulesFile != null)
        {
            itsEng.addExternalRules(rulesFile.toURI());
        }

        // Load the linked rules for HTML
        if (isHTML5)
        {
            HTML5Filter.loadLinkedRules(doc, inputFile.toURI(), itsEng);
        }

        // Apply the all rules (external and internal) to the document
        itsEng.applyRules(IProcessor.DC_ALL);

        return itsEng;
    }
}
