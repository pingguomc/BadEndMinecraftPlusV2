package cn.serendipityr.EndMinecraftPlusV2.MultipleVersion.VersionSupport.P1_9_X_1_12_1;

import cn.serendipityr.EndMinecraftPlusV2.AdvanceModule.MCForge.MCForge;
import cn.serendipityr.EndMinecraftPlusV2.MultipleVersion.Bot.BotManager;
import cn.serendipityr.EndMinecraftPlusV2.MultipleVersion.Packet.PacketManager;
import cn.serendipityr.EndMinecraftPlusV2.Tools.ConfigUtil;
import cn.serendipityr.EndMinecraftPlusV2.Tools.LogUtil;
import cn.serendipityr.EndMinecraftPlusV2.Tools.ProxyUtil;
import org.spacehq.mc.protocol.MinecraftProtocol;
import org.spacehq.packetlib.Client;
import org.spacehq.packetlib.event.session.*;
import org.spacehq.packetlib.tcp.TcpSessionFactory;

import java.net.Proxy;

public class BotHandler implements cn.serendipityr.EndMinecraftPlusV2.MultipleVersion.Bot.BotHandler {
    @Override
    public Object createClient(Proxy proxy, String userName) {
        return new Client(ConfigUtil.AttackAddress, ConfigUtil.AttackPort, new MinecraftProtocol(userName), new TcpSessionFactory(proxy));
    }

    @Override
    public Object createClientLocal(String userName) {
        return new Client(ConfigUtil.AttackAddress, ConfigUtil.AttackPort, new MinecraftProtocol(userName), new TcpSessionFactory());
    }

    @Override
    public void connect(Object c, Proxy proxy) {
        Client client = (Client) c;
        String userName = BotManager.clientList.get(c);
        if (ConfigUtil.ForgeSupport) {
            new MCForge(client.getSession(), ConfigUtil.ForgeModList).init();
        }

        client.getSession().addListener(new SessionListener() {
            public void packetReceived(PacketReceivedEvent e) {
                if (!BotManager.aliveList.containsKey(client)) {
                    if (ConfigUtil.SaveWorkingProxy) {
                        ProxyUtil.saveWorkingProxy(proxy);
                    }
                    BotManager.aliveList.put(client, userName);
                    BotManager.joinedCount++;
                }

                new Thread(() -> {
                    PacketManager.handlePacket(BotManager.packetHandler, client, e.getPacket(), userName);
                }).start();
            }

            public void packetSent(PacketSentEvent e) {
            }

            public void connected(ConnectedEvent e) {
            }

            public void disconnecting(DisconnectingEvent e) {
            }

            public void disconnected(DisconnectedEvent e) {
                String disconnectMsg;

                if (e.getCause() == null) {
                    disconnectMsg = e.getReason();
                    LogUtil.doLog(0, "[假人断开连接] [" + userName + "] " + disconnectMsg, "BotAttack");
                } else {
                    disconnectMsg = e.getCause().getClass().getSimpleName();
                    if (ConfigUtil.ShowFails) {
                        LogUtil.doLog(0, "[假人断开连接] [" + userName + "] " + disconnectMsg, "BotAttack");
                    }
                }

                client.getSession().setFlag("disconnectMsg", disconnectMsg);
                BotManager.aliveList.remove(client);
                BotManager.failedCount++;
            }
        });

        client.getSession().connect(true);
    }

    @Override
    public void disconnect(Object c) {
        Client client = (Client) c;
        client.getSession().disconnect("");
    }

    @Override
    public void connectLocal(Object c) {
        Client client = (Client) c;
        String userName = BotManager.clientList.get(c);
        if (ConfigUtil.ForgeSupport) {
            LogUtil.doLog(0, "[DEBUG] [载入Mod列表]", "BotAttack");
            new MCForge(client.getSession(), ConfigUtil.ForgeModList).init();
        }

        LogUtil.doLog(0, "[DEBUG] [创建监听器]", "BotAttack");
        client.getSession().addListener(new SessionListener() {
            public void packetReceived(PacketReceivedEvent e) {
                if (!BotManager.aliveList.containsKey(client)) {
                    BotManager.aliveList.put(client, userName);
                    BotManager.joinedCount++;
                }

                new Thread(() -> {
                    PacketManager.handlePacket(BotManager.packetHandler, client, e.getPacket(), userName);
                }).start();
            }

            public void packetSent(PacketSentEvent e) {
            }

            public void connected(ConnectedEvent e) {
                LogUtil.doLog(0, "[DEBUG] [Bot连接成功]", "BotAttack");
            }

            public void disconnecting(DisconnectingEvent e) {
            }

            public void disconnected(DisconnectedEvent e) {
                String disconnectMsg;

                if (e.getCause() == null) {
                    disconnectMsg = e.getReason();
                    LogUtil.doLog(0, "[假人断开连接] [" + userName + "] " + disconnectMsg, "BotAttack");
                } else {
                    e.getCause().printStackTrace();
                    disconnectMsg = e.getCause().getClass().getSimpleName();
                    if (ConfigUtil.ShowFails) {
                        LogUtil.doLog(0, "[假人断开连接] [" + userName + "] " + disconnectMsg, "BotAttack");
                    }
                }

                BotManager.failedCount++;
                client.getSession().setFlag("disconnectMsg", disconnectMsg);

                BotManager.aliveList.remove(client);
            }
        });

        LogUtil.doLog(0, "[DEBUG] [连接建立开始] " + client.getHost() + ":" + client.getPort(), "BotAttack");
        client.getSession().connect(true);
    }

    @Override
    public boolean checkClientStatus(Object client) {
        Client c = (Client) client;
        return c.getSession().isConnected();
    }

    @Override
    public String getClientDisconnectMsg(Object client) {
        Client c = (Client) client;
        return c.getSession().getFlag("disconnectMsg");
    }

    @Override
    public boolean hasClientFlag(Object client, String flag) {
        Client c = (Client) client;
        return c.getSession().hasFlag(flag);
    }

    @Override
    public void setClientFlag(Object client, String flag, Object value) {
        Client c = (Client) client;
        c.getSession().setFlag(flag, value);
    }

    @Override
    public Object getClientFlag(Object client, String flag) {
        Client c = (Client) client;
        return c.getSession().getFlag(flag);
    }

    @Override
    public void setClientTimeout(Object client, int timeout) {
        Client c = (Client) client;
        c.getSession().setReadTimeout(timeout);
        c.getSession().setWriteTimeout(timeout);
        c.getSession().setConnectTimeout(timeout);
    }
}
