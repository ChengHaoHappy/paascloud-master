/*
 * Copyright (c) 2018. paascloud.net All Rights Reserved.
 * 项目名称：paascloud快速搭建企业级分布式微服务平台
 * 类名称：UacFallbackProvider.java
 * 创建人：刘兆明
 * 联系方式：paascloud.net@gmail.com
 * 开源地址: https://github.com/paascloud
 * 博客地址: http://blog.paascloud.net
 * 项目官网: http://paascloud.net
 */

package com.paascloud.gateway.fallback;

import com.netflix.hystrix.exception.HystrixTimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.netflix.zuul.filters.route.FallbackProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * 自定义Zuul回退机制处理器。
 * Provides fallback when a failure occurs on a route 英文意思就是说提供一个回退机制当路由后面的服务发生故障时。
 * The class Uac fallback provider.
 *
 * @author paascloud.net@gmail.com
 */
@Component
@Slf4j
public class UacFallbackProvider implements FallbackProvider {


	/**
	 * 返回值表示需要针对此微服务做回退处理（该名称一定要是注册进入 eureka 微服务中的那个 serviceId 名称）；
	 * api服务id，如果需要所有调用都支持回退，则return "*" 或 return null
	 * @return * 所有调用都支持回退
	 */
	@Override
	public String getRoute() {
		return "*";
	}

	@Override
	public ClientHttpResponse fallbackResponse(final Throwable cause) {
		if (cause instanceof HystrixTimeoutException) {
			return response(HttpStatus.GATEWAY_TIMEOUT);
		} else {
			return fallbackResponse();
		}
	}

	@Override
	public ClientHttpResponse fallbackResponse() {
		return response(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	/**
	 * 网关向api服务请求是失败了，但是消费者客户端向网关发起的请求是OK的，
	 * 不应该把api的404,500等问题抛给客户端
	 * 网关和api服务集群对于客户端来说是黑盒子
	 * @param status
	 * @return
	 */
	private ClientHttpResponse response(final HttpStatus status) {
		return new ClientHttpResponse() {
			@Override
			public HttpStatus getStatusCode() {
				return status;
			}

			@Override
			public int getRawStatusCode() {
				return status.value();
			}

			@Override
			public String getStatusText() {
				return status.getReasonPhrase();
			}

			@Override
			public void close() {
				log.info("close");
			}

			/**
			 * 当 paascloud-provider-uac  微服务出现宕机后，客户端再请求时候就会返回 fallback 等字样的字符串提示；
			 * 但对于复杂一点的微服务，我们这里就得好好琢磨该怎么友好提示给用户了
			 * 如果请求用户服务失败，返回什么信息给消费者客户端
			 * @return
			 */
			@Override
			public InputStream getBody() {
				String message = "{\n" +
						"\"code\": 200,\n" +
						"\"message\": \"微服务故障, 请稍后再试\"\n" +
						"}";
				return new ByteArrayInputStream(message.getBytes());
			}

			@Override
			public HttpHeaders getHeaders() {
				HttpHeaders headers = new HttpHeaders();
				//和body中的内容编码一致，否则容易乱码
				headers.setContentType(MediaType.APPLICATION_JSON);
				return headers;
			}
		};
	}
}
