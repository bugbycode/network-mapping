package com.bugbycode.tunnel;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.bugbycode.client.startup.NettyClient;

import io.netty.channel.EventLoopGroup;

@Component
public class TunnelClient implements ApplicationRunner{

	@Value("${spring.netty.tunnel.host}")
	private String host;

	@Value("${spring.netty.tunnel.port}")
	private int port;

	@Value("${spring.netty.tunnel.username}")
	private String username;

	@Value("${spring.netty.tunnel.password}")
	private String password;
	
	@Value("${spring.netty.tunnel.mapping}")
	private String mapping;
	
	@Autowired
	private Map<String,NettyClient> nettyClientMap;

	@Autowired
	private EventLoopGroup remoteGroup;
	
	@Override
	public void run(ApplicationArguments args) throws Exception {
		new StartupRunnable(host, port, username, password,mapping,remoteGroup,nettyClientMap).run();
	}
	
}
