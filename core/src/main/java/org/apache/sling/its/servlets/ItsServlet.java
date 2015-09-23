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

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.jcr.NamespaceRegistry;
import javax.jcr.query.Query;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.okapi.common.MimeTypeMapper;
import net.sf.okapi.common.Namespaces;

import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.its.constants.SlingItsConstants;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

@Component(immediate = true, metatype = true, name = "org.apache.sling.its.servlets.ItsServlet", label = "%servlet.get.name", description = "%servlet.get.description")
@Service(Servlet.class)
@Properties({
        @Property(name = "service.description", value = "ITS Servlet"),
        @Property(name = "service.vendor", value = "Adobe Systems"),

        // Use this as a default servlet for Sling
        @Property(name = "sling.servlet.resourceTypes", value = "sling/servlet/default", propertyPrivate = true),
        @Property(name = "sling.servlet.prefix", intValue = -1, propertyPrivate = true),

        // Generic handler for all get requests
        @Property(name = "sling.servlet.methods", value = "GET", propertyPrivate = true),
        @Property(name = "sling.servlet.selectors", value = { "its" }, propertyPrivate = true),
        @Property(name = "sling.servlet.extensions", value = { "xml", "html" }, propertyPrivate = true) })
public class ItsServlet extends SlingSafeMethodsServlet
{
    /** UID for serialization. */
    private static final long serialVersionUID = 5230389885707780236L;
    /** Logger instance. */
    private static final Logger LOG = LoggerFactory.getLogger(ItsServlet.class);
    /** Boolean to determine if the requested page is html. */
    private boolean isHtml;

    /**
     * Gets automatically invoked when servlet is started.
     *
     * @param ctx
     *            the component context
     */
    protected final void activate(final ComponentContext ctx)
    {

    }

    /**
     * Gets automatically invoked when service is stopped.
     *
     * @param ctx
     *            the component context
     */
    protected final void deactivate(final ComponentContext ctx)
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
        if (ResourceUtil.isNonExistingResource(request.getResource()))
        {
            LOG.error("No resource found for path: " + request.getResource().getPath());
            response.getWriter().write(
                "500: No resource found for path: " + request.getResource().getPath());
            return;
        }

        this.isHtml = request.getRequestPathInfo().getExtension().equals("html");

        try
        {
            final DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docFactory.setNamespaceAware(true);
            final DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // root elements.
            final Document doc = docBuilder.newDocument();
            createDocument(request.getResource(), doc);

            // make sure the encoding is set before getWriter() is called.
            if (this.isHtml)
            {
                response.setCharacterEncoding(CharEncoding.UTF_8);
                response.setContentType(MimeTypeMapper.HTML_MIME_TYPE);
                response.getWriter().write("<!DOCTYPE html>");
            }
            else
            {
                response.setCharacterEncoding(CharEncoding.UTF_8);
                response.setContentType(MimeTypeMapper.XML_MIME_TYPE);
            }

            // write the content into xml file.
            final TransformerFactory transformerFactory = TransformerFactory.newInstance();
            final Transformer transformer = transformerFactory.newTransformer();
            final DOMSource source = new DOMSource(doc);
            final StreamResult result = new StreamResult(response.getWriter());

            // set the correct properties for the xml or html file.
            transformer.setOutputProperty(OutputKeys.METHOD, Namespaces.XML_NS_PREFIX);
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

            // Output the xml or html file.
            transformer.transform(source, result);
        }
        catch (final ParserConfigurationException pce)
        {
            LOG.error("Failed to create DocumentBuilder. Stack Trace: ", pce);
        }
        catch (final TransformerException tfe)
        {
            LOG.error("Failed to transform the document. Stack Trace: ", tfe);
        }
    }

    /**
     * Create all the necessary elements and append it to the document. For
     * xml, the root element of the document should be the requested resource.
     * For html, it has to be the first child of the request resource (the html
     * resource).
     *
     * @param rootResource
     *          requested resource
     * @param doc
     *          XML Document
     */
    private void createDocument(final Resource rootResource, final Document doc)
    {
        final Resource firstChild = getFirstChild(rootResource);
        final Resource resource = (this.isHtml ? firstChild : rootResource);
        final String resourceType = (firstChild != null ? firstChild.getResourceType()
            : StringUtils.EMPTY);
        final Element rootElement = doc.createElement(resource.getName());
        if (!this.isHtml)
        {
            addNamespaces(rootElement);
        }
        doc.appendChild(rootElement);
        processAttributes(resource, rootElement);

        // children element logic.
        final Iterator<Resource> iter = resource.listChildren();
        while (iter.hasNext())
        {
            processChild(iter.next(), rootElement, resourceType);
        }
    }

    /**
     * Add its, sling-its, jcr and sling namespaces to the root element of the
     * xml document.
     *
     * @param rootElement
     *           the root element of the Document.
     */
    private void addNamespaces(final Element rootElement)
    {
        rootElement.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
            SlingItsConstants.XMLNS + Namespaces.ITS_NS_PREFIX, Namespaces.ITS_NS_URI);
        rootElement.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
            SlingItsConstants.XMLNS + "sling-its", "http://www.w3.org/2013/7/sling-its");
        rootElement.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
            SlingItsConstants.XMLNS + NamespaceRegistry.PREFIX_JCR,
            NamespaceRegistry.NAMESPACE_JCR);
        rootElement.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
            SlingItsConstants.XMLNS + SlingConstants.NAMESPACE_PREFIX,
            "http://sling.apache.org/jcr/sling/1.0");
    }

    /**
     * Returns the first child of the parentResource. If parentResource does
     * not exist nor does it have children, it will return null.
     *
     * @param parentResource
     *         parent resource
     * @return the first child resource of parent resource or null
     */
    private Resource getFirstChild(final Resource parentResource)
    {
        if (parentResource != null)
        {
            final Iterator<Resource> children = parentResource.listChildren();
            if (children.hasNext())
            {
                return children.next();
            }
        }
        return null;
    }

    /**
     * Process the child resource. Each child resource needs its own element
     * and append it to the document. After element has been created, the
     * attributes subsequently needs to be processed. However,
     * text-content-node resources do not need its own element. The
     * text-content property needs to be appended to the previous element.
     *
     * @param resource
     *          the current resource
     * @param element
     *          the current element
     * @param resourceType
     *          the resourceType provided by the root element
     */
    private void processChild(final Resource resource, final Element element,
        final String resourceType)
    {
        final ValueMap valueMap = resource.adaptTo(ValueMap.class);
        final String prefix = valueMap.get(SlingItsConstants.NODE_PREFIX, String.class);
        final String name = getElementName(resource, prefix);
        final Document doc = element.getOwnerDocument();
        final Element el = doc.createElement(name);
        if (name.equals(SlingItsConstants.TEXT_CONTENT_NODE))
        {
            final Text text = doc.createTextNode(valueMap.get(
                SlingItsConstants.TEXT_CONTENT, StringUtils.EMPTY));
            element.appendChild(text);
        }
        else if (name.endsWith(SlingItsConstants.ITS_RULES) && this.isHtml
            && !element.getNodeName().equals("script") && StringUtils.isNotBlank(prefix))
        {
            final Element scriptElement = doc.createElement("script");
            scriptElement.setAttribute("type", "application/its+xml");
            element.appendChild(scriptElement);
            scriptElement.appendChild(el);
            processAttributes(resource, el);
        }
        else
        {
            element.appendChild(el);
            processAttributes(resource, el);
        }

        if (name.endsWith(SlingItsConstants.ITS_RULES) && StringUtils.isNotBlank(prefix)
            && StringUtils.isNotBlank(resourceType))
        {
            for (final String globalRulePath : SlingItsConstants.getGlobalRules().values())
            {
                final Iterator<Resource> globalRules = resource.getResourceResolver().findResources(
                    "SELECT * FROM [nt:base] as t WHERE ISCHILDNODE([" + globalRulePath
                        + resourceType + "]) AND t.[node-prefix] LIKE '" + prefix
                        + "' ORDER BY name(t) ASC", Query.JCR_SQL2);
                while (globalRules.hasNext())
                {
                    processChild(globalRules.next(), el, resourceType);
                }
            }
        }
        else
        {
            final Iterator<Resource> iter = resource.listChildren();
            while (iter.hasNext())
            {
                processChild(iter.next(), el, resourceType);
            }
        }
    }

    /**
     * Process the properties of the current resource. Every property of the
     * current resource needs to be outputted to the document with the
     * exception of properties with the jcr and sling prefix.
     *
     * To adhere to the w3c id rule, there will be an extra id property that
     * needs to be generated.
     *
     * @param resource
     *          the current resource
     * @param element
     *          the current element
     */
    private void processAttributes(final Resource resource, final Element element)
    {
        final Document doc = element.getOwnerDocument();
        final ValueMap props = resource.adaptTo(ValueMap.class);
        final List<String> namespaces = Arrays.asList(props.get(
            SlingItsConstants.NAMESPACE_DECLARATION, new String[] {}));
        for (final String key : props.keySet())
        {
            if (isValidProperty(key))
            {
                final String value = (String) props.get(key);
                if (SlingItsConstants.TEXT_CONTENT.equals(key))
                {
                    element.setTextContent(value);
                }
                else if (SlingItsConstants.ITS_NOTE.equals(key)
                    && element.getNodeName().endsWith(SlingItsConstants.ITS_LOCNOTE_RULE))
                {
                    final Element locNoteElement = doc.createElement(props.get(
                        SlingItsConstants.NODE_PREFIX, String.class) + ":locNote");
                    locNoteElement.setTextContent(value);
                    element.appendChild(locNoteElement);
                }
                else if (namespaces.contains(key))
                {
                    element.setAttribute(SlingItsConstants.XMLNS + key, value);
                }
                else if (this.isHtml
                    && StringUtils.equals(key, SlingItsConstants.XML_PRIMARY_TYPE_PROP)
                    && (!props.keySet().contains(SlingItsConstants.NODE_PREFIX)))
                {
                    element.setAttribute(SlingItsConstants.HTML_PRIMARY_TYPE_PROP, value);
                }
                else if (this.isHtml
                    && StringUtils.equals(key,
                        JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)
                    && (!props.keySet().contains(SlingItsConstants.NODE_PREFIX)))
                {
                    element.setAttribute(SlingItsConstants.HTML_RESOURCE_TYPE_PROP, value);
                }
                else
                {
                    element.setAttribute(key, value);
                }
            }
        }
        if (!resource.getPath().startsWith(SlingItsConstants.ITS_GLOBAL_PATH)
            && !element.getNodeName().endsWith(SlingItsConstants.ITS_RULES)
            && !props.keySet().contains("id") && !props.keySet().contains("xml:id"))
        {
            element.setAttribute((this.isHtml ? "data-sling-its-id" : "sling-its:id"),
                getUniqueId(resource.getPath()));
        }
    }

    /**
     * Gets the element name. It should be the resource's name without the
     * parentheses and the number within the parentheses. However, if the
     * node has a prefix property. The prefix needs to be appended to the name.
     *
     * @param resource
     *         Current resource
     * @param prefix
     *         The prefix, could be null if no prefix were provided
     * @return Name of the resource without the iteration of the node in its
     *         name. Prefix may be appended if provided with one.
     */
    private String getElementName(final Resource resource, final String prefix)
    {
        final String name = (resource.getName()).replaceAll("\\(\\d+\\)",
            StringUtils.EMPTY);
        if (StringUtils.isNotBlank(prefix))
        {
            return prefix + ":" + name;
        }
        return name;
    }

    /**
     * Check if it's a property name we want to output into the xml file. The
     * xlink prefix is to externally reference global rules. For our purposes,
     * we include the rules ourselves. Lastly, namespace-declaration and
     * node-prefix is used to render out the correct namespace/prefix.
     *
     * @param key
     *         property name
     * @return true if key is a property we want to output on to the xml file;
     *         otherwise, false.
     */
    private boolean isValidProperty(final String key)
    {
        return (key.indexOf("jcr:") < 0 || key.equals(SlingItsConstants.XML_PRIMARY_TYPE_PROP))
            && key.indexOf("xlink") < 0
            && !key.equals(SlingItsConstants.NAMESPACE_DECLARATION)
            && !key.equals(SlingItsConstants.NODE_PREFIX);
    }

    /**
     * Need to generate unique IDs with paths. So parentheses will need to be
     * stripped. Forward slashes will be replaced with underscores.
     *
     * @param path
     *         resource path
     * @return unique id
     */
    private String getUniqueId(final String path)
    {
        return ((path.substring(1)).replaceAll("[\\(\\)]", StringUtils.EMPTY)).replaceAll(
            "/", "_");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void destroy()
    {
        super.destroy();
    }
}
