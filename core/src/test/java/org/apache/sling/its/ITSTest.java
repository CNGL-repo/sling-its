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
package org.apache.sling.its;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import net.sf.okapi.common.Util;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.w3c.its.Main;

/**
 * ITSTest class.
 */
public class ITSTest
{
    /** XML file extension.*/
    public static final String XML = "xml";
    /** HTML file extension.*/
    public static final String HTML = "html";
    /** The root directory. */
    private final String root = getParentDir(this.getClass(), "/input.xml")
        + "/its2.0/inputdata";

    //    private FileCompare fc = new FileCompare();

    /**
     * Test all the data categories' test files.
     *
     * @throws URISyntaxException
     *          the uri syntax exception due to bad uri.
     */
    @Test
    public final void process() throws URISyntaxException
    {
        processBatches(root + "/translate", Main.DC_TRANSLATE);
        processBatches(root + "/localizationnote", Main.DC_LOCALIZATIONNOTE);
        processBatches(root + "/terminology", Main.DC_TERMINOLOGY);
        processBatches(root + "/directionality", Main.DC_DIRECTIONALITY);
        processBatches(root + "/languageinformation", Main.DC_LANGUAGEINFORMATION);
        processBatches(root + "/elementswithintext", Main.DC_WITHINTEXT);
        processBatches(root + "/domain", Main.DC_DOMAIN);
        processBatches(root + "/textanalysis", Main.DC_TEXTANALYSIS);
        processBatches(root + "/localefilter", Main.DC_LOCALEFILTER);
        processBatches(root + "/externalresource", Main.DC_EXTERNALRESOURCE);
        processBatches(root + "/targetpointer", Main.DC_TARGETPOINTER);
        processBatches(root + "/idvalue", Main.DC_IDVALUE);
        processBatches(root + "/preservespace", Main.DC_PRESERVESPACE);
        processBatches(root + "/locqualityissue", Main.DC_LOCQUALITYISSUE);
        processBatches(root + "/locqualityrating", Main.DC_LOCQUALITYRATING);
        processBatches(root + "/storagesize", Main.DC_STORAGESIZE);
        processBatches(root + "/mtconfidence", Main.DC_MTCONFIDENCE);
        processBatches(root + "/allowedcharacters", Main.DC_ALLOWEDCHARACTERS);
        processBatches(root + "/provenance", Main.DC_PROVENANCE);
    }

    /**
     * Shortcut to process both xml and html formats.
     *
     * @param base
     *        the base directory
     * @param category
     *        the data category
     * @throws URISyntaxException
     *         the uri syntax exception due to bad uri.
     */
    public final void processBatches(final String base, final String category)
        throws URISyntaxException
    {
        processBatch(base + "/html", category);
        processBatch(base + "/xml", category);
    }

    /**
     * Process all files in specified folder.
     *
     * @param base
     *         the base directory should be the root plus the file extension
     *         directory.
     * @param category
     *         data category
     * @throws URISyntaxException
     *         the uri syntax exception due to bad uri.
     */
    public final void processBatch(final String base, final String category)
        throws URISyntaxException
    {
        removeOutput(base);
        final File f = new File(base);
        if (!f.exists())
        {
            return;
        }
        final String[] files = Util.getFilteredFiles(base, "");
        for (final String file : files)
        {
            if (file.contains("rules") || file.contains("standoff"))
            {
                continue;
            }
            process(base + "/" + file, category);
        }
    }

    /**
     * Remove the output directory.
     *
     * @param baseDir
     *        the base directory should include the root path, data category,
     *        and file extension.
     */
    private void removeOutput(final String baseDir)
    {
        final String outDir = baseDir.replace("/inputdata/", "/output/");
        Util.deleteDirectory(outDir, true);
    }

    /**
     * Take the xml/html file and run it through okapi's Main. Okapi's
     * Main should generate all the metadata in the output.txt for each
     * file name.
     *
     * @param baseName
     *          path of the file
     * @param dataCategory
     *          data category
     */
    private void process(final String baseName, final String dataCategory)
    {
        final String input = baseName;
        String output = input.replace("/inputdata/", "/output/");
        final int n = output.lastIndexOf('.');
        if (n > -1)
        {
            output = output.substring(0, n);
        }
        output += "output";
        output += ".txt";

        System.out.println("input-: " + input);

        System.out.println("output-: " + input);

        Main.main(new String[] { input, output, "-dc", dataCategory });
        assertTrue(new File(output).exists());

        final String gold = output.replace("/output/", "/expected/");
        assertTrue(compareFilesLineByLine(output, gold));
    }

    /**
     * Compare file line by line.
     *
     * @param output
     *         The path of the output file. The output file should be the file
     *         modified by Okapi's main.
     * @param gold
     *         The path of the expected file.
     * @return true if output and expected file are the same; otherwise, false.
     */
    private boolean compareFilesLineByLine(final String output, final String gold)
    {
        FileReader outputFileReader = null;
        FileReader expectedFileReader = null;

        BufferedReader outputbr = null;
        BufferedReader expectedbr = null;
        try
        {
            outputFileReader = new FileReader(output);
            expectedFileReader = new FileReader(gold);

            outputbr = new BufferedReader(outputFileReader);
            expectedbr = new BufferedReader(expectedFileReader);

            String outputText;
            String expectedText;
            while (((outputText = outputbr.readLine()) != null)
                && ((expectedText = expectedbr.readLine()) != null))
            {
                if (!StringUtils.equals(outputText, expectedText))
                {
                    System.out.println("There is a difference between the generated output file,\n"
                        + output + "\nand the expected file,\n" + gold + ".");
                    System.out.println("Output text: " + outputText);
                    System.out.println("Expected text: " + expectedText);
                    return false;
                }
            }
        }
        catch (final FileNotFoundException e)
        {
            e.printStackTrace();
            return false;
        }
        catch (final IOException e)
        {
            e.printStackTrace();
            return false;
        }
        finally
        {
            if (outputFileReader != null)
            {
                try
                {
                    outputFileReader.close();
                }
                catch (final IOException e)
                {
                    e.printStackTrace();
                    return false;
                }
            }
            if (expectedFileReader != null)
            {
                try
                {
                    expectedFileReader.close();
                }
                catch (final IOException e)
                {
                    e.printStackTrace();
                    return false;
                }
            }
            if (outputbr != null)
            {
                try
                {
                    outputbr.close();
                }
                catch (final IOException e)
                {
                    e.printStackTrace();
                    return false;
                }
            }
            if (expectedbr != null)
            {
                try
                {
                    expectedbr.close();
                }
                catch (final IOException e)
                {
                    e.printStackTrace();
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Get the parent directory.
     *
     * @param clazz
     *        class
     * @param filepath
     *        file path
     * @return parent directory
     */
    @SuppressWarnings("rawtypes")
    private String getParentDir(final Class clazz, final String filepath)
    {
        final URL url = clazz.getResource(filepath);
        String parentDir = null;
        if (url != null)
        {
            try
            {
                final File file = new File(url.toURI());
                parentDir = Util.ensureSeparator(file.getParent(), true);
            }
            catch (final URISyntaxException e)
            {
                return null;
            }
        }
        return parentDir;
    }
}
