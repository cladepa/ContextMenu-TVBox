package com.cladepa.contextmenu;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {

    private TextView tvResult;
    private EditText etBufferDir;
    private EditText etDeviceName;
    private TextView tvDeviceNameStatus;
    private EditText etStfolderPath;
    private TextView tvStfolderStatus;
    private EditText etFavorites;

    private LinearLayout llDeviceChecklist;
    private TextView tvScanStatus;
    private List<CheckBox> deviceCheckBoxes = new ArrayList<CheckBox>();

    private LinearLayout llMouseChecklist;
    private LinearLayout llKeyboardChecklist;
    private TextView tvInputScanStatus;
    private List<CheckBox> mouseCheckBoxes = new ArrayList<CheckBox>();
    private List<CheckBox> keyboardCheckBoxes = new ArrayList<CheckBox>();

    private EditText etFlashCount;
    private EditText etFlashPeriod;
    private EditText etFlashSize;
    private EditText etHistoryFontSize;
    private Button[] cornerButtons;
    private View[] colorSwatches;

    private String selectedCorner;
    private int selectedColor;

    private static final String[] CORNERS = {"TL", "TR", "BL", "BR"};
    private static final String[] CORNER_LABELS = {"\u2196 ВЛ", "\u2197 ВП", "\u2199 НЛ", "\u2198 НП"};
    private static final int[] PRESET_COLORS = {
            0xFF4CAF50, 0xFFF44336, 0xFF2196F3, 0xFFFFEB3B, 0xFFFFFFFF, 0xFFFF9800
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#121212"));
        root.setPadding(dp(32), dp(32), dp(32), dp(32));

        // --- версия ---
        TextView tvVersion = new TextView(this);
        try {
            String vName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            tvVersion.setText("build: " + vName);
        } catch (Exception e) {
            tvVersion.setText("build: ???");
        }
        tvVersion.setTextColor(Color.parseColor("#4CAF50"));
        tvVersion.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvVersion.setGravity(Gravity.CENTER);
        addWithBottomMargin(root, tvVersion, 16);

        // --- разрешение оверлея ---
        final TextView tvPerm = new TextView(this);
        tvPerm.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvPerm.setGravity(Gravity.CENTER);
        addWithBottomMargin(root, tvPerm, 24);

        // --- имя устройства ---
        root.addView(sectionLabel("Имя этого устройства (используется в именах файлов буфера "
                + "и истории — при смене файлы переименуются автоматически, дублей не будет)"));
        etDeviceName = new EditText(this);
        etDeviceName.setText(Prefs.deviceName(this));
        etDeviceName.setTextColor(Color.WHITE);
        addWithBottomMargin(root, etDeviceName, 8);

        Button btnSaveDeviceName = new Button(this);
        btnSaveDeviceName.setText("Сохранить имя (переименует файлы)");
        addWithBottomMargin(root, btnSaveDeviceName, 8);

        tvDeviceNameStatus = new TextView(this);
        tvDeviceNameStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        addWithBottomMargin(root, tvDeviceNameStatus, 32);

        btnSaveDeviceName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                renameDevice();
            }
        });

        // --- путь буфера ---
        root.addView(sectionLabel("Папка синхронизации буфера"));
        etBufferDir = new EditText(this);
        etBufferDir.setText(Prefs.bufferDir(this));
        etBufferDir.setTextColor(Color.WHITE);
        addWithBottomMargin(root, etBufferDir, 8);

        Button btnSaveDir = new Button(this);
        btnSaveDir.setText("Сохранить и проверить путь");
        addWithBottomMargin(root, btnSaveDir, 8);

        tvBufferStatus = new TextView(this);
        tvBufferStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        addWithBottomMargin(root, tvBufferStatus, 32);

        btnSaveDir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAndCheckBufferDir();
            }
        });

        // --- путь до .stfolder ---
        root.addView(sectionLabel("Папка .stfolder (для скрипта notify_tvbox.sh — Syncthing-Fork "
                + "выполняет скрипты именно отсюда после синхронизации, это НЕ то же самое, что папка буфера)"));
        etStfolderPath = new EditText(this);
        etStfolderPath.setText(Prefs.stfolderPath(this));
        etStfolderPath.setTextColor(Color.WHITE);
        addWithBottomMargin(root, etStfolderPath, 8);

        Button btnSaveStfolder = new Button(this);
        btnSaveStfolder.setText("Сохранить и проверить .stfolder");
        addWithBottomMargin(root, btnSaveStfolder, 8);

        tvStfolderStatus = new TextView(this);
        tvStfolderStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        addWithBottomMargin(root, tvStfolderStatus, 32);

        btnSaveStfolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAndCheckStfolder();
            }
        });

        // --- избранные устройства (чек-лист + ручной ввод) ---
        root.addView(sectionLabel("Устройства (для \u00abВставить из\u00bb / \u00abОтправить в\u00bb)"));

        Button btnScan = new Button(this);
        btnScan.setText("Сканировать папку");
        addWithBottomMargin(root, btnScan, 8);

        tvScanStatus = new TextView(this);
        tvScanStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvScanStatus.setTextColor(Color.parseColor("#888888"));
        addWithBottomMargin(root, tvScanStatus, 8);

        llDeviceChecklist = new LinearLayout(this);
        llDeviceChecklist.setOrientation(LinearLayout.VERTICAL);
        addWithBottomMargin(root, llDeviceChecklist, 16);

        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanDevices();
            }
        });

        root.addView(sectionLabel("Добавить вручную (через запятую) — на случай, если файла ещё нет, но устройство уже нужно завести заранее"));
        etFavorites = new EditText(this);
        etFavorites.setTextColor(Color.WHITE);
        addWithBottomMargin(root, etFavorites, 4);

        TextView tvNamingHint = new TextView(this);
        tvNamingHint.setText("Имена файлов в папке буфера: <имя>.txt — то, чем устройство делится "
                + "(появится в \u00abВставить из\u00bb); to_<имя>.txt — то, что адресовано этому устройству "
                + "(появится в \u00abОтправить в\u00bb). \u00abto_all.txt\u00bb всегда в основном меню как "
                + "\u00abВсем\u00bb, отдельно настраивать не нужно.");
        tvNamingHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvNamingHint.setTextColor(Color.parseColor("#888888"));
        addWithBottomMargin(root, tvNamingHint, 8);

        Button btnSaveFav = new Button(this);
        btnSaveFav.setText("Сохранить избранные");
        addWithBottomMargin(root, btnSaveFav, 32);
        btnSaveFav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveFavorites();
            }
        });

        // --- устройства ввода (мышь / клавиатура) ---
        root.addView(sectionLabel("Устройства ввода — определяются по возможностям "
                + "(мышь: REL_X/REL_Y/BTN_MOUSE, клавиатура: KEY_A/KEY_LEFTCTRL). "
                + "Новые подключённые донглы добавляются сюда автоматически."));

        root.addView(sectionLabel("Мышь"));
        llMouseChecklist = new LinearLayout(this);
        llMouseChecklist.setOrientation(LinearLayout.VERTICAL);
        addWithBottomMargin(root, llMouseChecklist, 12);

        root.addView(sectionLabel("Клавиатура"));
        llKeyboardChecklist = new LinearLayout(this);
        llKeyboardChecklist.setOrientation(LinearLayout.VERTICAL);
        addWithBottomMargin(root, llKeyboardChecklist, 8);

        Button btnScanInput = new Button(this);
        btnScanInput.setText("Пересканировать устройства ввода");
        addWithBottomMargin(root, btnScanInput, 8);

        Button btnSaveInput = new Button(this);
        btnSaveInput.setText("Сохранить устройства ввода");
        addWithBottomMargin(root, btnSaveInput, 8);

        tvInputScanStatus = new TextView(this);
        tvInputScanStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvInputScanStatus.setTextColor(Color.parseColor("#888888"));
        addWithBottomMargin(root, tvInputScanStatus, 32);

        btnScanInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanInputDevices();
            }
        });
        btnSaveInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveInputDevices();
            }
        });

        // --- настройки мигания ---
        root.addView(sectionLabel("Индикатор (мигание)"));

        LinearLayout cornerRow = new LinearLayout(this);
        cornerRow.setOrientation(LinearLayout.HORIZONTAL);
        cornerButtons = new Button[4];
        selectedCorner = Prefs.flashCorner(this);
        for (int i = 0; i < CORNERS.length; i++) {
            final int idx = i;
            Button b = new Button(this);
            b.setText(CORNER_LABELS[i]);
            b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedCorner = CORNERS[idx];
                    updateCornerHighlight();
                }
            });
            cornerButtons[i] = b;
            cornerRow.addView(b);
        }
        addWithBottomMargin(root, cornerRow, 16);
        updateCornerHighlight();

        etFlashCount = labeledNumberField(root, "Сколько раз мигнуть", Prefs.flashCount(this));
        etFlashPeriod = labeledNumberField(root, "Период (мс на один цикл)", Prefs.flashPeriodMs(this));
        etFlashSize = labeledNumberField(root, "Размер кружка (dp)", Prefs.flashSizeDp(this));
        etHistoryFontSize = labeledNumberField(root, "Размер шрифта в истории (sp)",
                Prefs.historyFontSizeSp(this));

        root.addView(sectionLabel("Цвет"));
        LinearLayout colorRow = new LinearLayout(this);
        colorRow.setOrientation(LinearLayout.HORIZONTAL);
        colorSwatches = new View[PRESET_COLORS.length];
        selectedColor = Prefs.flashColor(this);
        for (int i = 0; i < PRESET_COLORS.length; i++) {
            final int idx = i;
            View sw = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(36), dp(36));
            lp.rightMargin = dp(8);
            sw.setLayoutParams(lp);
            sw.setBackgroundColor(PRESET_COLORS[i]);
            sw.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedColor = PRESET_COLORS[idx];
                    updateColorHighlight();
                }
            });
            colorSwatches[i] = sw;
            colorRow.addView(sw);
        }
        addWithBottomMargin(root, colorRow, 16);
        updateColorHighlight();

        LinearLayout flashBtnRow = new LinearLayout(this);
        flashBtnRow.setOrientation(LinearLayout.HORIZONTAL);

        Button btnSaveFlash = new Button(this);
        btnSaveFlash.setText("Сохранить");
        flashBtnRow.addView(btnSaveFlash);

        Button btnTestFlash = new Button(this);
        btnTestFlash.setText("Тест");
        flashBtnRow.addView(btnTestFlash);

        addWithBottomMargin(root, flashBtnRow, 8);

        btnSaveFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveFlashSettings();
            }
        });

        btnTestFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OverlayManager.flashWith(getApplicationContext(), selectedCorner,
                        readIntOr(etFlashCount, 4), readIntOr(etFlashPeriod, 300),
                        selectedColor, readIntOr(etFlashSize, 16));
            }
        });

        Button btnTestScriptRoot = new Button(this);
        btnTestScriptRoot.setText("Тест: скрипт от root");
        addWithBottomMargin(root, btnTestScriptRoot, 8);

        Button btnTestScriptSyncthing = new Button(this);
        btnTestScriptSyncthing.setText("Тест: скрипт от имени Syncthing-Fork (uid 10131)");
        addWithBottomMargin(root, btnTestScriptSyncthing, 8);

        final TextView tvScriptTestResult = new TextView(this);
        tvScriptTestResult.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvScriptTestResult.setTextColor(Color.parseColor("#888888"));
        addWithBottomMargin(root, tvScriptTestResult, 32);

        btnTestScriptRoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runScriptTest("0", tvScriptTestResult);
            }
        });

        btnTestScriptSyncthing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runScriptTest("10131", tvScriptTestResult);
            }
        });

        // --- вспомогательные скрипты ---
        root.addView(sectionLabel("Вспомогательные скрипты"));
        Button btnInstallScripts = new Button(this);
        btnInstallScripts.setText("Установить/обновить скрипты");
        addWithBottomMargin(root, btnInstallScripts, 8);

        final TextView tvScriptsStatus = new TextView(this);
        tvScriptsStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvScriptsStatus.setTextColor(Color.parseColor("#888888"));
        addWithBottomMargin(root, tvScriptsStatus, 32);

        btnInstallScripts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvScriptsStatus.setText("Устанавливаю...");
                final String stfolder = etStfolderPath.getText().toString().trim();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        HelperScripts.install("/data/local/tmp/discover_menu.sh",
                                HelperScripts.DISCOVER_MENU_SH);
                        final String stfolderCheck = Root.exec("[ -d '" + stfolder + "' ] && echo OK || echo FAIL");
                        if (stfolderCheck.contains("OK")) {
                            HelperScripts.install(stfolder + "/notify_tvbox.sh",
                                    HelperScripts.NOTIFY_TVBOX_SH);
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (stfolderCheck.contains("OK")) {
                                    tvScriptsStatus.setText("Готово: discover_menu.sh в /data/local/tmp, "
                                            + "notify_tvbox.sh в " + stfolder + " ✓");
                                    tvScriptsStatus.setTextColor(Color.parseColor("#4CAF50"));
                                } else {
                                    tvScriptsStatus.setText("discover_menu.sh установлен ✓, но .stfolder ("
                                            + stfolder + ") не найдена — notify_tvbox.sh не положен, "
                                            + "проверь путь выше ⚠");
                                    tvScriptsStatus.setTextColor(Color.parseColor("#FFA000"));
                                }
                            }
                        });
                    }
                }).start();
            }
        });

        // --- root-тест ---
        root.addView(sectionLabel("Диагностика"));
        Button btnTest = new Button(this);
        btnTest.setText("Проверить root из приложения");
        addWithBottomMargin(root, btnTest, 8);

        tvResult = new TextView(this);
        tvResult.setText("Результат появится тут");
        tvResult.setTextColor(Color.WHITE);
        tvResult.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        addWithBottomMargin(root, tvResult, 32);

        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvResult.setText("Выполняю...");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final String result = Root.exec("id");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvResult.setText(result.isEmpty() ? "ПУСТО / ошибка" : result);
                            }
                        });
                    }
                }).start();
            }
        });

        // --- способы вызова ---
        root.addView(sectionLabel("Способы вызова (для tvQuickActions и любых других автоматизаций)"));

        TextView tvMethods = new TextView(this);
        tvMethods.setText(
                "МЕНЮ:\n\n"
                + "1) Activity — подтверждено рабочим через tvQuickActions:\n"
                + "   am start -n com.cladepa.contextmenu/.TriggerActivity\n"
                + "   в tvQuickActions: тип \u00abActivity\u00bb \u2192 com.cladepa.contextmenu \u2192 TriggerActivity\n\n"
                + "2) Broadcast:\n"
                + "   am broadcast -a tv.contextmenu.SHOW -n com.cladepa.contextmenu/.CommandReceiver --user 0\n\n"
                + "МИГАНИЕ (индикатор) — ВАЖНО: обязательно нужен флаг --user 0, иначе непривилегированные "
                + "вызывающие (например Syncthing-Fork) получат SecurityException (broadcast по умолчанию "
                + "целится во всех пользователей системы, это требует спецразрешения, которого у "
                + "обычных приложений нет):\n\n"
                + "1) Broadcast (используется скриптом notify_tvbox.sh по умолчанию):\n"
                + "   am broadcast -a tv.contextmenu.FLASH -n com.cladepa.contextmenu/.CommandReceiver --user 0\n\n"
                + "2) Скрипт для Syncthing-хука (кладётся в .stfolder кнопкой "
                + "\u00abУстановить скрипты\u00bb выше):\n"
                + "   sh <.stfolder>/notify_tvbox.sh"
        );
        tvMethods.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvMethods.setTextColor(Color.parseColor("#888888"));
        root.addView(tvMethods);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);

        checkAndGrantOverlayPermission(tvPerm);
        scanDevices();
        scanInputDevices();
    }

    private void renameDevice() {
        final String oldName = Prefs.deviceName(this);
        final String newName = etDeviceName.getText().toString().trim();
        if (newName.length() == 0) {
            tvDeviceNameStatus.setText("Имя не может быть пустым");
            tvDeviceNameStatus.setTextColor(Color.parseColor("#F44336"));
            return;
        }
        if (newName.equals(oldName)) {
            tvDeviceNameStatus.setText("Имя не изменилось");
            tvDeviceNameStatus.setTextColor(Color.parseColor("#888888"));
            return;
        }
        tvDeviceNameStatus.setText("Переименовываю...");
        tvDeviceNameStatus.setTextColor(Color.parseColor("#FFA000"));
        final String dir = Prefs.bufferDir(this);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Root.exec("mv '" + dir + "/" + oldName + ".txt' '" + dir + "/" + newName
                        + ".txt' 2>/dev/null");
                Root.exec("mv '" + dir + "/to_" + oldName + ".txt' '" + dir + "/to_" + newName
                        + ".txt' 2>/dev/null");
                Root.exec("mv '" + dir + "/clip_hist-" + oldName + ".txt' '" + dir
                        + "/clip_hist-" + newName + ".txt' 2>/dev/null");
                Prefs.setDeviceName(MainActivity.this, newName);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvDeviceNameStatus.setText("Готово: " + oldName + " \u2192 " + newName
                                + " (файлы переименованы, если существовали)");
                        tvDeviceNameStatus.setTextColor(Color.parseColor("#4CAF50"));
                        scanDevices();
                    }
                });
            }
        }).start();
    }

    private void scanDevices() {
        final String dir = etBufferDir.getText().toString().trim();
        final String myName = Prefs.deviceName(this);
        tvScanStatus.setText("Сканирую " + dir + "...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                String script =
                        "MY_NAME='" + myName + "'\n" +
                        "DEVICES=''\n" +
                        "add_device() {\n" +
                        "  name=\"$1\"\n" +
                        "  case \",$DEVICES,\" in\n" +
                        "    *\",$name,\"*) return ;;\n" +
                        "  esac\n" +
                        "  if [ -z \"$DEVICES\" ]; then DEVICES=\"$name\"; else DEVICES=\"$DEVICES,$name\"; fi\n" +
                        "}\n" +
                        "for f in '" + dir + "'/*.txt; do\n" +
                        "  [ -e \"$f\" ] || continue\n" +
                        "  base=$(basename \"$f\" .txt)\n" +
                        "  case \"$base\" in\n" +
                        "    \"$MY_NAME\") continue ;;\n" +
                        "    to_all) continue ;;\n" +
                        "    \"to_$MY_NAME\") continue ;;\n" +
                        "    clip_hist-*) continue ;;\n" +
                        "    to_*) name=${base#to_}; add_device \"$name\" ;;\n" +
                        "    *) add_device \"$base\" ;;\n" +
                        "  esac\n" +
                        "done\n" +
                        "echo \"$DEVICES\"";
                final String result = Root.exec(script);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        populateDeviceChecklist(result);
                    }
                });
            }
        }).start();
    }

    private void populateDeviceChecklist(String csv) {
        llDeviceChecklist.removeAllViews();
        deviceCheckBoxes.clear();

        String trimmed = csv == null ? "" : csv.trim();
        String[] names = trimmed.isEmpty() ? new String[0] : trimmed.split(",");

        Set<String> favSet = new HashSet<String>();
        String favCsv = Prefs.favoriteDevices(this);
        for (String f : favCsv.split(",")) {
            String t = f.trim();
            if (!t.isEmpty()) favSet.add(t);
        }

        if (names.length == 0) {
            tvScanStatus.setText("Файлов-устройств не найдено (проверь путь)");
            return;
        }
        tvScanStatus.setText("Найдено: " + names.length + ". Отметь галочкой, что показывать сразу:");

        for (String name : names) {
            CheckBox cb = new CheckBox(this);
            cb.setText(name);
            cb.setTextColor(Color.WHITE);
            cb.setChecked(favSet.contains(name));
            llDeviceChecklist.addView(cb);
            deviceCheckBoxes.add(cb);
        }
    }

    private void saveFavorites() {
        List<String> result = new ArrayList<String>();
        for (CheckBox cb : deviceCheckBoxes) {
            if (cb.isChecked()) result.add(cb.getText().toString());
        }
        String manual = etFavorites.getText().toString().trim();
        if (!manual.isEmpty()) {
            String[] parts = manual.split(",");
            for (String part : parts) {
                String t = part.trim();
                if (!t.isEmpty() && !result.contains(t)) result.add(t);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < result.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(result.get(i));
        }
        Prefs.setFavoriteDevices(this, sb.toString());
        tvScanStatus.setText("Избранные сохранены (" + result.size() + ")");
    }

    private void scanInputDevices() {
        tvInputScanStatus.setText("Сканирую...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String[] mice = InputDeviceScanner.scanMouseCapable();
                final String[] kbds = InputDeviceScanner.scanKeyboardCapable();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        populateInputChecklist(llMouseChecklist, mouseCheckBoxes, mice,
                                Prefs.csvToArray(Prefs.mouseDevices(MainActivity.this)));
                        populateInputChecklist(llKeyboardChecklist, keyboardCheckBoxes, kbds,
                                Prefs.csvToArray(Prefs.keyboardDevices(MainActivity.this)));
                        tvInputScanStatus.setText("Найдено — мышь: " + mice.length
                                + ", клавиатура: " + kbds.length);
                    }
                });
            }
        }).start();
    }

    private void populateInputChecklist(LinearLayout container, List<CheckBox> boxes,
                                         String[] found, String[] savedEnabled) {
        container.removeAllViews();
        boxes.clear();
        Set<String> enabledSet = new HashSet<String>(Arrays.asList(savedEnabled));
        if (found.length == 0) {
            TextView tv = new TextView(this);
            tv.setText("Ничего не найдено");
            tv.setTextColor(Color.parseColor("#888888"));
            container.addView(tv);
            return;
        }
        for (String name : found) {
            CheckBox cb = new CheckBox(this);
            cb.setText(name);
            cb.setTextColor(Color.WHITE);
            cb.setChecked(savedEnabled.length == 0 || enabledSet.contains(name));
            container.addView(cb);
            boxes.add(cb);
        }
    }

    private void saveInputDevices() {
        List<String> mice = new ArrayList<String>();
        for (CheckBox cb : mouseCheckBoxes) if (cb.isChecked()) mice.add(cb.getText().toString());
        List<String> kbds = new ArrayList<String>();
        for (CheckBox cb : keyboardCheckBoxes) if (cb.isChecked()) kbds.add(cb.getText().toString());
        Prefs.setMouseDevices(this, Prefs.arrayToCsv(mice));
        Prefs.setKeyboardDevices(this, Prefs.arrayToCsv(kbds));
        tvInputScanStatus.setText("Сохранено — мышь: " + mice.size() + ", клавиатура: " + kbds.size());
    }

    private void runScriptTest(final String uid, final TextView tvOut) {
        tvOut.setText("Выполняю от uid " + uid + "...");
        final String stfolder = etStfolderPath.getText().toString().trim();
        new Thread(new Runnable() {
            @Override
            public void run() {
                String scriptPath = stfolder + "/notify_tvbox.sh";
                String cmd = "su " + uid + " sh '" + scriptPath + "' 2>&1";
                final String result = Root.exec(cmd);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvOut.setText("uid " + uid + " -> "
                                + (result.isEmpty() ? "(пусто, без ошибок)" : result));
                    }
                });
            }
        }).start();
    }

    private void saveAndCheckStfolder() {
        final String path = etStfolderPath.getText().toString().trim();
        if (path.isEmpty()) {
            tvStfolderStatus.setText("Путь не может быть пустым");
            tvStfolderStatus.setTextColor(Color.parseColor("#F44336"));
            return;
        }
        tvStfolderStatus.setText("Проверяю...");
        tvStfolderStatus.setTextColor(Color.parseColor("#FFA000"));
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String check = Root.exec("[ -d '" + path + "' ] && echo OK || echo FAIL");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Prefs.setStfolderPath(MainActivity.this, path);
                        if (check.contains("OK")) {
                            tvStfolderStatus.setText("Сохранено, папка найдена ✓");
                            tvStfolderStatus.setTextColor(Color.parseColor("#4CAF50"));
                        } else {
                            tvStfolderStatus.setText("Сохранено, но папка пока не найдена — "
                                    + "проверь, что Syncthing уже создал .stfolder для этого правила ⚠");
                            tvStfolderStatus.setTextColor(Color.parseColor("#FFA000"));
                        }
                    }
                });
            }
        }).start();
    }

    private TextView tvBufferStatus;

    private void saveAndCheckBufferDir() {
        final String path = etBufferDir.getText().toString().trim();
        if (path.isEmpty()) {
            tvBufferStatus.setText("Путь не может быть пустым");
            tvBufferStatus.setTextColor(Color.parseColor("#F44336"));
            return;
        }
        tvBufferStatus.setText("Проверяю...");
        tvBufferStatus.setTextColor(Color.parseColor("#FFA000"));
        new Thread(new Runnable() {
            @Override
            public void run() {
                Root.exec("mkdir -p '" + path + "'");
                final String check = Root.exec("[ -d '" + path + "' ] && echo OK || echo FAIL");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (check.contains("OK")) {
                            Prefs.setBufferDir(MainActivity.this, path);
                            tvBufferStatus.setText("Сохранено, папка существует ✓");
                            tvBufferStatus.setTextColor(Color.parseColor("#4CAF50"));
                            scanDevices();
                        } else {
                            tvBufferStatus.setText("Не удалось создать/найти папку ✗");
                            tvBufferStatus.setTextColor(Color.parseColor("#F44336"));
                        }
                    }
                });
            }
        }).start();
    }

    private void saveFlashSettings() {
        Prefs.setFlashCorner(this, selectedCorner);
        Prefs.setFlashCount(this, readIntOr(etFlashCount, 4));
        Prefs.setFlashPeriodMs(this, readIntOr(etFlashPeriod, 300));
        Prefs.setFlashSizeDp(this, readIntOr(etFlashSize, 16));
        Prefs.setFlashColor(this, selectedColor);
        Prefs.setHistoryFontSizeSp(this, readIntOr(etHistoryFontSize, 14));
    }

    private void updateCornerHighlight() {
        for (int i = 0; i < CORNERS.length; i++) {
            cornerButtons[i].setBackgroundColor(CORNERS[i].equals(selectedCorner)
                    ? Color.parseColor("#4CAF50") : Color.parseColor("#333333"));
            cornerButtons[i].setTextColor(Color.WHITE);
        }
    }

    private void updateColorHighlight() {
        for (int i = 0; i < PRESET_COLORS.length; i++) {
            colorSwatches[i].setAlpha(PRESET_COLORS[i] == selectedColor ? 1.0f : 0.4f);
        }
    }

    private int readIntOr(EditText et, int def) {
        try {
            return Integer.parseInt(et.getText().toString().trim());
        } catch (Exception e) {
            return def;
        }
    }

    private EditText labeledNumberField(LinearLayout parent, String label, int currentValue) {
        parent.addView(sectionLabel(label));
        EditText et = new EditText(this);
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        et.setText(String.valueOf(currentValue));
        et.setTextColor(Color.WHITE);
        addWithBottomMargin(parent, et, 16);
        return et;
    }

    private TextView sectionLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#AAAAAA"));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(4);
        lp.bottomMargin = dp(4);
        tv.setLayoutParams(lp);
        return tv;
    }

    private void addWithBottomMargin(LinearLayout parent, View v, int marginDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(marginDp);
        v.setLayoutParams(lp);
        parent.addView(v);
    }

    private void checkAndGrantOverlayPermission(final TextView tvPerm) {
        if (android.provider.Settings.canDrawOverlays(this)) {
            tvPerm.setText("Оверлей: разрешено ✓");
            tvPerm.setTextColor(Color.parseColor("#4CAF50"));
            return;
        }
        tvPerm.setText("Оверлей: выдаю через root...");
        tvPerm.setTextColor(Color.parseColor("#FFA000"));
        new Thread(new Runnable() {
            @Override
            public void run() {
                Root.exec("appops set " + getPackageName() + " SYSTEM_ALERT_WINDOW allow");
                final boolean ok = android.provider.Settings.canDrawOverlays(MainActivity.this);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (ok) {
                            tvPerm.setText("Оверлей: разрешено ✓ (авто через root)");
                            tvPerm.setTextColor(Color.parseColor("#4CAF50"));
                        } else {
                            tvPerm.setText("Оверлей: НЕ разрешено ✗ (нужна ручная выдача)");
                            tvPerm.setTextColor(Color.parseColor("#F44336"));
                        }
                    }
                });
            }
        }).start();
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics());
    }
}
