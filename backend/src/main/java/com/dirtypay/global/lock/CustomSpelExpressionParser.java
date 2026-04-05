package com.dirtypay.global.lock;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * SpEL 표현식으로 분산 락 키를 생성하는 유틸리티.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
public class CustomSpelExpressionParser {

    private static final ExpressionParser PARSER = new SpelExpressionParser();

    private CustomSpelExpressionParser() {}

    /**
     * 메서드 파라미터를 컨텍스트에 등록하여 SpEL 표현식을 평가한다.
     *
     * @param expression SpEL 표현식 (예: "'wallet:' + #receiverId")
     * @param joinPoint  AOP 조인 포인트
     * @return 평가된 락 키 문자열
     */
    public static String parse(String expression, ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        StandardEvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < paramNames.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }
        return String.valueOf(PARSER.parseExpression(expression).getValue(context));
    }
}
