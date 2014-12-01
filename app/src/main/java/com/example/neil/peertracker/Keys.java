package com.example.neil.peertracker;

public class Keys {

    public static final String actionPrefix = Keys.class.getPackage().getName() + ".";

    static class Actions {
        public static final String ACTION_ERROR = actionPrefix + "ACTION_ERROR";
        public static final String ACTION_DEVICE_FOUND = actionPrefix + "ACTION_DEVICE_FOUND";
        public static final String ACTION_DEVICE_LOST = actionPrefix + "ACTION_DEVICE_LOST";
        public static final String ACTION_CONNECTED_TO_NETWORK = actionPrefix + "ACTION_CONNECTED_TO_NETWORK";
        public static final String ACTION_PASSWORD_IS_INCORRECT = actionPrefix + "ACTION_PASSWORD_IS_INCORRECT";
    }

    class Extras {
        public static final String EXTRA_ERROR = "extra_error";
        public static final String EXTRA_DEVICE_ID = "extra_deviceId";
        public static final String EXTRA_BUS_NAME = "extra_BusName";
        public static final String EXTRA_NETWORK_SSID = "extra_network_ssid";
    }

}
