package com.booking.professionalservice.config;

import com.booking.common.model.dto.request.CustomPagingRequest;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;

@Configuration
public class CacheKeyGeneratorConfig {

    @Bean("customPagingKeyGenerator")
    public KeyGenerator customPagingKeyGenerator() {
        return new KeyGenerator() {
            @Override
            public Object generate(Object target, Method method, Object... params) {
                StringBuilder key = new StringBuilder(method.getName());

                for (Object param : params) {
                    key.append("::").append(toKeyPart(param));
                }

                return key.toString();
            }

            private String toKeyPart(Object param) {
                if (param == null) {
                    return "null";
                }

                if (param instanceof String value) {
                    return value;
                }

                if (param instanceof CustomPagingRequest pagingRequest) {
                    Integer pageNumber = pagingRequest.getPagination() != null
                            ? pagingRequest.getPagination().getPageNumber()
                            : null;

                    Integer pageSize = pagingRequest.getPagination() != null
                            ? pagingRequest.getPagination().getPageSize()
                            : null;

                    String sortBy = pagingRequest.getSorting() != null
                            ? pagingRequest.getSorting().getSortBy()
                            : "";

                    String sortDirection = pagingRequest.getSorting() != null
                            ? pagingRequest.getSorting().getSortDirection()
                            : "";

                    return "page=" + pageNumber
                            + ",size=" + pageSize
                            + ",sortBy=" + sortBy
                            + ",sortDir=" + sortDirection;
                }

                return String.valueOf(param);
            }
        };
    }
}
