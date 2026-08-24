package com.cladepa.contextmenu;

import android.util.Base64;

public class HelperScripts {

    public static final String DISCOVER_MENU_SH =
        "#!/system/bin/sh\n" +
        "unset LD_LIBRARY_PATH LD_PRELOAD\n" +
        "\n" +
        "BUFFER_DIR=\"$1\"\n" +
        "[ -z \"$BUFFER_DIR\" ] && BUFFER_DIR=\"/storage/emulated/0/Zametki_ALL/claude_bufer\"\n" +
        "\n" +
        "MOUSE_NAMES=\"$2\"\n" +
        "[ -z \"$MOUSE_NAMES\" ] && MOUSE_NAMES=\"HAOBO Technology USB Composite Device\"\n" +
        "\n" +
        "MY_NAME=\"$3\"\n" +
        "[ -z \"$MY_NAME\" ] && MY_NAME=\"tvbox\"\n" +
        "\n" +
        "echo \"$MOUSE_NAMES\" | tr ',' '\\n' | while read -r mname; do\n" +
        "  [ -z \"$mname\" ] && continue\n" +
        "  DEV=\"\"\n" +
        "  for f in /sys/class/input/event*/device/name; do\n" +
        "    n=$(cat \"$f\")\n" +
        "    if [ \"$n\" = \"$mname\" ]; then\n" +
        "      d=\"${f%/device/name}\"\n" +
        "      DEV=\"/dev/input/${d##*/}\"\n" +
        "      break\n" +
        "    fi\n" +
        "  done\n" +
        "  if [ -n \"$DEV\" ]; then\n" +
        "    sendevent \"$DEV\" 2 0 1\n" +
        "    sendevent \"$DEV\" 0 0 0\n" +
        "    sendevent \"$DEV\" 2 0 -1\n" +
        "    sendevent \"$DEV\" 0 0 0\n" +
        "  fi\n" +
        "done\n" +
        "sleep 0.05\n" +
        "\n" +
        "LINE=$(dumpsys input | grep -oE 'xCursorPosition=[0-9.]+, yCursorPosition=[0-9.]+.*age=[0-9]+ms' | awk -F'age=' '{split($2,a,\"ms\"); age=a[1]+0; if (minage==\"\" || age<minage) {minage=age; line=$0}} END{print line}')\n" +
        "X=$(echo \"$LINE\" | sed -n 's/.*xCursorPosition=\\([0-9]*\\)\\..*/\\1/p')\n" +
        "Y=$(echo \"$LINE\" | sed -n 's/.*yCursorPosition=\\([0-9]*\\)\\..*/\\1/p')\n" +
        "\n" +
        "DEVICES=\"\"\n" +
        "add_device() {\n" +
        "  name=\"$1\"\n" +
        "  case \",$DEVICES,\" in\n" +
        "    *\",$name,\"*) return ;;\n" +
        "  esac\n" +
        "  if [ -z \"$DEVICES\" ]; then DEVICES=\"$name\"; else DEVICES=\"$DEVICES,$name\"; fi\n" +
        "}\n" +
        "\n" +
        "for f in \"$BUFFER_DIR\"/*.txt; do\n" +
        "  [ -e \"$f\" ] || continue\n" +
        "  base=$(basename \"$f\" .txt)\n" +
        "  case \"$base\" in\n" +
        "    \"$MY_NAME\") continue ;;\n" +
        "    to_all) continue ;;\n" +
        "    \"to_$MY_NAME\") continue ;;\n" +
        "    clip_hist-*) continue ;;\n" +
        "    to_*)\n" +
        "      name=${base#to_}\n" +
        "      add_device \"$name\"\n" +
        "      ;;\n" +
        "    *)\n" +
        "      add_device \"$base\"\n" +
        "      ;;\n" +
        "  esac\n" +
        "done\n" +
        "\n" +
        "echo \"${X}|${Y}|${DEVICES}\"\n";

    public static final String NOTIFY_TVBOX_SH =
        "#!/bin/sh\n" +
        "am broadcast -a tv.contextmenu.FLASH -n com.cladepa.contextmenu/.CommandReceiver --user 0\n";

    public static String install(String targetPath, String content) {
        String b64 = Base64.encodeToString(content.getBytes(), Base64.NO_WRAP);
        String cmd = "echo '" + b64 + "' | base64 -d > '" + targetPath
                + "' && chmod 755 '" + targetPath + "'";
        return Root.exec(cmd);
    }
}
