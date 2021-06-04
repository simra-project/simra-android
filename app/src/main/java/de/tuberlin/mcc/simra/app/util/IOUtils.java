package de.tuberlin.mcc.simra.app.util;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import de.tuberlin.mcc.simra.app.BuildConfig;
import de.tuberlin.mcc.simra.app.activities.SettingsActivity;

public class IOUtils {
    private static final String TAG = "IOUtils_LOG";

    public static boolean isDirectoryEmpty(String path) {
        File dir = new File(path);
        if (dir.isDirectory()) {
            return dir.listFiles().length == 0;
        }
        return false;
    }

    /*
     *
     * Zips a file at a location and places the resulting zip file at the toLocation
     * Example: zipFileAtPath("downloads/myfolder", "downloads/myFolder.zip");
     */
    public static void zipFolder(String inputFolderPath, String outZipPath) {
        try {
            FileOutputStream fos = new FileOutputStream(outZipPath);
            ZipOutputStream zos = new ZipOutputStream(fos);
            File srcFile = new File(inputFolderPath);
            File[] files = srcFile.listFiles();
            Log.d(TAG, "Zip directory: " + srcFile.getName());
            for (int i = 0; i < files.length; i++) {
                Log.d("", "Adding file: " + files[i].getName());
                byte[] buffer = new byte[1024];
                FileInputStream fis = new FileInputStream(files[i]);
                zos.putNextEntry(new ZipEntry(files[i].getName()));
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, length);
                }
                zos.closeEntry();
                fis.close();
            }
            zos.close();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public static boolean zipto(String sourcePath, String toLocation, Context ctx) {

        final int BUFFER = 2048;

        File sourceFile = new File(sourcePath);
        Log.d(TAG, "sourcePath: " + sourcePath);
        try {
            BufferedInputStream origin = null;
            FileOutputStream dest = new FileOutputStream(toLocation);
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(
                    dest));
            if (sourceFile.isDirectory()) {
                Log.d(TAG, sourcePath + " is directory. Entering zipSubFolder");
                zipSubFolder(out, sourceFile, sourceFile.getParent().length(),ctx);
            } else {
                // never used so far 14/05/21

                Log.d(TAG, sourcePath + " is not directory. Zipping");
                byte data[] = new byte[BUFFER];
                Log.d(TAG, "80");
                FileInputStream fi = new FileInputStream(sourcePath);
                Log.d(TAG, "82");
                origin = new BufferedInputStream(fi, BUFFER);
                ZipEntry entry = new ZipEntry(getLastPathComponent(sourcePath));
                Log.d(TAG, "85");
                Log.d(TAG, entry.getName() + " " + entry.getName().contains(".zip") + " wo ist der Log");
                Log.d(TAG, entry.getName() + " so true ");

                // if (entry.getName().contains(".zip")){
                    Log.d(TAG, entry.getName() + " so true ");

                    Log.d(TAG, "91");
                    entry.setTime(sourceFile.lastModified()); // to keep modification time after unzipping

                    Log.d(TAG, "95");
                out.putNextEntry(entry);

                    Log.d(TAG, "98");
                int count;
                while ((count = origin.read(data, 0, BUFFER)) != -1) {

                    Log.d(TAG, "101");
                    out.write(data, 0, count);
                }}


            Log.d(TAG, "105");
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /*
     *
     * Zips a subfolder
     *
     */

    private static void zipSubFolder(ZipOutputStream out, File folder,
                              int basePathLength, Context ctx) throws IOException {
        if(! (folder.getAbsolutePath().equals(ctx.getFilesDir().getParent()) || folder.getAbsolutePath().contains("files") || folder.getAbsolutePath().contains("shared_prefs"))) {
            Log.d(TAG, folder.getAbsolutePath() + " is not to be zipped");
            Log.d(TAG, "folder.getAbsolutePath().equals(ctx.getFilesDir().getParent() " + folder.getAbsolutePath().equals(ctx.getFilesDir().getParent()) + " is the reason");
            Log.d(TAG, "folder.getAbsolutePath().contains(\"files\") " + folder.getAbsolutePath().contains("files") + " is not to be zipped");
            Log.d(TAG, "folder.getAbsolutePath().contains(\"shared_prefs\") " +  folder.getAbsolutePath().contains("shared_prefs") + " is not to be zipped");
            // Log.d(TAG, "folder.getAbsolutePath().contains(.zip) " +  folder.getAbsolutePath().contains(".zip") + " is not to be zipped");


            return;
        }
        Log.d(TAG, folder.getAbsolutePath() + " is to be zipped");
        final int BUFFER = 2048;

        File[] fileList = folder.listFiles();
        BufferedInputStream origin = null;
        for (File file : fileList) {
            if (file.isDirectory()) {
                Log.d(TAG, file.getAbsolutePath() + " is directory. Entering zipSubFolder");
                zipSubFolder(out, file, basePathLength, ctx);
            } else {

                Log.d(TAG, file.getAbsolutePath() + " is not directory.");

                byte data[] = new byte[BUFFER];
                Log.d(TAG, "149");
                String unmodifiedFilePath = file.getPath();
                String relativePath = unmodifiedFilePath
                        .substring(basePathLength);
                if(!relativePath.contains(".zip")) {
                    Log.d(TAG, "zipping");
                    FileInputStream fi = new FileInputStream(unmodifiedFilePath);
                    Log.d(TAG, "157");

                    origin = new BufferedInputStream(fi, BUFFER);
                    ZipEntry entry = new ZipEntry(relativePath);
                    Log.d(TAG, entry.getName() + " " + entry.getName().contains(".zip") + " wo ist der Log");
                    Log.d(TAG, entry.getName() + " so true ");

                    entry.setTime(file.lastModified()); // to keep modification time after unzipping
                    out.putNextEntry(entry);
                    int count;
                    while ((count = origin.read(data, 0, BUFFER)) != -1) {
                        out.write(data, 0, count);
                    }
                    origin.close();
                }
            }
        }
    }

    /*
     * gets the last path component
     *
     * Example: getLastPathComponent("downloads/example/fileToZip");
     * Result: "fileToZip"
     */
    public static String getLastPathComponent(String filePath) {
        String[] segments = filePath.split("/");
        if (segments.length == 0)
            return "";
        String lastPathComponent = segments[segments.length - 1];
        return lastPathComponent;
    }

    public static void zip(List<File> files, File zipFile ) throws IOException {
        final int BUFFER_SIZE = 4096;

        BufferedInputStream origin;

        try (ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)))) {
            byte[] data = new byte[BUFFER_SIZE];

            for (File file : files) {
                if (!file.isFile()) {
                    continue;
                }
                FileInputStream fileInputStream = new FileInputStream(file);

                origin = new BufferedInputStream(fileInputStream, BUFFER_SIZE);

                String filePath = file.getAbsolutePath();

                try {
                    ZipEntry entry = new ZipEntry(filePath.substring(filePath.lastIndexOf("/") + 1));

                    out.putNextEntry(entry);

                    int count;
                    while ((count = origin.read(data, 0, BUFFER_SIZE)) != -1) {
                        out.write(data, 0, count);
                    }
                } finally {
                    origin.close();
                }
            }
        }
    }

    public static boolean unpackZip(String path, String zipname)
    {
        InputStream is;
        ZipInputStream zis;
        try
        {
            String filename;
            is = new FileInputStream(path + "files/" + zipname);
            zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry entry;
            byte[] buffer = new byte[1024];
            int count;

            while ((entry = zis.getNextEntry()) != null)
            {
                filename = entry.getName();
                Log.d(TAG,"filename: " + filename);
                Log.d(TAG, "entry.isDirectory(): " + entry.isDirectory());
                // Need to create directories if not exists, otherwise throws Exception
                if (entry.isDirectory()) {
                    File fmd = new File(path + filename);
                    //mkdirs : Creates the directory named by this abstract pathname, including any necessary but nonexistent parent directories
                    fmd.mkdirs();
                    Log.d(TAG,"fmd: " + fmd.getAbsolutePath());
                    continue;
                }

                Log.d(TAG, "path + filename: " + path + filename);
                FileOutputStream fout = new FileOutputStream(path + filename);

                while ((count = zis.read(buffer)) != -1)
                {
                    fout.write(buffer, 0, count);
                }

                fout.close();
                zis.closeEntry();
            }

            zis.close();
        }
        catch(IOException e)
        {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public static class Directories {

        /**
         * Returns the Base Folder (Private App File Directory)
         *
         * @param ctx The App Context
         * @return Path with trailing slash
         */
        public static String getBaseFolderPath(Context ctx) {
            // return getExternalBaseDirectoryPath();
            return ctx.getFilesDir() + "/";
        }

        public static File getSharedPrefsDirectory(Context context) {
            File[] dirs = context.getFilesDir().getParentFile().listFiles();
            for (int i = 0; i < dirs.length; i++) {
                if (dirs[i].getName().equals("shared_prefs")) {
                    return dirs[i];
                }
            }
            return null;
        }

        /**
         * Returns the External Folder Path (Shared File Directory)
         * Might be on SD Card
         *
         * @return Path with trailing slash
         */

        public static String getExternalBaseDirectoryPath() {
            String app_folder_path = Environment.getExternalStorageDirectory().toString() + "/simra/";
            File dir = new File(app_folder_path);
            if (!dir.exists() && !dir.mkdirs()) {

            }
            return app_folder_path;
        }

        /**
         * Returns the Path to the Picture Cache Directory (Shared File Directory)
         * Might be on SD Card
         *
         * @return Path with trailing slash
         */
        public static String getPictureCacheDirectoryPath(Context context) {
            String app_folder_path = getBaseFolderPath(context) + "images/";
            File dir = new File(app_folder_path);
            if (!dir.exists() && !dir.mkdirs()) {

            }
            return app_folder_path;
        }
    }

    /**
     * Well known Files
     * Using this should be only temporary
     * Those we need access to from all over the app, because the access was never centralized...
     */
    public static class Files {
        public static String getFileInfoLine() {
            return BuildConfig.VERSION_CODE + "#1" + System.lineSeparator();
        }
        public static String getFileInfoLine(int modelVersion) {
            return BuildConfig.VERSION_CODE + "#1#" + modelVersion + System.lineSeparator();
        }
        public static String getMetaDataFilePath(Context context) {
            return IOUtils.Directories.getBaseFolderPath(context) + "metaData.csv";
        }

        public static File getMetaDataFile(Context context) {
            return new File(getMetaDataFilePath(context));
        }

        public static String getGPSLogFileName(int rideId, boolean isTempFile) {
            return (isTempFile ? "Temp" : "") + rideId + "_accGps.csv";
        }

        public static String getGPSLogFilePath(int rideId, boolean isTempFile, Context context) {
            return IOUtils.Directories.getBaseFolderPath(context) + getGPSLogFileName(rideId, isTempFile);
        }

        public static File getGPSLogFile(int rideId, boolean isTempFile, Context context) {
            return new File(getGPSLogFilePath(rideId, isTempFile, context));
        }

        public static File getRegionsFile(Context context) {
            return new File(IOUtils.Directories.getBaseFolderPath(context) + "simRa_regions.config");
        }

        public static File getDENewsFile(Context context) {
            return new File(IOUtils.Directories.getBaseFolderPath(context) + "simRa_news_de.config");
        }
        public static File getENNewsFile(Context context) {
            return new File(IOUtils.Directories.getBaseFolderPath(context) + "simRa_news_en.config");
        }

    }
}
