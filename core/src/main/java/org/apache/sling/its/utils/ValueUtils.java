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

import java.util.Arrays;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utilities class to support javax.jcr.Value.
 */
public final class ValueUtils
{
    /** Logger instance. */
    private static final Logger LOG = LoggerFactory.getLogger(ValueUtils.class);

    /**
     * Convert the Value array into List of Strings.
     *
     * @param valueArray
     *          array of value objects
     * @return List of Strings
     */
    public static List<String> convertToArrayList(final Value[] valueArray)
    {
        final String[] stringArray = new String[valueArray.length];
        for (int i = 0; i < valueArray.length; i++)
        {
            try
            {
                stringArray[i] = valueArray[i].getString();
            }
            catch (final ValueFormatException e)
            {
                stringArray[i] = null;
                LOG.error("Failed to convert Value object to String. Strack Trace: \n", e);
            }
            catch (final IllegalStateException e)
            {
                stringArray[i] = null;
                LOG.error("Value has already been accessed elsewhere. Stack Trace: \n", e);
            }
            catch (final RepositoryException e)
            {
                stringArray[i] = null;
                LOG.error("Failed to access Value. Stack Trace: \n", e);
            }
        }
        return Arrays.asList(stringArray);

    }

    /**
     * Private constructor to prevent instantiation of this class.
     */
    private ValueUtils()
    {
        throw new AssertionError("This class is not ment to be instantiated.");
    }
}
