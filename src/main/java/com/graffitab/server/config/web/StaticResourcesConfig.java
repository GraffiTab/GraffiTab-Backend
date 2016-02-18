package com.graffitab.server.config.web;

import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration.WebMvcAutoConfigurationAdapter;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

@Configuration
public class StaticResourcesConfig extends WebMvcAutoConfigurationAdapter {
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		// Defaults to the opposite (lowest precedence), but having it as the highest
		// allows us to map a controller to /** (changing in the properties the staticPath to /public/**.
		// It is useful having that controller as a gutter for 404 NOT_FOUND errors / endpoints.
		registry.setOrder(Ordered.HIGHEST_PRECEDENCE);
		super.addResourceHandlers(registry);
	}

	@Override
	public InternalResourceViewResolver defaultViewResolver() {
		InternalResourceViewResolver resolver = new InternalResourceViewResolver();
		resolver.setPrefix("/WEB-INF/jsp/");
		resolver.setSuffix(".jspx");
		return resolver;
	}
}
