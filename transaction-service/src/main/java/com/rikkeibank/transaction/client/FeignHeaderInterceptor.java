package com.rikkeibank.transaction.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Propagates the caller's trusted identity headers (X-User-Id / X-User-Name / X-User-Role),
 * originally set by the API Gateway, onto every outgoing synchronous service-to-service call
 * (RestTemplate/OpenFeign) so account-service's @PreAuthorize checks keep working.
 */
@Component
public class FeignHeaderInterceptor implements RequestInterceptor {

    private static final String[] FORWARDED_HEADERS = {"X-User-Id", "X-User-Name", "X-User-Role", "Authorization"};

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) return;
        HttpServletRequest request = attributes.getRequest();
        for (String header : FORWARDED_HEADERS) {
            String value = request.getHeader(header);
            if (value != null) {
                template.header(header, value);
            }
        }
    }
}
