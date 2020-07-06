package de.tuberlin.mcc.simra.app.entities;

public class MetaData {

    public static class STATE {
        /**
         * The ride is saved locally and was not yet annotated
         */
        public static final int JUST_RECORDED = 0;
        /**
         * The ride is saved locally and was annotated by the user
         */
        public static final int ANNOTATED = 1;
        /**
         * The ride is was synced with the server and can not be edited anymore
         */
        public static final int SYNCED = 2;
    }
}
