package com.bugbycode.tunnel;

import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.StringUtils;

import com.bugbycode.client.startup.NettyClient;
import com.bugbycode.module.ConnectionInfo;
import com.bugbycode.module.Message;
import com.bugbycode.module.MessageCode;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class TunnelHandler extends ChannelInboundHandlerAdapter {
	
	private final Logger logger = LogManager.getLogger(TunnelHandler.class);
	
	private StartupRunnable startup;

	private EventLoopGroup remoteGroup;
	
	private Map<String,NettyClient> nettyClientMap;
	
	private String mapping;
	
	public TunnelHandler(StartupRunnable startup,EventLoopGroup remoteGroup,Map<String,NettyClient> nettyClientMap,
			String mapping) {
		this.startup = startup;
		this.remoteGroup = remoteGroup;
		this.nettyClientMap = nettyClientMap;
		this.mapping = mapping;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
		
		logger.info("Connection to server successfully.");
		
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);
		logger.info("Connection closed.");
		this.startup.restart();
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		Message message = (Message) msg;
		String token = message.getToken();
		int type = message.getType();
		if(type == MessageCode.HEARTBEAT) {
			//
		}else if(type == MessageCode.AUTH_SUCCESS) {
			//
			logger.info("Auth successfully.");
			
			Set<String> mappingSet = StringUtils.commaDelimitedListToSet(this.mapping);
			for(String net : mappingSet) {
				String[] netArr = net.split("@");
				Message openTcp = new Message(null, MessageCode.OPEN_TCP, new ConnectionInfo(netArr[0], Integer.valueOf(netArr[1]), Integer.valueOf(netArr[2])));
				ctx.writeAndFlush(openTcp);
			}
		}else if(type == MessageCode.AUTH_ERROR) {
			//
			logger.info("Auth failed.");
			ctx.close();
		}else if(type == MessageCode.OPEN_TCP_SUCCESS){
			logger.info("Open tcp server successfully ......");
		}else if(type == MessageCode.OPEN_TCP_ERROR) {
			ConnectionInfo conn = (ConnectionInfo) message.getData();
			logger.info("Open tcp server " + conn.getPort() + " error ......");
			ctx.close();
		}else if(type == MessageCode.CONNECTION) {
			new NettyClient(message, nettyClientMap, startup, remoteGroup).connection();
		} else if(type == MessageCode.CLOSE_CONNECTION) {
			NettyClient client = nettyClientMap.get(token);
			if(client != null) {
				client.close();
			}
		} else if(type == MessageCode.TRANSFER_DATA) {
			NettyClient client = nettyClientMap.get(token);
			if(client != null) {
				client.writeAndFlush((byte[])message.getData());
			}
		}
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			IdleStateEvent event = (IdleStateEvent) evt;
			if (event.state() == IdleState.READER_IDLE) {
				logger.debug("Heartbeat timeout.");
			} else if (event.state() == IdleState.WRITER_IDLE) {
				Message msg = new Message();
				msg.setType(MessageCode.HEARTBEAT);
				ctx.channel().writeAndFlush(msg);
			}
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		ctx.close();
        logger.error(cause.getMessage());
	}
	
	public void sendMessage(Message message) {
		this.startup.writeAndFlush(message);
	}
	
}
