package com.bugbycode.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import com.bugbycode.forward.ForwardHandler;
import com.bugbycode.forward.ForwardServer;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

@Configuration
public class AppConfig {
	
	@Value("${spring.netty.tunnel.authinfo}")
	private String authinfo;
	
	@Bean("channelGroup")
	public ChannelGroup getChannelGroup() {
		return new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
	}
	
	@Bean
	public Map<String,ForwardHandler> forwardHandlerMap(){
		return Collections.synchronizedMap(new HashMap<String,ForwardHandler>());
	}
	
	@Bean
	public Map<String,List<ForwardServer>> forwardServerMap(){
		return Collections.synchronizedMap(new HashMap<String,List<ForwardServer>>());
	}
	
	@Bean
	public Map<String, Channel> onlineAgentMap(){
		return Collections.synchronizedMap(new HashMap<String,Channel>());
	}
	
	@Bean
	public Map<String,String> authMap(){
		Map<String,String> authMap = Collections.synchronizedMap(new HashMap<String,String>());
		Set<String> authSet = StringUtils.commaDelimitedListToSet(authinfo); 
		for(String auth : authSet) {
			int index = auth.indexOf('@');
			if(index == -1) {
				continue;
			}
			String username = auth.substring(0, index++);
			String password = auth.substring(index);
			authMap.put(username, password);
		}
		return authMap;
	}
}
