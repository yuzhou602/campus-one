package com.campusone.security;

import com.campusone.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
public class AopAuthorizationAspect {
    private final PermissionService permissionService;

    @Around("@within(com.campusone.security.RequiresRole) || @annotation(com.campusone.security.RequiresRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint) throws Throwable {
        RequiresRole requiresRole = resolveRequiresRole(joinPoint);
        if (requiresRole == null) {
            throw new BusinessException(403, "无法确认接口权限，已拒绝访问");
        }
        if (!permissionService.hasAnyRole(requiresRole.value())) {
            throw new BusinessException(403, "无权限访问: 需要角色 " + String.join("/", requiresRole.value()));
        }
        return joinPoint.proceed();
    }

    private RequiresRole resolveRequiresRole(ProceedingJoinPoint joinPoint) {
        RequiresRole ann = null;
        try {
            Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
            ann = method.getAnnotation(RequiresRole.class);
            if (ann == null && joinPoint.getTarget() != null) {
                ann = joinPoint.getTarget().getClass().getAnnotation(RequiresRole.class);
            }
        } catch (Throwable ex) {
            throw new BusinessException(403, "权限配置解析失败");
        }
        return ann;
    }
}
