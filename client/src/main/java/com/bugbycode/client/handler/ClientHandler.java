package com.bugbycode.client.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bugbycode.client.startup.NettyClient;
import com.bugbycode.module.Message;
import com.bugbycode.module.MessageCode;
import com.bugbycode.tunnel.StartupRunnable;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

	private final Logger logger = LogManager.getLogger(ClientHandler.class);
	
	private StartupRunnable startup;
	
	private String token;
	
	private NettyClient client;
	
	public ClientHandler(StartupRunnable startup,String token,NettyClient client) {
		this.startup = startup;
		this.token = token;
		this.client = client;
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
		byte[] data = new byte[msg.readableBytes()];
		msg.readBytes(data);
		Message message = new Message(token, MessageCode.TRANSFER_DATA, data);
		if(startup == null) {
			throw new RuntimeException("User client exit.");
		}
		startup.writeAndFlush(message);
	}

	@Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception{
		super.channelInactive(ctx);
		client.close();
	}
	
	@Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }
	
	@Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception{
		logger.error(cause.getMessage());
		ctx.channel().close();
    }
}
