package com.github.wandererex.wormhole.serialize;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Frame {
    //0x1:内网proxy连接公网服务器请求，0x2x:0x1响应(0失败，1成功)，0x3:数据传输，0x4:数据传输响应(0失败，1成功), 0x5:心跳, 0x6:心跳响应, 0x7:内网proxy连接公网服务器下线，0x8:0x7响应(0失败，1成功)
    //0x9:内网proxy与内网服务建立连接, 0xA:0x9响应(0失败，1成功)
    //0xA:内网proxy与内网服务断开连接, 0xA:0xA响应(0失败，1成功)
    //0xB:公网服务器断开连接, 0xA:0xB响应(0失败，1成功)
    private int opCode;

    private String serviceKey;

    private byte[] payload;
}
