package com.app.superpos.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.*;

import com.app.superpos.constants.DefaultValues;

public class SharedPreferencesHelper {
    public static SharedPreferencesHelper instance = new SharedPreferencesHelper();

    static class Keys {
        static String printerAddress = "SharedPreferences.UserSetting.PrinterAddress";
    }

    private String preferencesName = "SharedPreferences.UserSetting";

    public void storePrinterAddress(Context context, String value) {
        SharedPreferences sharedPreference = context.getSharedPreferences(
                preferencesName,
                Context.MODE_PRIVATE
        );
        Editor editor = sharedPreference.edit();
        editor.putString(Keys.printerAddress, value);
        editor.apply();
    }

    public String getPrinterAddress(Context context) {
        SharedPreferences sharedPreference = context.getSharedPreferences(
                preferencesName,
                Context.MODE_PRIVATE
        );
        String printerAddress = sharedPreference.getString(
                Keys.printerAddress,
                DefaultValues.emptyString
        );
        if (printerAddress == null) {
            return DefaultValues.emptyString;
        }
        return printerAddress;
    }
}
