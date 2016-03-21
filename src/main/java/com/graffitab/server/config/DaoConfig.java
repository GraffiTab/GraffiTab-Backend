package com.graffitab.server.config;

import java.io.Serializable;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.graffitab.server.persistence.dao.HibernateDaoImpl;
import com.graffitab.server.persistence.dao.Identifiable;
import com.graffitab.server.persistence.model.Comment;
import com.graffitab.server.persistence.model.Device;
import com.graffitab.server.persistence.model.Location;
import com.graffitab.server.persistence.model.asset.Asset;
import com.graffitab.server.persistence.model.notification.Notification;
import com.graffitab.server.persistence.model.streamable.Streamable;
import com.graffitab.server.persistence.model.user.User;
import com.graffitab.server.persistence.model.user.UserSession;

@Configuration
public class DaoConfig {

	@Bean
	public HibernateDaoImpl<User, Long> userDao() {
		return generateDao(User.class);
	}

	@Bean
	public HibernateDaoImpl<Asset, Long> assetDao() {
		return generateDao(Asset.class);
	}

	@Bean
	public HibernateDaoImpl<Device, Long> deviceDao() {
		return generateDao(Device.class);
	}

	@Bean
	public HibernateDaoImpl<Notification, Long> notificationDao() {
		return generateDao(Notification.class);
	}

	@Bean
	public HibernateDaoImpl<UserSession, Long> userSessionDao() {
		return generateDao(UserSession.class);
	}

	@Bean
	public HibernateDaoImpl<Streamable, Long> streamableDao() {
		return generateDao(Streamable.class);
	}

	@Bean
	public HibernateDaoImpl<Comment, Long> commentDao() {
		return generateDao(Comment.class);
	}

	@Bean
	public HibernateDaoImpl<Location, Long> locationDao() {
		return generateDao(Location.class);
	}

	private <T extends Identifiable<K>,K extends Serializable> HibernateDaoImpl<T,K> generateDao(Class<T> entityClass) {
		HibernateDaoImpl<T, K> dao = new HibernateDaoImpl<>();
		dao.setEntityClass(entityClass);
		return dao;
	}
}
