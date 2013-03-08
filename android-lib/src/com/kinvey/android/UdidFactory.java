/**
 * Copyright (c) 2013, Kinvey, Inc. All rights reserved.
 *
 * This software contains valuable confidential and proprietary information of
 * KINVEY, INC and is subject to applicable licensing agreements.
 * Unauthorized reproduction, transmission or distribution of this file and its
 * contents is a violation of applicable laws.
 */

package com.kinvey.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

/**
 * @see <a href="http://stackoverflow.com/questions/2785485/is-there-a-unique-android-device-id">
 *       http://stackoverflow.com/questions/2785485/is-there-a-unique-android-device-id
 *     </a>
 */
class UdidFactory {

    /** Constant <code>PREFS_FILE="device_id.xml"</code> */
    protected static final String PREFS_FILE = "device_id.xml";
    /** Constant <code>PREFS_DEVICE_ID="device_id"</code> */
    protected static final String PREFS_DEVICE_ID = "device_id";

    /** Constant <code>uuid</code> */
    protected static UUID uuid;

    /**
     * <p>Constructor for UdidFactory.</p>
     *
     * @param context a {@link android.content.Context} object.
     */
    public UdidFactory(Context context) {

        if (uuid == null) {
            synchronized (UdidFactory.class) {
                if (uuid == null) {
                    final SharedPreferences prefs = context
                            .getSharedPreferences(PREFS_FILE, 0);
                    final String id = prefs.getString(PREFS_DEVICE_ID, null);

                    if (id != null) {
                        // Use the ids previously computed and stored in the
                        // prefs file
                        uuid = UUID.fromString(id);

                    } else {

                        final String androidId = Secure
                                .getString(context.getContentResolver(),
                                        Secure.ANDROID_ID);

                        // Use the Android ID unless it's broken, in which case
                        // fallback on deviceId, unless it's not available, then
                        // fallback on a random number which we store to a prefs file
                        try {
                            if (!"9774d56d682e549c".equals(androidId)) {
                                uuid = UUID.nameUUIDFromBytes(androidId.getBytes("utf8"));
                            } else {
                                final String deviceId = ((TelephonyManager) context
                                        .getSystemService(Context.TELEPHONY_SERVICE))
                                        .getDeviceId();
                                uuid = deviceId != null ? UUID.nameUUIDFromBytes(deviceId.getBytes("utf8"))
                                        : UUID.randomUUID();
                            }
                        } catch (UnsupportedEncodingException e) {
                            throw new RuntimeException(e);
                        }

                        // Write the value out to the prefs file
                        prefs.edit().putString(PREFS_DEVICE_ID, uuid.toString()).commit();

                    }

                }
            }
        }

    }



    /**
     * Returns a unique UUID for the current android device. As with all UUIDs,
     * this unique ID is "very highly likely" to be unique across all Android
     * devices. Much more so than ANDROID_ID is.
     *
     * The UUID is generated by using ANDROID_ID as the base key if appropriate,
     * falling back on TelephonyManager.getDeviceID() if ANDROID_ID is known to
     * be incorrect, and finally falling back on a random UUID that's persisted
     * to SharedPreferences if getDeviceID() does not return a usable value.
     *
     * In some rare circumstances, this ID may change. In particular, if the
     * device is factory reset a new device ID may be generated. In addition, if
     * a user upgrades their phone from certain buggy implementations of Android
     * 2.2 to a newer, non-buggy version of Android, the device ID may change.
     * Or, if a user uninstalls your app on a device that has neither a proper
     * Android ID nor a Device ID, this ID may change on reinstallation.
     *
     * Note that if the code falls back on using TelephonyManager.getDeviceId(),
     * the resulting ID will NOT change after a factory reset. Something to be
     * aware of.
     *
     * Works around a bug in Android 2.2 for many devices when using ANDROID_ID
     * directly.
     *
     * @see <a href="http://code.google.com/p/android/issues/detail?id=10603">http://code.google.com/p/android/issues/detail?id=10603</a>
     * @return a UUID that may be used to uniquely identify your device for most
     *         purposes.
     */
    public UUID getDeviceUuid() {
        return uuid;
    }


    /**
     * <p>getDeviceInfoHeader</p>
     *
     * @param context a {@link android.content.Context} object.
     * @return a {@link java.lang.String} object.
     */
    public String getDeviceInfoHeader(final Context context) {
        // Device Manufacturer
        String ma;
        try {
            ma = Build.class.getDeclaredField("MANUFACTURER").get(Build.class).toString().replace(" ", "_");
        } catch (final Throwable e) {
            ma = "UNKNOWN";
        }
        // Device Model
        final String devModel = Build.MODEL.replace(" ", "_");
        // OS name
        final String osName = "Android";
        // OS Version
        final String osVersion = Build.VERSION.RELEASE.replace(" ", "_");
        // UDID
        final UUID udid = getDeviceUuid();

        return String.format("%s/%s %s %s %s", ma, devModel, osName, osVersion, udid.toString());
    }



}
