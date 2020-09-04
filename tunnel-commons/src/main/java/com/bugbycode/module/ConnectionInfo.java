package com.bugbycode.module;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

public class ConnectionInfo implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 9027795841206273339L;

	private int protocol;
	
	private String host;
	
	private int port;
	
	private int agentPort;

	public ConnectionInfo() {
		
	}
	
	public ConnectionInfo(String host, int port,int agentPort) {
		this.host = host;
		this.port = port;
		this.agentPort = agentPort;
	}

	public int getProtocol() {
		return protocol;
	}

	public void setProtocol(int protocol) {
		this.protocol = protocol;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
	
	public int getAgentPort() {
		return agentPort;
	}

	public void setAgentPort(int agentPort) {
		this.agentPort = agentPort;
	}

	@Override
	public String toString() {
		JSONObject json = new JSONObject();
		try {
			json.put("port", port);
			json.put("host", host);
			json.put("agentPort", agentPort);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return json.toString();
	}
}
