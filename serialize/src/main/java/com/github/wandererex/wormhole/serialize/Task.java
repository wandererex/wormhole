package com.github.wandererex.wormhole.serialize;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

public class Task implements Runnable{
    private Channel channel;

    private Object msg;

    public Task(Channel channel, Object msg) {
        this.channel = channel;
        this.msg = msg;
    }

    @Override
    public void run() {
        channel.writeAndFlush(msg);
    }
}
