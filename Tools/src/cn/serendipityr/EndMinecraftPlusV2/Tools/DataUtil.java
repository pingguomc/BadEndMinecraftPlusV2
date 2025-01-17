package cn.serendipityr.EndMinecraftPlusV2.Tools;

import cc.summermc.bukkitYaml.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class DataUtil {
    public static boolean notModify = false;
    public static List<String> botRegPasswords;
    public static HashMap<String, String> botRegPasswordsMap = new HashMap<>();

    public static void loadData() {
        File dataFile = new File("data.yml");

        if (dataFile.exists()) {
            YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
            botRegPasswords = data.getStringList("Data");
            botRegPasswords.remove("");

            String lastBotName = data.getString("LastBotName");
            Integer lastRandomFlag = data.getInt("LastRandomFlag");
            String lastRandomLength = data.getString("LastRandomLength");

            boolean needReset;

            needReset = !ConfigUtil.BotName.equals(lastBotName) || !ConfigUtil.RandomFlag.equals(lastRandomFlag) || !(ConfigUtil.RandomMinLength + "|" + ConfigUtil.RandomMaxLength).equals(lastRandomLength);

            if (lastBotName != null && needReset) {
                LogUtil.doLog(-1, "检测到BotName已被修改，是否重置数据文件以使更改生效？ [y/n]:", "DataUtil");
                Scanner scanner = new Scanner(System.in);
                if (scanner.nextLine().contains("y")) {
                    if (dataFile.delete()) {
                        LogUtil.doLog(0, "数据文件已成功重置。", "DataUtil");
                    } else {
                        LogUtil.doLog(0, "修改数据文件时出错，操作成功未完成。", "DataUtil");
                    }
                    LogUtil.emptyLog();
                    loadData();
                    return;
                }

                notModify = true;
                LogUtil.emptyLog();
            }
        } else {
            botRegPasswords = new ArrayList<>();
        }

        if (botRegPasswords.size() < ConfigUtil.BotCount) {
            int count = ConfigUtil.BotCount - botRegPasswords.size();

            for (int i = 0; i < count; i++) {
                String newBotName;

                switch (ConfigUtil.RandomFlag) {
                    case 2:
                        newBotName = ConfigUtil.BotName.replace("$rnd", OtherUtils.getRandomString_Ili(ConfigUtil.RandomMinLength, ConfigUtil.RandomMaxLength));
                        break;
                    case 3:
                        newBotName = ConfigUtil.BotName.replace("$rnd", OtherUtils.getRandomString_Abc(ConfigUtil.RandomMinLength, ConfigUtil.RandomMaxLength));
                        break;
                    case 4:
                        newBotName = ConfigUtil.BotName.replace("$rnd", OtherUtils.getRandomString_123(ConfigUtil.RandomMinLength, ConfigUtil.RandomMaxLength));
                        break;
                    case 1:
                    default:
                        newBotName = ConfigUtil.BotName.replace("$rnd", OtherUtils.getRandomString(ConfigUtil.RandomMinLength, ConfigUtil.RandomMaxLength));
                        break;
                }

                String newBotPwd = OtherUtils.getRandomString(8, 10);
                botRegPasswords.add(newBotName + "@" + newBotPwd);
            }
        }

        for (String PwdData : botRegPasswords) {
            try {
                String[] aPwdData = PwdData.split("@");
                botRegPasswordsMap.put(aPwdData[0], aPwdData[1]);
            } catch (Exception ignored) {
            }
        }

        updateData(botRegPasswords);
    }

    public static void updateData(List<String> dataList) {
        File dataFile = new File("data.yml");
        YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);

        data.set("Data", dataList);

        if (!notModify) {
            data.set("LastBotName", ConfigUtil.BotName);
            data.set("LastRandomFlag", ConfigUtil.RandomFlag);
            data.set("LastRandomLength", ConfigUtil.RandomMinLength + "|" + ConfigUtil.RandomMaxLength);
        }

        try {
            data.save(dataFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
