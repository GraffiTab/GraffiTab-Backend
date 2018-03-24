package com.graffitab.server.config.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
public class StaticResourcesConfig extends WebMvcConfigurerAdapter {

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		// Defaults to the opposite (lowest precedence), but having it as the highest
		// allows us to map a controller to '/**' (changing the 'spring.mvc.static-path-pattern' property
		// to '/public/**' instead of default '/**').
		// It is useful having that controller as a gutter for 404 NOT_FOUND errors / endpoints.
		registry.setOrder(Ordered.HIGHEST_PRECEDENCE);
		registry.addResourceHandler("/public/**")
				.addResourceLocations("classpath:public/");

         // Setup Swagger UI
        // This is for the default swagger config.
//        registry.addResourceHandler("swagger-ui.html")
//                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/documentation/**")
                .addResourceLocations("classpath:/dist/");
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
	}

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/docs").setViewName("redirect:/docs/");
        registry.addViewController("/docs/").setViewName("forward:/documentation/index.html");
	    super.addViewControllers(registry);
    }
}
