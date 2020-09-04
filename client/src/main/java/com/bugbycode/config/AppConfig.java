package com.bugbycode.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bugbycode.client.startup.NettyClient;

import io.netty.channel.nio.NioEventLoopGroup;

@Configuration
public class AppConfig {

	@Bean
	public Map<String,NettyClient> nettyClientMap(){
		return Collections.synchronizedMap(new HashMap<String,NettyClient>());
	}
	
	@Bean
	public NioEventLoopGroup remoteGroup() {
		return new NioEventLoopGroup();
	}
}
