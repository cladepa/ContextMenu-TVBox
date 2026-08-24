package com.cladepa.contextmenu;

import java.util.ArrayList;
import java.util.List;

public class InputDeviceScanner {

    private static final String AWK_FIND_BY_CAPS =
        "awk -v caps=\"%s\" '"
        + "BEGIN { n = split(caps, req, \" \") } "
        + "/^add device [0-9]+: / { if (path != \"\") check(); path = $NF; devname = \"\"; delete found; next } "
        + "/^  name:/ { devname = $0; sub(/^  name: */, \"\", devname); gsub(/\"/, \"\", devname); next } "
        + "{ for (i=1;i<=NF;i++) found[$i] = 1 } "
        + "function check(   i,ok) { ok = 1; for (i=1;i<=n;i++) if (!(req[i] in found)) ok = 0; "
        + "if (ok && devname != \"\") print devname } "
        + "END { if (path != \"\") check() }'";

    public static String[] scanMouseCapable() {
        String cmd = "getevent -lp | " + String.format(AWK_FIND_BY_CAPS, "REL_X REL_Y BTN_MOUSE");
        return splitLines(Root.exec(cmd));
    }

    public static String[] scanKeyboardCapable() {
        String cmd = "getevent -lp | " + String.format(AWK_FIND_BY_CAPS, "KEY_A KEY_LEFTCTRL");
        return splitLines(Root.exec(cmd));
    }

    public static String[] scanAllPresentNames() {
        String cmd = "for f in /sys/class/input/event*/device/name; do [ -e \"$f\" ] && cat \"$f\"; done";
        return splitLines(Root.exec(cmd));
    }

    private static String[] splitLines(String out) {
        if (out == null || out.trim().length() == 0) return new String[0];
        String[] lines = out.split("\n");
        List<String> result = new ArrayList<String>();
        for (String l : lines) {
            String t = l.trim();
            if (t.length() > 0 && !t.startsWith("ERR:") && !t.startsWith("EXCEPTION:")) result.add(t);
        }
        return result.toArray(new String[0]);
    }
}
