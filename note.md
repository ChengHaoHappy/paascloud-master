### 登录认证 /auth/form 在 FormAuthenticationConfig 配置
1.前端请求中携带 uac的 clientId 和 clientSecret
2.验证用户名密码是否正确 (loadUserByUsername), 正确后，完善 Authentication (用户身份信息、权限信息等)
3.登录成功后还需验证 clientId 和 clientSecret (loadClientByClientId)
4.生成 OAuth2AccessToken 
5.从配置文件获取 uac 配置的 accessToken 和 refreshToken 的 过期时间
6.保存用户登录信息到数据库 uac_user_token 表， token 信息到 redis，并设置 key 的过期时间
7.记录登录日志

### token 超时刷新策略
设置token有效时间为 t
存储在redis中的有效时间为 2t

验证过程：在t有效期期内，直接返回true
t-2t 内刷新token
大于2t,redis删除token,重新登录

### 请求认证
1. 查找redis中是否包含请求所在的token
2. 验证权限

注意： 此项目中没有对jwt解码来获取用户信息。

