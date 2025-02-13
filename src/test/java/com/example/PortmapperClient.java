package com.example;

import java.net.*;
import java.io.*;

public class PortmapperClient {

    private static final String PORTMAPPER_HOST = "192.168.153.154"; // Portmapper的地址
    private static final int PORTMAPPER_PORT = 111; // Portmapper默认使用TCP 111端口

    public static void main(String[] args) {
        try {
            // 创建TCP连接到Portmapper服务器
            Socket socket = new Socket(PORTMAPPER_HOST, PORTMAPPER_PORT);
            socket.setSoTimeout(50000); // 设置超时5秒

            // 获取输出流，发送请求数据
            OutputStream outputStream = socket.getOutputStream();
            byte[] requestData = getPortmapperRequestData();  // 获取16进制请求数据
            outputStream.write(requestData);
            System.out.println("Request sent to Portmapper...");

            // 获取输入流，接收响应数据
            InputStream inputStream = socket.getInputStream();
            byte[] responseData = new byte[512];
            System.out.println("Response received from Portmapper...");

            // 解析Portmapper响应数据
            parsePortmapperResponse(responseData);

            // 关闭连接
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static byte[] getPortmapperRequestData() {
        byte[] bytes1 = {
                (byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x38, (byte) 0x29, (byte) 0x36, (byte) 0x84, (byte) 0x28,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x02,
                (byte) 0x00, (byte) 0x01, (byte) 0x86, (byte) 0xa0, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x02,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x86, (byte) 0xa3,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x06,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
        };

        byte[] bytes2 = {
        };

        /**
         80 00 00 38 29 36 84 28
         00 00 00 00 00 00 00 02
         00 01 86 a0 00 00 00 02
         00 00 00 03 00 00 00 00
         00 00 00 00 00 00 00 00
         00 00 00 00 00 01 86 a3
         00 00 00 03 00 00 00 06
         00 00 00 00
         */
        byte[] combinedBytes = new byte[bytes1.length + bytes2.length];

// Copy bytes1 into the new array
        System.arraycopy(bytes1, 0, combinedBytes, 0, bytes1.length);

// Copy bytes2 into the new array after bytes1
        System.arraycopy(bytes2, 0, combinedBytes, bytes1.length, bytes2.length);
        System.out.println(combinedBytes.length);
        return combinedBytes;
    }

    private static void parsePortmapperResponse(byte[] responseData) {
        // 解析Portmapper响应，提取NFS服务的端口等信息
        System.out.println("Parsing Portmapper response...");
        // 假设响应中端口在返回的第25到28字节
        int port = ((responseData[24] & 0xFF) << 8) | (responseData[25] & 0xFF);
        System.out.println("Portmapper response: NFS port = " + port);
    }
}
