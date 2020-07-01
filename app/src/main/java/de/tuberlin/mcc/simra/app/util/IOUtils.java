package de.tuberlin.mcc.simra.app.util;

import android.content.Context;
import android.os.Environment;

import java.io.File;

public class IOUtils {
    public static boolean isDirectoryEmpty(String path) {
        File dir = new File(path);
        if (dir.isDirectory()) {
            return dir.listFiles().length == 0;
        }
        return false;
    }

    public static void deleteDirectoryContent(String path) {
        File dir = new File(path);
        if (dir.isDirectory()) {
            for (File file : dir.listFiles())
                if (!file.isDirectory())
                    file.delete();
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
            return ctx.getFilesDir() + "/";
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
        public static String getPictureCacheDirectoryPath() {
            String app_folder_path = getExternalBaseDirectoryPath() + "images/";
            File dir = new File(app_folder_path);
            if (!dir.exists() && !dir.mkdirs()) {

            }
            return app_folder_path;
        }
    }
}
