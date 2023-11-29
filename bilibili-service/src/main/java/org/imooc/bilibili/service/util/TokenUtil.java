package org.imooc.bilibili.service.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.imooc.bilibili.domain.Exception.ConditionException;

import java.util.Calendar;
import java.util.Date;

public class TokenUtil {

    private static final String ISSURE = "Eureka";

    public static String generateToken(Long userId) throws Exception {
        // 使用JWT生成token, 选择JWT加密算法
        Algorithm algorithm = Algorithm.RSA256(RSAUtil.getPublicKey(),RSAUtil.getPrivateKey());
        // 使用日历类生成过期时间
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.SECOND, 600);
        // withIssuer: 签发者是谁  withExpiresAt: 过期时间:s  sign: 加密算法
        return JWT.create()
                .withKeyId(String.valueOf(userId))
                .withIssuer(ISSURE)
                .withExpiresAt(calendar.getTime())
                .sign(algorithm);
    }

    public static Long verifyToken(String token){
        // 使用try-catch而不是直接抛出异常
        try {
            Algorithm algorithm = Algorithm.RSA256(RSAUtil.getPublicKey(), RSAUtil.getPrivateKey());
            // 生成验证器
            JWTVerifier verifier = JWT.require(algorithm).build();
            DecodedJWT jwt = verifier.verify(token);
            String userId = jwt.getKeyId();
            return Long.valueOf(userId);
        } catch (TokenExpiredException e) {
            throw new ConditionException("555", "token过期！");
        } catch (Exception e) {
            throw new ConditionException("非法用户token！");
        }
    }

    public static String generateRefreshToken(Long userId) throws Exception {
        // 使用JWT生成token, 选择JWT加密算法
        Algorithm algorithm = Algorithm.RSA256(RSAUtil.getPublicKey(), RSAUtil.getPrivateKey());
        // 使用日历类生成过期时间
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.DAY_OF_MONTH, 7);
        // withIssuer: 签发者是谁  withExpiresAt: 过期时间:s  sign: 加密算法
        return JWT.create()
                .withKeyId(String.valueOf(userId))
                .withIssuer(ISSURE)
                .withExpiresAt(calendar.getTime())
                .sign(algorithm);
    }
}
