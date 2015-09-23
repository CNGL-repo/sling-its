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
package org.apache.sling.its.constants;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ITS constants.
 */
public final class SlingItsConstants
{
    /** The text-content-node resource name. */
    public static final String TEXT_CONTENT_NODE = "text-content-node";
    /** The property name that holds the text content. */
    public static final String TEXT_CONTENT = "text-content";
    /** Property that contains all the properties missing the xmlns: prefix. */
    public static final String NAMESPACE_DECLARATION = "namespace-declaration";
    /** Property that holds the prefix for the element name. */
    public static final String NODE_PREFIX = "node-prefix";
    /** XML's attribute name for the jcr:primaryType. */
    public static final String XML_PRIMARY_TYPE_PROP = "jcr:primaryType";
    /** HTML's attribute name for the jcr:primaryType. */
    public static final String HTML_PRIMARY_TYPE_PROP = "jcr-primaryType";
    /** HTML's attribute name for the sling:resourceType. */
    public static final String HTML_RESOURCE_TYPE_PROP = "sling-resourceType";
    /** The xmlns prefix. */
    public static final String XMLNS = "xmlns:";
    /** The rules constant. */
    public static final String ITS_RULES = "rules";
    /** The path to signify it is a global rule. */
    public static final String ITS_GLOBAL_PATH = "/etc/its";
    /** The locNoteRule constant. */
    public static final String ITS_LOCNOTE_RULE = "locNoteRule";
    /** The element that contains the note. */
    public static final String ITS_LOCNOTE = "locNote";
    /** The attribute that will hold the note. */
    public static final String ITS_NOTE = "note";

    /**
     * The map from the name of the rule to the location where it should be
     * stored.
     */
    private static final Map<String, String> GLOBAL_RULES = new LinkedHashMap<String, String>();

    static
    {
        GLOBAL_RULES.put("param", "/etc/its/param/global/");
        GLOBAL_RULES.put("translateRule", "/etc/its/translate/global/");
        GLOBAL_RULES.put("locNoteRule", "/etc/its/locnote/global/");
        GLOBAL_RULES.put("idValueRule", "/etc/its/idvalue/global/");
        GLOBAL_RULES.put("targetPointerRule", "/etc/its/targetpointer/global/");
    }

    /**
     * Get the global rules.
     *
     * @return the hashmap for global rules.
     */
    public static Map<String, String> getGlobalRules()
    {
        return GLOBAL_RULES;
    }

    /**
     * Private constructor to prevent instantiation of this class.
     */
    private SlingItsConstants()
    {
        throw new AssertionError("This class is not ment to be instantiated.");
    }
}
