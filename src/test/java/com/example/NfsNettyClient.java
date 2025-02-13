package com.example;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class NfsNettyClient {

    public static void main(String[] args) throws Exception {
        // 请根据实际情况修改 NFS 服务器的地址与端口
        String host = "192.168.153.154"; // NFS 服务器 IP
        int port = 111;             // NFS 服务端口（TCP）

        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                     .channel(NioSocketChannel.class)
                     .handler(new ChannelInitializer<SocketChannel>() {
                         @Override
                         protected void initChannel(SocketChannel ch) {
                             ChannelPipeline pipeline = ch.pipeline();
                             pipeline.addLast("logger", new LoggingHandler(LogLevel.DEBUG));
                             pipeline.addLast(new NfsClientHandler());
                         }
                     });

            // 连接到 NFS 服务器
            ChannelFuture future = bootstrap.connect(host, port).sync();
            ChannelPromise channelPromise = future.channel().newPromise();
            channelPromise.awaitUninterruptibly(10, TimeUnit.SECONDS);
        } finally {
            group.shutdownGracefully();
        }
    }

    /**
     * NFS 客户端 Handler：构造 RPC 请求，并简单处理返回数据
     */
    static class NfsClientHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            // 构造 Portmapper GETPORT 请求，查询 NFS 服务端口（NFS 程序号 100003）
            ByteBuf buf = Unpooled.buffer();

            // 1. RPC 消息头
            //    - XID：随机生成
            int xid = new Random().nextInt();
            buf.writeInt(xid);
            //    - 消息类型：CALL (0)
            buf.writeInt(0);
            //    - RPC 版本：2
            buf.writeInt(2);
            //    - 程序号：Portmapper 程序号为 100000
            buf.writeInt(100000);
            //    - 程序版本：2
            buf.writeInt(2);
            //    - 过程号：GETPORT (Portmapper 中过程号 3 表示 GETPORT)
            buf.writeInt(3);

            // 2. 认证信息（使用 AUTH_NULL：flavor=0，长度=0）
            //    - 认证
            buf.writeInt(0); // flavor
            buf.writeInt(0); // length
            //    - 校验器
            buf.writeInt(0); // flavor
            buf.writeInt(0); // length

            // 3. GETPORT 参数：构造 mapping 结构
            //    a) prog：目标服务程序号（NFS 程序号 100003）
            buf.writeInt(100003);
            //    b) vers：目标服务版本（NFS v3）
            buf.writeInt(3);
            //    c) prot：协议（TCP 为 6；如果需要 UDP 则用 17）
            buf.writeInt(6);
            //    d) port：填 0，表示让 Portmapper 返回实际端口
            buf.writeInt(0);

            // 发送构造好的 Portmapper GETPORT 请求到 111 端口（注意：连接时应连接到 111 端口）
            ctx.writeAndFlush(buf);
            System.out.println("已发送 Portmapper GETPORT 请求，XID=" + xid);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            // 读取服务器返回的数据
            ByteBuf buf = (ByteBuf) msg;
            byte[] resp = new byte[buf.readableBytes()];
            buf.readBytes(resp);

            // 这里只是简单地将返回数据以十六进制字符串形式打印出来
            System.out.println("收到响应数据：");
            System.out.println(bytesToHex(resp));

            // 在这里应对返回数据做 XDR 解码，解析出目录文件列表
            // 完整的实现需要根据 NFS 协议（READDIR3res 的结构）进行解析

            buf.release();
            ctx.close();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }

        // 辅助方法：将字节数组转换为十六进制字符串
        private String bytesToHex(byte[] bytes) {
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x ", b));
            }
            return sb.toString();
        }
    }
}
