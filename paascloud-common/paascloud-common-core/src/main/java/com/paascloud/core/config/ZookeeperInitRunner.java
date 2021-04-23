/*
 * Copyright (c) 2018. paascloud.net All Rights Reserved.
 * 项目名称：paascloud快速搭建企业级分布式微服务平台
 * 类名称：ZookeeperInitRunner.java
 * 创建人：刘兆明
 * 联系方式：paascloud.net@gmail.com
 * 开源地址: https://github.com/paascloud
 * 博客地址: http://blog.paascloud.net
 * 项目官网: http://paascloud.net
 */

package com.paascloud.core.config;

import com.paascloud.config.properties.PaascloudProperties;
import com.paascloud.core.registry.RegistryCenterFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.net.InetAddress;

/**
 *
 * 平常开发中有可能需要实现在项目启动后执行的功能，
 * SpringBoot提供的一种简单的实现方案就是添加一个model并实现CommandLineRunner接口，
 * 实现功能的代码放在实现的run方法中
 * The class Redis init runner.
 *
 * @author paascloud.net @gmail.com
 */
@Component
@Order
@Slf4j
public class ZookeeperInitRunner implements CommandLineRunner {
	@Resource
	private PaascloudProperties paascloudProperties;
	@Value("${spring.application.name}")
	private String applicationName;

	/**
	 * Run.
	 *
	 * @param args the args
	 *
	 * @throws Exception the exception
	 */
	@Override
	public void run(String... args) throws Exception {
		String hostAddress = InetAddress.getLocalHost().getHostAddress();
		log.info("###ZookeeperInitRunner，init. HostAddress={}, applicationName={}", hostAddress, applicationName);
		RegistryCenterFactory.startup(paascloudProperties, hostAddress, applicationName);
		log.info("###ZookeeperInitRunner，finish<<<<<<<<<<<<<");
	}

}