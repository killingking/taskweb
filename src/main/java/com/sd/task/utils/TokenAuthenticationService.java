package com.sd.task.utils;

import com.sd.task.pojo.dto.JSONResult;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class TokenAuthenticationService {
    static final long EXPIRETIONTIME = 432_000_000;//5天
    //jwt密码
    static final String SECRET = "0233697b-85bf-b203-a49e-c1a5a1e8579d";
    //token前缀
    static final String TOKEN_PREFIX = "Bearer";
    //存放token的header key
    static final String HEADER_STRING = "Authorization";

    //JWT生成方法
    public static void addAuthentication(HttpServletResponse response, String username, Authentication authResult) {
        Collection<? extends GrantedAuthority> authorities = authResult.getAuthorities();
        String authority = authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(","));
        //生成JWT
        String JWT = Jwts.builder()
                //保存权限(角色)
                .claim("authorities", authority)
                //用户名写入标题
                .setSubject(username)
                //有效期设置
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRETIONTIME))
                //签名设置
                .signWith(SignatureAlgorithm.HS512, SECRET)
                .compact();
        //将JWT写入body
        try {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_OK);
            HashMap<String, Object> token = new HashMap<>();
            token.put("token", TOKEN_PREFIX + JWT);
            response.getOutputStream().println(JSONResult.fillResultString(0, "login success", token));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //JWT验证方法
    public static Authentication getAuthentication(HttpServletRequest request) {
        //从Header中拿到token
        String token = request.getHeader(HEADER_STRING);
        if (StringUtils.hasLength(token)) {
            //解析token
            Claims claims = Jwts.parser()
                    //验签
                    .setSigningKey(SECRET)
                    //去掉前缀
                    .parseClaimsJws(token.replace(TOKEN_PREFIX, ""))
                    .getBody();
            //拿到用户名
            String user = claims.getSubject();
            //得到权限(角色)
            List<GrantedAuthority> authorities = AuthorityUtils.commaSeparatedStringToAuthorityList("authorities");
            return user != null ?
                    new UsernamePasswordAuthenticationToken(user, null, authorities) :
                    null;
        }
        return null;
    }

    //JWT验证方法
    public static String getAuthentication(String token) {
        //从Header中拿到token
        if (StringUtils.hasLength(token)) {
            //解析token
            Claims claims = Jwts.parser()
                    //验签
                    .setSigningKey(SECRET)
                    //去掉前缀
                    .parseClaimsJws(token.replace(TOKEN_PREFIX, ""))
                    .getBody();
            //拿到用户名
            String user = claims.getSubject();
            return user;
        }
        return null;
    }
}
