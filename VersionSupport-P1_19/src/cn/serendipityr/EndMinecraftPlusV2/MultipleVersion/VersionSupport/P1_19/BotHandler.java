package cn.serendipityr.EndMinecraftPlusV2.MultipleVersion.VersionSupport.P1_19;

import cn.serendipityr.EndMinecraftPlusV2.MultipleVersion.Bot.BotManager;
import cn.serendipityr.EndMinecraftPlusV2.MultipleVersion.Packet.PacketManager;
import cn.serendipityr.EndMinecraftPlusV2.Tools.ConfigUtil;
import cn.serendipityr.EndMinecraftPlusV2.Tools.LogUtil;
import cn.serendipityr.EndMinecraftPlusV2.Tools.ProxyUtil;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.packetlib.ProxyInfo;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.*;
import com.github.steveice10.packetlib.packet.Packet;
import com.github.steveice10.packetlib.tcp.TcpClientSession;

import java.net.Proxy;

public class BotHandler implements cn.serendipityr.EndMinecraftPlusV2.MultipleVersion.Bot.BotHandler {
    @Override
    public Object createClient(Proxy proxy, String userName) {
        ProxyInfo proxyInfo = new ProxyInfo(getProxyType(ConfigUtil.ProxyType), proxy.address());
        return new TcpClientSession(ConfigUtil.AttackAddress, ConfigUtil.AttackPort, new MinecraftProtocol(userName), proxyInfo);
    }

    @Override
    public Object createClientLocal(String userName) {
        return new TcpClientSession(ConfigUtil.AttackAddress, ConfigUtil.AttackPort, new MinecraftProtocol(userName));
    }

    @Override
    public void connect(Object c, Proxy proxy) {
        TcpClientSession client = (TcpClientSession) c;
        String userName = BotManager.clientList.get(c);

        client.addListener(new SessionListener() {
            @Override
            public void packetReceived(Session session, Packet packet) {
                if (!BotManager.aliveList.containsKey(client)) {
                    if (ConfigUtil.SaveWorkingProxy) {
                        ProxyUtil.saveWorkingProxy(proxy);
                    }
                    BotManager.aliveList.put(client, userName);
                    BotManager.joinedCount++;
                }

                new Thread(() -> {
                    PacketManager.handlePacket(BotManager.packetHandler, client, packet, userName);
                }).start();
            }

            @Override
            public void packetSending(PacketSendingEvent packetSendingEvent) {
            }

            @Override
            public void packetSent(Session session, Packet packet) {
            }

            @Override
            public void packetError(PacketErrorEvent packetErrorEvent) {
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

                client.setFlag("disconnectMsg", disconnectMsg);
                BotManager.aliveList.remove(client);
                BotManager.failedCount++;
            }
        });

        client.connect(true);
    }

    @Override
    public void disconnect(Object c) {
        TcpClientSession client = (TcpClientSession) c;
        client.disconnect("");
    }

    @Override
    public void connectLocal(Object c) {
        TcpClientSession client = (TcpClientSession) c;
        String userName = BotManager.clientList.get(c);
        if (ConfigUtil.ForgeSupport) {
            LogUtil.doLog(0, "[DEBUG] [载入Mod列表]", "BotAttack");
            LogUtil.doLog(0, "[DEBUG] [载入Mod列表] 选定的协议库版本无法启用Forge支持。", "BotAttack");
        }

        LogUtil.doLog(0, "[DEBUG] [创建监听器]", "BotAttack");
        client.addListener(new SessionListener() {
            @Override
            public void packetReceived(Session session, Packet packet) {
                if (!BotManager.aliveList.containsKey(client)) {
                    BotManager.aliveList.put(client, userName);
                    BotManager.joinedCount++;
                }

                new Thread(() -> {
                    PacketManager.handlePacket(BotManager.packetHandler, client, packet, userName);
                }).start();
            }

            @Override
            public void packetSending(PacketSendingEvent packetSendingEvent) {
            }

            @Override
            public void packetSent(Session session, Packet packet) {
            }

            @Override
            public void packetError(PacketErrorEvent packetErrorEvent) {
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
                client.setFlag("disconnectMsg", disconnectMsg);

                BotManager.aliveList.remove(client);
            }
        });

        LogUtil.doLog(0, "[DEBUG] [连接建立开始] " + client.getHost() + ":" + client.getPort(), "BotAttack");
        client.connect(true);
    }

    @Override
    public boolean checkClientStatus(Object client) {
        TcpClientSession c = (TcpClientSession) client;
        return c.isConnected();
    }

    @Override
    public String getClientDisconnectMsg(Object client) {
        TcpClientSession c = (TcpClientSession) client;
        return c.getFlag("disconnectMsg");
    }

    @Override
    public boolean hasClientFlag(Object client, String flag) {
        TcpClientSession c = (TcpClientSession) client;
        return c.hasFlag(flag);
    }

    @Override
    public void setClientFlag(Object client, String flag, Object value) {
        TcpClientSession c = (TcpClientSession) client;
        c.setFlag(flag, value);
    }

    @Override
    public Object getClientFlag(Object client, String flag) {
        TcpClientSession c = (TcpClientSession) client;
        return c.getFlag(flag);
    }

    @Override
    public void setClientTimeout(Object client, int timeout) {
        TcpClientSession c = (TcpClientSession) client;
        c.setReadTimeout(timeout);
        c.setWriteTimeout(timeout);
        c.setConnectTimeout(timeout);
    }

    private static ProxyInfo.Type getProxyType(int type) {
        ProxyInfo.Type proxyType;
        switch (type) {
            case 3:
                proxyType = ProxyInfo.Type.SOCKS5;
                break;
            case 2:
                proxyType = ProxyInfo.Type.SOCKS4;
                break;
            case 1:
            default:
                proxyType = ProxyInfo.Type.HTTP;
                break;
        }

        return proxyType;
    }
}