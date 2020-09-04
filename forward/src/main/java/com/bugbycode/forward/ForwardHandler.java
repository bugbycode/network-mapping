package com.bugbycode.forward;

import java.util.LinkedList;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bugbycode.module.ConnectionInfo;
import com.bugbycode.module.Message;
import com.bugbycode.module.MessageCode;
import com.util.RandomUtil;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ForwardHandler extends SimpleChannelInboundHandler<ByteBuf>{

	private final Logger logger = LogManager.getLogger(ForwardHandler.class);

	private final String token;
	
	private boolean isClosed;
	
	public Map<String,ForwardHandler> forwardHandlerMap;
	
	private Map<String, Channel> onlineAgentMap;
	
	private String host;
	
	private int port;
	
	private String username;
	
	private LinkedList<Message> queue;
	
	public ForwardHandler(String username,String host,int port,
			Map<String, Channel> onlineAgentMap,Map<String,ForwardHandler> forwardHandlerMap) {
		this.token = RandomUtil.GetGuid32();
		this.queue = new LinkedList<Message>();
		this.onlineAgentMap = onlineAgentMap;
		this.forwardHandlerMap = forwardHandlerMap;
		this.username = username;
		this.host = host;
		this.port = port;
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
		byte[] data = new byte[msg.readableBytes()];
		msg.readBytes(data);
		Message message = new Message(token, MessageCode.TRANSFER_DATA, data);
		writeAndFlush(message);
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		Channel agentChanel = onlineAgentMap.get(username);
		if(agentChanel == null) {
			ctx.close();
			return;
		}
		this.isClosed = false;
		this.forwardHandlerMap.put(token, this);
		
		ConnectionInfo conn = new ConnectionInfo(host, port,0);
		Message message = new Message(token, MessageCode.CONNECTION, conn);
		writeAndFlush(message);
		
		logger.info("Begin connection to " + host + ":" + port + " ......");
		
		Message msg = read();
		
		if(msg.getType() == MessageCode.CONNECTION_ERROR || msg.getType() == MessageCode.CLOSE_CONNECTION) {
			logger.info("Connection to " + host + ":" + port + " error ......");
			ctx.close();
			return;
		} else {
			logger.info("Connection to " + host + ":" + port + " successfully ......");
		}
		new WorkThread(ctx).start();
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		this.isClosed = true;
		this.forwardHandlerMap.remove(token);
		Message message = new Message(token, MessageCode.CLOSE_CONNECTION, null);
		writeAndFlush(message);
		notifyTask();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		ctx.channel().close();
		ctx.close();
		cause.printStackTrace();
	}
	
	public synchronized void send(Message message) {
		this.queue.addLast(message);
		this.notifyAll();
	}
	
	private void writeAndFlush(Message message) {
		Channel agentChanel = onlineAgentMap.get(username);
		if(agentChanel != null) {
			agentChanel.writeAndFlush(message);
		}
	}
	
	private synchronized Message read() throws InterruptedException {
		while(queue.isEmpty()) {
			if(isClosed) {
				throw new InterruptedException("Server is closed.");
			}
			wait();
		}
		return this.queue.removeFirst();
	}
	
	private synchronized void notifyTask() {
		this.notifyAll();
	}
	
	private class WorkThread extends Thread{

		private ChannelHandlerContext ctx;
		
		public WorkThread(ChannelHandlerContext ctx) {
			this.ctx = ctx;
		}
		
		@Override
		public void run() {
			while(!isClosed) {
				try {
					Message msg = read();
					if(msg.getType() == MessageCode.CLOSE_CONNECTION) {
						logger.info("Disconnection to " + host + ":" + port + " ......");
						ctx.close();
					} else if(token.equals(msg.getToken()) && msg.getType() == MessageCode.TRANSFER_DATA) {
						byte[] data = (byte[])msg.getData();
						ByteBuf buff = ctx.alloc().buffer(data.length);
						buff.writeBytes(data);
						ctx.writeAndFlush(buff);
					}
				} catch (InterruptedException e) {
					logger.error(e.getMessage());
				}
			}
		}
		
	}
}
