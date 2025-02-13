package com.example;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class NfsConnectivityClient {

    public static void main(String[] args) {
        // 修改为实际 NFS 服务器地址
        final String host = "192.168.153.154";
        // NFS 默认端口为 2049
        final int port = 2049;
        // 设置连接超时时间（毫秒）
        final int connectTimeoutMillis = 10000;

        // 创建用于客户端事件处理的线程组
        EventLoopGroup group = new NioEventLoopGroup();

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                     .channel(NioSocketChannel.class)
                     // 可选：设置连接超时等参数
                     .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMillis)
                     .handler(new ChannelInitializer<SocketChannel>() {
                         @Override
                         protected void initChannel(SocketChannel ch) throws Exception {
                             // 这里可以添加 ChannelHandler，如果仅用于检测连接性，可以不添加其他处理器

                         }
                     });

            System.out.println("Attempting to connect to NFS server at " + host + ":" + port);

            // 尝试连接服务器，并等待连接完成
            ChannelFuture future = bootstrap.connect(host, port).sync();

            if (future.isSuccess()) {
                System.out.println("Connected successfully to NFS server!");
            } else {
                System.out.println("Failed to connect to NFS server.");
            }

            // 等待直到通道关闭
            Channel channel = future.channel();
            channel.closeFuture().sync();
        } catch (Exception e) {
            System.err.println("Error connecting to NFS server:");
            e.printStackTrace();
        } finally {
            // 优雅关闭 EventLoopGroup 以释放所有资源
            group.shutdownGracefully();
        }
    }
}
