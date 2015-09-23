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

import static org.junit.Assert.assertEquals;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Test class for DocumentUtils.
 */
public class DocumentUtilsTest
{
    private Document document1;

    private Document document2;

    private Document document3;

    @Before
    public final void setUp() throws ParserConfigurationException
    {
        final DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setNamespaceAware(true);
        final DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        this.document1 = docBuilder.newDocument();
        final Element rootElement1 = this.document1.createElement("doc");
        rootElement1.setAttribute(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
            "foo");
        this.document1.appendChild(rootElement1);

        this.document2 = docBuilder.newDocument();
        final Element rootElement2 = this.document2.createElement("html");
        rootElement2.setAttribute("sling-resourceType", "bar");
        this.document2.appendChild(rootElement2);

        this.document3 = docBuilder.newDocument();
        final Element rootElement3 = this.document3.createElement("doc");
        this.document3.appendChild(rootElement3);
    }

    @Test
    public final void testGetResourceType()
    {
        final String firstDocumentResourceType = DocumentUtils.getResourceType(this.document1);
        final String secondDocumentResourceType = DocumentUtils.getResourceType(this.document2);
        final String thirdDocumentResourceType = DocumentUtils.getResourceType(this.document3);

        assertEquals(firstDocumentResourceType, "foo");
        assertEquals(secondDocumentResourceType, "bar");
        assertEquals(thirdDocumentResourceType, StringUtils.EMPTY);
    }
}
