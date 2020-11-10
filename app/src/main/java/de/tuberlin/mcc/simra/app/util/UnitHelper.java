package de.tuberlin.mcc.simra.app.util;

import android.content.Context;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.tuberlin.mcc.simra.app.R;

public class UnitHelper {
    public static String getShortTranslationForUnit(DISTANCE unit, Context context) {
        switch (unit) {
            case IMPERIAL:
                return context.getString(R.string.feet_short);
            default:
                return context.getString(R.string.meter_short);
        }
    }

    public static long convertMeterToFeet(int meter) {
        return Math.round(meter * 3.28);
    }

    public static long convertFeetToMeter(int feet) {
        return Math.round(feet / 3.28);
    }

    public enum DISTANCE {
        /**
         * Metric System using meter and cm
         */
        METRIC("m"),
        /**
         * Imperial System using miles and inches
         */
        IMPERIAL("ft");

        private static final Map<String, DISTANCE> ENUM_MAP;

        static {
            // Build an immutable map of String name to enum pairs. (for faster access)
            Map<String, DISTANCE> map = new ConcurrentHashMap<>();
            for (DISTANCE instance : DISTANCE.values()) {
                map.put(instance.getName(), instance);
            }
            ENUM_MAP = Collections.unmodifiableMap(map);
        }

        private String name;

        DISTANCE(String unit_name) {
            name = unit_name;
        }


        public static DISTANCE parseFromString(String name) {
            return ENUM_MAP.get(name);
        }

        public String getName() {
            return this.name;
        }
    }

}
