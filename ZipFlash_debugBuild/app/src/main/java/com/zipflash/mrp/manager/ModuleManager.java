package com.zipflash.mrp.manager;

import android.content.Context;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import org.json.JSONObject;

public class ModuleManager {

    private Context context;

    public ModuleManager(Context ctx) {
        this.context = ctx;
    }

    public File getModulesDir() {
        File dir = new File(context.getFilesDir(), "modules");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public File[] listModules() {
        return getModulesDir().listFiles();
    }

    public void addModule(File zipFile) throws IOException {
        File dest = new File(getModulesDir(), zipFile.getName());
        copyFile(zipFile, dest);
        saveStatus(dest.getName(), true, false);
    }

    private void saveStatus(String module, boolean enabled, boolean synced) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("enabled", enabled);
            obj.put("synced", synced);
            FileWriter fw = new FileWriter(new File(getModulesDir(), module + ".json"));
            fw.write(obj.toString());
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void copyFile(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);
        byte[] buf = new byte[4096];
        int len;
        while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
        in.close();
        out.close();
    }
}
