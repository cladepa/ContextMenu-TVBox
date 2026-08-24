package com.cladepa.contextmenu;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Root {
    public static String exec(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(
                    new String[]{"/system/xbin/su", "0", "sh", "-c", cmd});
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
            BufferedReader er = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            StringBuilder esb = new StringBuilder();
            while ((line = er.readLine()) != null) esb.append(line).append("\n");
            p.waitFor();
            String out = sb.toString().trim();
            String err = esb.toString().trim();
            if (!out.isEmpty()) return out;
            if (!err.isEmpty()) return "ERR: " + err;
            return "";
        } catch (Exception e) {
            return "EXCEPTION: " + e.getMessage();
        }
    }
}
