package com.github.wormhole.serialize;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.util.List;

public class PackageDecoder extends MessageToMessageDecoder<ByteBuf> {
    private Serialization<Frame> serialization = new FrameSerialization();

    public PackageDecoder() {
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out) throws Exception {
        Frame pkg = serialization.deserialize(byteBuf);
        out.add(pkg);
    }
}
