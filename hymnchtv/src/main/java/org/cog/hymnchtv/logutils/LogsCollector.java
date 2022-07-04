/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cog.hymnchtv.logutils;

import org.cog.hymnchtv.HymnsApp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import timber.log.Timber;

/**
 * Collect logs and save them in compressed zip file.
 *
 * @author Damian Minkov
 * @author Eng Chong Meng
 */
public class LogsCollector
{
    /**
     * The name of the log dir.
     */
    public final static String LOGGING_DIR_NAME = "log";

    /**
     * The prefix name of standard java crash log file.
     */
    private static final String JAVA_ERROR_LOG_PREFIX = "hs_err_pid";

    /**
     * The date format used in file names.
     */
    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd@HH.mm.ss", Locale.US);

    /**
     * The pattern uses to match crash logs.
     */
    private static final Pattern JAVA_ERROR_LOG_PATTERN
            = Pattern.compile(Pattern.quote("sip.communicator"), Pattern.CASE_INSENSITIVE);

    /**
     * Save the log files in archive file. If destination is a folder, we generate the filename with
     * current date and time. If the destination is null we do nothing and if it is a file we use
     * as it, as we check does it end with zip extension, is missing we add it.
     *
     * @param destination the possible destination archived file
     * @param optional an optional file to be added to the archive.
     * @return the resulting file in zip format
     * @throws FileNotFoundException on file access permission denied
     */
    public static File collectLogs(File destination, File optional)
            throws FileNotFoundException
    {
        if (destination == null)
            return null;

        if (!destination.isDirectory()) {
            if (!destination.getName().endsWith("zip"))
                destination = new File(destination.getParentFile(), destination.getName() + ".zip");
        }
        else {
            destination = new File(destination, getDefaultFileName());
        }
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(destination));
        collectJavaCrashLogs(out);
        collectHomeFolderLogs(out);

        if (optional != null) {
            addFileToZip(optional, out);
        }
        try {
            out.close();
            return destination;
        } catch (IOException ex) {
            Timber.e(ex, "Error closing archive file");
        }
        return null;
    }

    /**
     * The default filename to use.
     *
     * @return the default filename to use.
     */
    public static String getDefaultFileName()
    {
        return FORMAT.format(new Date()) + "-logs.zip";
    }

    /**
     * Collects all files from log folder except the lock file; and put them in the zip file as zip entries.
     *
     * @param out the output zip file.
     */
    private static void collectHomeFolderLogs(ZipOutputStream out)
    {
        try {
            File[] fs = new File(HymnsApp.getGlobalContext().getFilesDir() + "/log").listFiles();
            if (fs != null) {
                for (File f : fs) {
                    if (f.getName().endsWith(".lck"))
                        continue;
                    addFileToZip(f, out);
                }
            }
        } catch (Exception e) {
            Timber.e(e, "Error obtaining logs folder");
        }
    }

    /**
     * Copies a file to the given archive.
     *
     * @param file the file to copy.
     * @param out the output archive stream.
     */
    private static void addFileToZip(File file, ZipOutputStream out)
    {
        byte[] buf = new byte[1024];

        try {
            FileInputStream in = new FileInputStream(file);
            // new ZIP entry
            out.putNextEntry(new ZipEntry(LOGGING_DIR_NAME + File.separator + file.getName()));

            // transfer bytes
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }

            out.closeEntry();
            in.close();
        } catch (FileNotFoundException ex) {
            Timber.e(ex, "Error obtaining file to archive");
        } catch (IOException ex) {
            Timber.e(ex, "Error saving file to archive");
        }
    }

    /**
     * Search for java crash logs belonging to us and add them to the log archive.
     *
     * @param out the output archive stream.
     */
    private static void collectJavaCrashLogs(ZipOutputStream out)
    {
        // First check in working dir
        addCrashFilesToArchive(new File(".").listFiles(), JAVA_ERROR_LOG_PREFIX, out);

        //java.io.tmpdir
        String tmpDir;
        if ((tmpDir = System.getProperty("java.io.tmpdir")) != null) {
            File[] tempFiles = new File(tmpDir).listFiles();
            addCrashFilesToArchive(tempFiles, JAVA_ERROR_LOG_PREFIX, out);
        }
    }

    /**
     * Checks if file is a crash log file and does it belongs to us.
     *
     * @param files files to check.
     * @param filterStartsWith a prefix for the files, can be null if no prefix check should be made.
     * @param out the output archive stream.
     */
    private static void addCrashFilesToArchive(File files[], String filterStartsWith, ZipOutputStream out)
    {
        // no files to add
        if (files == null)
            return;

        // First check in working dir
        for (File f : files) {
            if ((filterStartsWith != null) && !f.getName().startsWith(filterStartsWith)) {
                continue;
            }
            if (isOurCrashLog(f)) {
                addFileToZip(f, out);
            }
        }
    }

    /**
     * Checks whether the crash log file is for our application.
     *
     * @param file the crash log file.
     * @return <tt>true</tt> if error log is ours.
     */
    private static boolean isOurCrashLog(File file)
    {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (JAVA_ERROR_LOG_PATTERN.matcher(line).find())
                    return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }
}
