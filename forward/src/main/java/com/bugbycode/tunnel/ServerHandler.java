package com.bugbycode.tunnel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.CollectionUtils;

import com.bugbycode.forward.ForwardHandler;
import com.bugbycode.forward.ForwardServer;
import com.bugbycode.module.Authentication;
import com.bugbycode.module.ConnectionInfo;
import com.bugbycode.module.Message;
import com.bugbycode.module.MessageCode;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class ServerHandler extends ChannelInboundHandlerAdapter {
	
	private final Logger logger = LogManager.getLogger(ServerHandler.class);
	
	private int loss_connect_time = 0;
	
	private ChannelGroup channelGroup;
	
	private Map<String, Channel> onlineAgentMap;
	
	private Map<String,ForwardHandler> forwardHandlerMap;
	
	private Map<String,List<ForwardServer>> forwardServerMap;
	
	private Map<String,String> authMap;
	
	private String username;
	
	private String password;
	
	public ServerHandler(ChannelGroup channelGroup,  
			Map<String,ForwardHandler> forwardHandlerMap,
			Map<String,List<ForwardServer>> forwardServerMap,
			Map<String, Channel> onlineAgentMap,Map<String,String> authMap) {
		this.channelGroup = channelGroup;
		this.onlineAgentMap = onlineAgentMap;
		this.authMap = authMap;
		this.forwardHandlerMap = forwardHandlerMap;
		this.forwardServerMap = forwardServerMap;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		logger.info("Client connection ......");
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);
		ctx.close();
		ctx.channel().close();
		onlineAgentMap.remove(username);
		List<ForwardServer> forwardServerList = forwardServerMap.get(username);
		if(!CollectionUtils.isEmpty(forwardServerList)) {
			for(ForwardServer server : forwardServerList) {
				server.shutdown();
			}
			forwardServerList.clear();
		}
		forwardServerMap.remove(username);
		logger.info("Client " + this.username + " disconnection ......");
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		loss_connect_time = 0;
		Channel channel = ctx.channel();
		Message message = (Message)msg;
		int type = message.getType();
		Object data = message.getData();
		String token = message.getToken();
		if(type == MessageCode.AUTH) {
			if(data == null || !(data instanceof Authentication)) {
				ctx.close();
				return;
			}
			Authentication authInfo = (Authentication)data;
			
			String username = authInfo.getUsername();
			String password = authInfo.getPassword();
			
			this.username = username;
			this.password = password;
			
			if(!(onlineAgentMap.get(username) == null)) {
				message.setType(MessageCode.AUTH_ERROR);
				message.setData(null);
				channel.writeAndFlush(message);
				ctx.close();
				return;
			}
			
			//认证失败
			if(authMap.get(username) == null || !authMap.get(this.username).equals(this.password)) {
				message.setType(MessageCode.AUTH_ERROR);
				message.setData(null);
				channel.writeAndFlush(message);
				ctx.close();
				logger.info("Client " + username + " auth error.");
				return;
			}
			logger.info("Client " + username + " auth successfully.");
			
			forwardServerMap.put(username, new ArrayList<ForwardServer>());
			
			message.setType(MessageCode.AUTH_SUCCESS);
			message.setData(null);
			channel.writeAndFlush(message);
			
			onlineAgentMap.put(username, channel);
			channelGroup.add(channel);
			return;
		}
		
		channel = channelGroup.find(channel.id());
		if(channel == null) {
			ctx.close();
			return;
		}
		
		if(type == MessageCode.HEARTBEAT) {
			//
			//System.out.println(message);
			return;
		}
		
		// open tcp server
		if(type == MessageCode.OPEN_TCP) {
			if(!(data instanceof ConnectionInfo)) {
				ctx.close();
				return;
			}
			
			ConnectionInfo conn = (ConnectionInfo) data;
			ForwardServer forwardServer = new ForwardServer(conn.getAgentPort(), username, conn.getHost(), conn.getPort(), onlineAgentMap, forwardHandlerMap);
			
			List<ForwardServer> forwardServerList = forwardServerMap.get(username);
			
			forwardServerList.add(forwardServer);
			
			forwardServer.start();
			
			return;
		}
		
		if(type == MessageCode.CONNECTION_SUCCESS || type == MessageCode.CONNECTION_ERROR 
				|| type == MessageCode.TRANSFER_DATA || type == MessageCode.CLOSE_CONNECTION) {
			ForwardHandler handle = forwardHandlerMap.get(token);
			if(handle != null) {
				handle.send(message);
			}
			return;
		}
	}
	
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		super.channelReadComplete(ctx);
	}
	
	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			IdleStateEvent event = (IdleStateEvent) evt;
			if (event.state() == IdleState.READER_IDLE) {
				loss_connect_time++;
				//logger.info("Read heartbeat timeout.");
				if (loss_connect_time > 2) {
					logger.info("Channel timeout.");
					ctx.channel().close();
				}
			}
		}
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		super.exceptionCaught(ctx, cause);
		ctx.close();
	}
}
