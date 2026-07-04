package id.donugr.admob.util;

import android.text.TextUtils;
import java.util.regex.Pattern;

public final class RuntimeIdValidator {
    private static final Pattern SAFE_RUNTIME_ID_PATTERN = Pattern.compile("^[A-Za-z0-9._:-]{1,128}$");

    private RuntimeIdValidator() {
    }

    public static boolean isSafe(String value) {
        return !TextUtils.isEmpty(value) && SAFE_RUNTIME_ID_PATTERN.matcher(value).matches();
    }
}
