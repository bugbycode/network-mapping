package com.bugbycode.tunnel;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

@Component
public class TunnelServer implements ApplicationRunner{
	
	private final Logger logger = LogManager.getLogger(TunnelServer.class);

	@Value("${spring.netty.tunnel.port}")
	private int serverPort; 
	
	@Value("${spring.netty.tunnel.so_backlog}")
	private int so_backlog;
	
	private EventLoopGroup boss;
	
	private EventLoopGroup worker;
	
	@Autowired
	private ChannelHandler serverChannelInitializer;
	
	@Override
	public void run(ApplicationArguments args) throws Exception {
		
		boss = new NioEventLoopGroup();
		worker = new NioEventLoopGroup();
		ServerBootstrap bootstrap = new ServerBootstrap();
		
		bootstrap.group(boss, worker).channel(NioServerSocketChannel.class)
				.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
				.option(ChannelOption.SO_BACKLOG, so_backlog)
				.childOption(ChannelOption.SO_KEEPALIVE, true)
				.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
				.childHandler(serverChannelInitializer);
		
		bootstrap.bind(serverPort).addListener(new ChannelFutureListener() {

			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (future.isSuccess()) {
					logger.info("Tunnel server startup successfully, port " + serverPort + "......");
				} else {
					logger.info("Tunnel server startup failed, port " + serverPort + "......");
					System.exit(0);;
				}
			}
		});
		
	}
}
