package com.zipflash.mrp.helper;

import android.app.ProgressDialog;
import android.content.Context;

public class UiHelper {

    public static ProgressDialog showLoading(Context ctx, String message) {
        ProgressDialog dialog = new ProgressDialog(ctx);
        dialog.setMessage(message);
        dialog.setCancelable(false);
        dialog.show();
        return dialog;
    }
}
