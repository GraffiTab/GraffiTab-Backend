package com.graffitab.server.api.authentication;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.springframework.stereotype.Component;

import com.graffitab.server.service.user.UserService;

@Component
public class ClearThreadLocalsFilter implements Filter {

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	  // Nothing to do
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {

		 try {
			 chain.doFilter(request, response);
		 } finally {
			 // After every request the thread local has to be cleared, as threads
			 // are constantly reused
			 UserService.clearThreadLocalUserCache();
		 }
	}

	@Override
	public void destroy() {
		// Nothing to do
	}

}
