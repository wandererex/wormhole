package com.github.wormhole.server;

import com.github.wormhole.serialize.FrameEncoder;
import com.github.wormhole.serialize.PackageDecoder;
import com.github.wormhole.serialize.PackageEncoder;
import com.github.wormhole.server.processor.BuildDataChannelProcessor;
import com.github.wormhole.server.processor.DisconnectClientPocessor;
import com.github.wormhole.server.processor.ProxyRegisterProcessor;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;

import com.github.wormhole.serialize.FrameDecoder;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import com.github.wormhole.client.SignalHandler;
import com.github.wormhole.common.config.ProxyServiceConfig;

public class Server {
    private int port;

    private int dataTransPort;

    private ChannelFuture channelFuture;

    private EventLoopGroup boss = new NioEventLoopGroup();
    private EventLoopGroup worker = new NioEventLoopGroup();

    private SignalHandler signalHandler;

    private Map<String, ProxyServer> proxyServerMap = new ConcurrentHashMap<>();

    private DataTransServer dataTransServer;

    private Map<String, String> proxyDataChannelMap = new ConcurrentHashMap<>();
    
    private Map<String, Channel> proxyIdChannelMap = new ConcurrentHashMap<>();

    public Server(int port, int dataTransPort) {
        this.port = port;
        this.dataTransPort = dataTransPort;
    }

    public void open() {
        buildSignalHandler();
        dataTransServer = new DataTransServer(dataTransPort, boss, worker, this);
        dataTransServer.open();

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(boss, worker)
        .option(ChannelOption.AUTO_READ, true)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.DEBUG))
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new FrameDecoder());
                        pipeline.addLast(new FrameEncoder());
                        pipeline.addLast(new PackageDecoder());
                        pipeline.addLast(new PackageEncoder());
                        pipeline.addLast(signalHandler);
                        pipeline.addLast(new LoggingHandler(LogLevel.DEBUG));
                    }
                });

        try {
            channelFuture = bootstrap.bind(port).sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        channelFuture.addListener((future) -> {
            if (!future.isSuccess()) {
                future.cause().printStackTrace();
            }

        });
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            boss.shutdownGracefully().syncUninterruptibly();
            worker.shutdownGracefully().syncUninterruptibly();
        }));
    }

    public String buildProxyServer(ProxyServiceConfig config, Channel channel) throws Exception {
        String proxyId = UUID.randomUUID().toString();
        ProxyServer proxyServer = new ProxyServer(boss, worker, proxyId, config, channel, this);
        proxyServer.open();
        proxyServerMap.put(proxyId, proxyServer);
        return proxyId;
    }

    private void buildSignalHandler() {
        signalHandler = new SignalHandler();
        signalHandler.register(new ProxyRegisterProcessor(this))
            .register(new BuildDataChannelProcessor(this))
            .register(new DisconnectClientPocessor(this));
    }

    public void close() {
        if (channelFuture != null) {
            channelFuture.channel().close();
        }
    }

    public static void main(String[] args) {
        String port = null;
        Integer dataTransPort = null;
        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                if (StringUtils.isNotEmpty(args[i]) && args[i].equals("--port")) {
                    if (i + 1 < args.length) {
                        String arg = args[i + 1];
                        if (StringUtils.isNotEmpty(arg)) {
                            port = arg;
                        }
                    }
                } else if (StringUtils.isNotEmpty(args[i]) && args[i].equals("--dataTransPort")) {
                    if (i + 1 < args.length) {
                        String arg = args[i + 1];
                        if (StringUtils.isNotEmpty(arg)) {
                            dataTransPort = Integer.parseInt(arg);
                        }
                    }
                }
            }
            if (port != null && dataTransPort != null) {
                new Server(Integer.parseInt(port), dataTransPort).open();
            }
        }
    }

    public int getPort() {
        return port;
    }

    public int getDataTransPort() {
        return dataTransPort;
    }

    public ChannelFuture getChannelFuture() {
        return channelFuture;
    }

    public EventLoopGroup getBoss() {
        return boss;
    }

    public EventLoopGroup getWorker() {
        return worker;
    }

    public SignalHandler getSignalHandler() {
        return signalHandler;
    }

    public ProxyServer getProxyServer(String proxyId) {
        return proxyServerMap.get(proxyId);
    }

    public DataTransServer getDataTransServer() {
        return dataTransServer;
    }

    public Map<String, ProxyServer> getProxyServerMap() {
        return proxyServerMap;
    }

    public Map<String, String> getDataChannelProxyIdMap() {
        return proxyDataChannelMap;
    }

    public Map<String, Channel> getProxyIdChannelMap() {
        return proxyIdChannelMap;
    }

    
}
