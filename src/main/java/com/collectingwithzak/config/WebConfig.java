package com.collectingwithzak.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new PageAttributeInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/static/**", "/api/**");
    }

    static class PageAttributeInterceptor implements HandlerInterceptor {
        @Override
        public void postHandle(HttpServletRequest request, HttpServletResponse response,
                               Object handler, ModelAndView modelAndView) {
            if (modelAndView == null || modelAndView.getViewName() == null
                    || modelAndView.getViewName().startsWith("redirect:")) return;

            String path = request.getRequestURI();
            String page;
            if ("/".equals(path)) {
                page = "dashboard";
            } else {
                String segment = path.substring(1);
                int slash = segment.indexOf('/');
                page = slash > 0 ? segment.substring(0, slash) : segment;
            }
            modelAndView.addObject("page", page);
        }
    }
}
