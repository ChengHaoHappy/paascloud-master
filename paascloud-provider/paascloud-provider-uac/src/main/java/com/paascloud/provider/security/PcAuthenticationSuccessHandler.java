package com.paascloud.provider.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paascloud.core.utils.RequestUtil;
import com.paascloud.provider.service.UacUserService;
import com.paascloud.security.core.SecurityUser;
import com.paascloud.wrapper.WrapMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.UnapprovedClientAuthenticationException;
import org.springframework.security.oauth2.provider.*;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 * APP环境下认证成功处理器.
 *
 * @author paascloud.net@gmail.com
 */
@Component("pcAuthenticationSuccessHandler")
@Slf4j
public class PcAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

	@Resource
	private ObjectMapper objectMapper;
	@Resource
	private ClientDetailsService clientDetailsService;
	@Resource
	private UacUserService uacUserService;
	@Resource
	private AuthorizationServerTokenServices authorizationServerTokenServices;

	private static final String BEARER_TOKEN_TYPE = "Basic ";

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
	                                    Authentication authentication) throws IOException, ServletException {

		logger.info("登录成功");
        //用户密码登录成功后还需要验证clientId，clientSecret
		//取出头部有关client的token，解码获得clientId，clientSecret为生成accessToken做准备
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);

		if (header == null || !header.startsWith(BEARER_TOKEN_TYPE)) {
			throw new UnapprovedClientAuthenticationException("请求头中无client信息");
		}


		String[] tokens = RequestUtil.extractAndDecodeHeader(header);
		assert tokens.length == 2;   //如果[boolean表达式]为true，则程序继续执行。如果为false，则程序抛出AssertionError，并终止执行。

		String clientId = tokens[0];
		String clientSecret = tokens[1];

		//从配置文件中加载配置的token过期时间等信息
		ClientDetails clientDetails = clientDetailsService.loadClientByClientId(clientId);

		if (clientDetails == null) {
			throw new UnapprovedClientAuthenticationException("clientId对应的配置信息不存在:" + clientId);
		} else if (!StringUtils.equals(clientDetails.getClientSecret(), clientSecret)) {
			throw new UnapprovedClientAuthenticationException("clientSecret不匹配:" + clientId);
		}

		TokenRequest tokenRequest = new TokenRequest(MapUtils.EMPTY_MAP, clientId, clientDetails.getScope(), "custom");  //自定义授权模式

		OAuth2Request oAuth2Request = tokenRequest.createOAuth2Request(clientDetails);

		OAuth2Authentication oAuth2Authentication = new OAuth2Authentication(oAuth2Request, authentication);

		OAuth2AccessToken token = authorizationServerTokenServices.createAccessToken(oAuth2Authentication);
		SecurityUser principal = (SecurityUser) authentication.getPrincipal();  //获取用户基本信息
		uacUserService.handlerLoginData(token, principal, request);

		log.info("tokenValue:"+token.getValue());

		log.info("用户【 {} 】记录登录日志", principal.getUsername());

		response.setContentType("application/json;charset=UTF-8");
		response.getWriter().write((objectMapper.writeValueAsString(WrapMapper.ok(token))));

	}

}
