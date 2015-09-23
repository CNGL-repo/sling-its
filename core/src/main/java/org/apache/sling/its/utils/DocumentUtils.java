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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import nu.validator.htmlparser.dom.HtmlDocumentBuilder;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * A utilities class for the org.w3c.dom.Document object.
 */
public final class DocumentUtils
{
    /** Logger instance. */
    private static final Logger LOG = LoggerFactory.getLogger(DocumentUtils.class);

    /**
     * Get the file and return a document object.
     *
     * @param requestParameter
     *          The request parameter that holds the metadata needed to pass to
     *          the File.
     * @param file
     *          File
     * @return Document
     *          A document object which holds all the xml elements.
     */
    public static Document getDocument(final RequestParameter requestParameter,
        final File file)
    {
        Document doc = null;
        OutputStream outputStream = null;
        try
        {
            outputStream = new FileOutputStream(file);
            outputStream.write(requestParameter.get());

            if (StringUtils.equals(
                FilenameUtils.getExtension(requestParameter.getFileName()), "html"))
            {
                final HtmlDocumentBuilder docBuilder = new HtmlDocumentBuilder();
                doc = docBuilder.parse(file);
            }
            else
            {
                final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setNamespaceAware(true);
                dbf.setValidating(false);
                doc = dbf.newDocumentBuilder().parse(file);
            }
        }
        catch (final SAXException saxe)
        {
            LOG.error("Failed to parse document. Stack Trace:", saxe);
        }
        catch (final ParserConfigurationException pce)
        {
            LOG.error("Failed to create DocumentBuilder. Stack Trace: ", pce);
        }
        catch (final FileNotFoundException nfe)
        {
            LOG.error("File Not Found. Stack Trace: ", nfe);
        }
        catch (final IOException ioe)
        {
            LOG.error("Failed to write to file. Stack Trace: ", ioe);
        }
        finally
        {
            if (outputStream != null)
            {
                try
                {
                    outputStream.close();
                }
                catch (final IOException e)
                {
                    LOG.error("Failed to close outputStream. Stack Trace: \n", e);
                }
            }
        }
        return doc;
    }

    /**
     * Gets the resourceType from the root element of the document. The
     * resourceType could be named sling:resourceType or sling-resourceType
     * depending on whether the imported document is xml or html. If not found,
     * it should return null.
     *
     * @param doc
     *        Document object
     * @return sling:resourceType or empty string
     */
    public static String getResourceType(final Document doc)
    {
        String resourceType = doc.getDocumentElement().getAttribute(
            JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY);
        resourceType = (StringUtils.isBlank(resourceType)) ? doc.getDocumentElement().getAttribute(
            "sling-resourceType")
            : resourceType;
        return resourceType;
    }

    /**
     * Private constructor to prevent instantiation of this class.
     */
    private DocumentUtils()
    {
        throw new AssertionError("This class is not ment to be instantiated.");
    }
}
