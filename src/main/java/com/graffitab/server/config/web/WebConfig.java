package com.graffitab.server.config.web;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.multipart.support.MultipartFilter;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.util.List;

@Configuration
@EnableWebMvc
@EnableAspectJAutoProxy
@EnableTransactionManagement
@Import(StaticResourcesConfig.class)
@ComponentScan(
  basePackages={"com.graffitab.server"}, useDefaultFilters = false,
  includeFilters = {
	      @Filter(type = FilterType.ANNOTATION, classes = {Controller.class, RestController.class}),
	      @Filter(type = FilterType.ANNOTATION, classes = {Component.class})
  }
)

public class WebConfig extends WebMvcConfigurationSupport {

    // Note: Static resources work through extending WebMvcAutoConfigurationAdapter {

	@Bean
	public RequestMappingHandlerAdapter requestMappingHandlerAdapter() {
		RequestMappingHandlerAdapter requestMappingHandlerAdapter = super.requestMappingHandlerAdapter();
		return requestMappingHandlerAdapter;
	}

	@Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
    }

	@Bean
	public MappingJackson2HttpMessageConverter delegateJacksonHttpMessageConverter() {
		return new MappingJackson2HttpMessageConverter();
	}

	@Bean(name = "delegate")
	public MappingJackson2HttpMessageConverter delegate() {
		MappingJackson2HttpMessageConverter conv = new MappingJackson2HttpMessageConverter();
		return conv;
	}

	@Bean
	public SingleJsonPropertyMappingJackson2HttpMessageConverter jacksonHttpMessageConverter() {
		return new SingleJsonPropertyMappingJackson2HttpMessageConverter();
	}

	@Bean
	public JsonDtoArgumentResolver jsonDtoArgumentResolver() {
		JsonDtoArgumentResolver resolver = new JsonDtoArgumentResolver();
		return resolver;
	}

	@Bean
	public List<HttpMessageConverter<?>> converters() {
		return requestMappingHandlerAdapter().getMessageConverters();
	}

	@Bean
    public CommonsMultipartResolver commonsMultipartResolver() {
        final CommonsMultipartResolver commonsMultipartResolver = new CommonsMultipartResolver();
        commonsMultipartResolver.setMaxUploadSize(4 * 1024 * 1024);
        return commonsMultipartResolver;
    }

	@Bean
    public FilterRegistrationBean multipartFilterRegistrationBean() {
        final MultipartFilter multipartFilter = new MultipartFilter();
        final FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean(multipartFilter);
        filterRegistrationBean.addInitParameter("multipartResolverBeanName", "commonsMultipartResolver");
        return filterRegistrationBean;
    }

	@Bean
	public LocaleResolver localeResolver() {
		return new FallbackAcceptHeaderLocaleResolver();
	}

	@Override
	public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
		converters.clear();
		converters.add(jacksonHttpMessageConverter());
	}

	@Override
	protected void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
		argumentResolvers.add(jsonDtoArgumentResolver());
	}


}
