package com.bugbycode.forward;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bugbycode.module.ConnectionInfo;
import com.bugbycode.module.Message;
import com.bugbycode.module.MessageCode;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class ForwardServer extends Thread{
	
	private final Logger logger = LogManager.getLogger(ForwardServer.class);

	private EventLoopGroup boss;

	private EventLoopGroup worker;
	
	private int agentPort;
	
	private String username;
	
	private String host;
	
	private int port;
	
	public Map<String,ForwardHandler> forwardHandlerMap;
	
	private Map<String, Channel> onlineAgentMap;
	
	public ForwardServer(int agentPort,String username,String host,int port,
			Map<String, Channel> onlineAgentMap,Map<String,ForwardHandler> forwardHandlerMap) {
		this.agentPort = agentPort;
		this.username = username;
		this.host = host;
		this.port = port;
		this.onlineAgentMap = onlineAgentMap;
		this.forwardHandlerMap = forwardHandlerMap;
	}
	
	@Override
	public void run() {
		ServerBootstrap bootstrap = new ServerBootstrap();
		boss = new NioEventLoopGroup();
		worker = new NioEventLoopGroup();
		bootstrap.group(boss, worker).channel(NioServerSocketChannel.class)
		.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
		.option(ChannelOption.SO_BACKLOG, 5000)
		.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
		.childOption(ChannelOption.SO_KEEPALIVE, true)
		.childHandler(new ChannelInitializer<SocketChannel>() {

			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				ch.pipeline().addLast(new ForwardHandler(username,host,port,onlineAgentMap,forwardHandlerMap));
			}
		});
		
		bootstrap.bind(agentPort).addListener(new ChannelFutureListener() {

			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				Message message = new Message(null, MessageCode.OPEN_TCP_SUCCESS, new ConnectionInfo(host,port,agentPort));
				Channel agentChannel = onlineAgentMap.get(username);
				if (future.isSuccess()) {
					logger.info("Forward server startup successfully, port " + agentPort + "......");
				} else {
					message.setType(MessageCode.OPEN_TCP_ERROR);
					
					//future.cause().printStackTrace();
					logger.info("Forward server startup failed, port " + agentPort + "......");
					shutdown();
				}
				if(agentChannel != null) {
					agentChannel.writeAndFlush(message);
				}
			}

		});
	}
	
	public void shutdown() {
		if(boss != null) {
			boss.shutdownGracefully();
		}
		
		if(worker != null) {
			worker.shutdownGracefully();
		}
		
		logger.info("Forward server shutdown, port " + agentPort + "......");
	}

}
