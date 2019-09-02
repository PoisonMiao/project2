package com.ifchange.tob.common.helper;

import com.google.common.collect.ImmutableMap;
import com.ifchange.tob.common.support.DateFormat;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class JvmOSHelperTest {

    @Test
    public void isWindows() {
        System.out.println(JvmOSHelper.isWindows());
    }

    @Test
    public void building() throws IOException {
        String btJSON = JvmOSHelper.projectDir() + "/src/main/resources/build-time.json";
        File btfJSON = new File(btJSON);
        if(btfJSON.exists()) {
            btfJSON.delete();
        }
        btfJSON.createNewFile();
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(btfJSON);
            fos.write(JsonHelper.toJSONBytes(ImmutableMap.of("time", DateHelper.now(DateFormat.StrikeDateTime))));
            fos.flush();
        } finally {
            BytesHelper.close(fos);
        }
    }

    @Test
    public void projectDir() {
    }
}