package de.tuberlin.mcc.simra.app.util;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import de.tuberlin.mcc.simra.app.BuildConfig;

public class IOUtils {
    public static boolean isDirectoryEmpty(String path) {
        File dir = new File(path);
        if (dir.isDirectory()) {
            return dir.listFiles().length == 0;
        }
        return false;
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
        /*
        public static String getExternalBaseDirectoryPath() {
            String app_folder_path = Environment.getExternalStorageDirectory().toString() + "/simra/";
            File dir = new File(app_folder_path);
            if (!dir.exists() && !dir.mkdirs()) {

            }
            return app_folder_path;
        }
         */
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
