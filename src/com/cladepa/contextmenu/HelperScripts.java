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
        "for f in /sys/class/input/event*/device/name; do\n" +
        "  n=$(cat \"$f\")\n" +
        "  case \",$MOUSE_NAMES,\" in\n" +
        "    *\",$n,\"*)\n" +
        "      d=\"${f%/device/name}\"\n" +
        "      DEV=\"/dev/input/${d##*/}\"\n" +
        "      sendevent \"$DEV\" 2 0 1\n" +
        "      sendevent \"$DEV\" 0 0 0\n" +
        "      sendevent \"$DEV\" 2 0 -1\n" +
        "      sendevent \"$DEV\" 0 0 0\n" +
        "      ;;\n" +
        "  esac\n" +
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
    public static final String CLIPBOARD_MONITOR_SH =
        "#!/system/bin/sh\n" +
        "# Демон мониторинга системного буфера обмена\n" +
        "# Добавляет все изменения буфера в историю, независимо от источника\n" +
        "\n" +
        "unset LD_LIBRARY_PATH LD_PRELOAD\n" +
        "\n" +
        "BUFFER_DIR=\"${1:-/storage/emulated/0/Zametki_ALL/claude_bufer}\"\n" +
        "DEVICE_NAME=\"${2:-tvbox}\"\n" +
        "POLL_INTERVAL=\"${3:-2}\"\n" +
        "\n" +
        "# Валидация директории\n" +
        "[ -d \"$BUFFER_DIR\" ] || { echo \"Buffer dir not found: $BUFFER_DIR\" >&2; exit 1; }\n" +
        "\n" +
        "# Кэш для отслеживания изменений буфера\n" +
        "CACHE_FILE=\"/data/local/tmp/clipboard_daemon_cache_${DEVICE_NAME}.txt\"\n" +
        "touch \"$CACHE_FILE\"\n" +
        "\n" +
        "# Функция для чтения буфера через Clip\n" +
        "get_clipboard() {\n" +
        "    ANDROID_ROOT=/system ANDROID_DATA=/data CLASSPATH=/data/local/tmp/clip.jar \\\n" +
        "        app_process /system/bin Clip 2>/dev/null\n" +
        "}\n" +
        "\n" +
        "# Функция для добавления в историю\n" +
        "add_to_history() {\n" +
        "    local content=\"$1\"\n" +
        "    local hist_file=\"$BUFFER_DIR/clip_hist-${DEVICE_NAME}.txt\"\n" +
        "    local ts=$(date '+%Y-%m-%dT%H:%M:%S')\n" +
        "    \n" +
        "    printf '\\n===ENTRY %s===\\n%s\\n' \"$ts\" \"$content\" >> \"$hist_file\"\n" +
        "}\n" +
        "\n" +
        "# Основной цикл мониторинга\n" +
        "while true; do\n" +
        "    # Читаем текущий буфер\n" +
        "    current=$(get_clipboard)\n" +
        "    \n" +
        "    # Читаем кэшированное значение\n" +
        "    cached=$(cat \"$CACHE_FILE\" 2>/dev/null)\n" +
        "    \n" +
        "    # Если буфер изменился\n" +
        "    if [ \"$current\" != \"$cached\" ] && [ -n \"$current\" ]; then\n" +
        "        # Сохраняем в основной файл устройства\n" +
        "        printf '%s' \"$current\" > \"$BUFFER_DIR/${DEVICE_NAME}.txt\"\n" +
        "        \n" +
        "        # Добавляем в историю\n" +
        "        add_to_history \"$current\"\n" +
        "        \n" +
        "        # Обновляем кэш\n" +
        "        printf '%s' \"$current\" > \"$CACHE_FILE\"\n" +
        "    fi\n" +
        "    \n" +
        "    sleep \"$POLL_INTERVAL\"\n" +
        "done\n";

    public static String install(String targetPath, String content) {
        String b64 = Base64.encodeToString(content.getBytes(), Base64.NO_WRAP);
        String cmd = "echo '" + b64 + "' | base64 -d > '" + targetPath
                + "' && chmod 755 '" + targetPath + "'";
        return Root.exec(cmd);
    }
}
