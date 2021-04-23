/*
 * Copyright (c) 2018. paascloud.net All Rights Reserved.
 * 项目名称：paascloud快速搭建企业级分布式微服务平台
 * 类名称：AuthHeaderFilter.java
 * 创建人：刘兆明
 * 联系方式：paascloud.net@gmail.com
 * 开源地址: https://github.com/paascloud
 * 博客地址: http://blog.paascloud.net
 * 项目官网: http://paascloud.net
 */

package com.paascloud.gateway.filter;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.paascloud.PublicUtil;
import com.paascloud.base.enums.ErrorCodeEnum;
import com.paascloud.base.exception.BusinessException;
import com.paascloud.core.interceptor.CoreHeaderInterceptor;
import com.paascloud.core.utils.RequestUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * The class Auth header filter.
 *
 * @author paascloud.net @gmail.com
 */
@Slf4j
@Component
public class AuthHeaderFilter extends ZuulFilter {

	private static final String BEARER_TOKEN_TYPE = "bearer ";
	private static final String OPTIONS = "OPTIONS";
	private static final String AUTH_PATH = "/auth";
	private static final String LOGOUT_URI = "/oauth/token";
	private static final String ALIPAY_CALL_URI = "/pay/alipayCallback";


	/**
	 * 四种类型：pre,routing,error,post
	 * pre：主要用在路由映射的阶段是寻找路由映射表的,可以在请求被路由之前调用
	 * routing:具体的路由转发过滤器是在routing路由器，具体的请求转发的时候会调用
	 * error:一旦前面的过滤器出错了，会调用error过滤器。
	 * post:当routing，error运行完后才会调用该过滤器，是在最后阶段的
	 *
	 * @return the string
	 */
	@Override
	public String filterType() {
		return "pre";
	}

	/**
	 * Filter order int.
	 * 自定义过滤器执行的顺序，数值越大越靠后执行，越小就越先执行
	 * @return the int
	 */
	@Override
	public int filterOrder() {
		return 0;
	}

	/**
	 * 控制过滤器生效不生效，可以在里面写一串逻辑来控制
	 * Should filter boolean.
	 *
	 * @return the boolean
	 */
	@Override
	public boolean shouldFilter() {
		return true;
	}

	/**
	 * Run object.
	 * 执行过滤逻辑
	 * @return the object
	 */
	@Override
	public Object run() {
		log.info("AuthHeaderFilter - 开始鉴权...");
		RequestContext requestContext = RequestContext.getCurrentContext();
		try {
			doSomething(requestContext);
		} catch (Exception e) {
			log.error("AuthHeaderFilter - [FAIL] EXCEPTION={}", e.getMessage(), e);
			throw new BusinessException(ErrorCodeEnum.UAC10011041);
		}
		return null;
	}

	private void doSomething(RequestContext requestContext) throws ZuulException {
		HttpServletRequest request = requestContext.getRequest();
		String requestURI = request.getRequestURI();
		log.info("requestURI: "+ requestURI);
		if (OPTIONS.equalsIgnoreCase(request.getMethod()) || !requestURI.contains(AUTH_PATH) || !requestURI.contains(LOGOUT_URI) || !requestURI.contains(ALIPAY_CALL_URI)) {
			return;
		}
		String authHeader = RequestUtil.getAuthHeader(request);

		if (PublicUtil.isEmpty(authHeader)) {
			throw new ZuulException("刷新页面重试", 403, "check token fail");
		}

		if (authHeader.startsWith(BEARER_TOKEN_TYPE)) {
			requestContext.addZuulRequestHeader(HttpHeaders.AUTHORIZATION, authHeader);

			log.info("authHeader={} ", authHeader);
			// 传递给后续微服务
			requestContext.addZuulRequestHeader(CoreHeaderInterceptor.HEADER_LABEL, authHeader);
		}
	}

}
