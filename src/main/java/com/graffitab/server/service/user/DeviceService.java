package com.graffitab.server.service.user;

import javax.annotation.Resource;

import org.hibernate.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graffitab.server.persistence.dao.HibernateDaoImpl;
import com.graffitab.server.persistence.model.Device;
import com.graffitab.server.persistence.model.Device.OSType;
import com.graffitab.server.persistence.model.user.User;

import lombok.extern.log4j.Log4j;

@Log4j
@Service
public class DeviceService {

	@Resource
	private UserService userService;

	@Resource
	private HibernateDaoImpl<Device, Long> deviceDao;

	@Transactional
	public void registerDevice(String token, OSType osType) {
		Device device = findDevicesWithTokenAndType(token, osType);
		User currentUser = userService.getCurrentUser();
		userService.merge(currentUser);

		// Check if a device with that token already exists.
		if (device == null) {
			Device toAdd = Device.device(osType, token);
			currentUser.getDevices().add(toAdd);
		}
		else {
			log.debug("A device with token " + token + " already exists");
		}
	}

	@Transactional
	public void unregisterDevice(String token, OSType osType) {
		Device device = findDevicesWithTokenAndType(token, osType);
		User currentUser = userService.getCurrentUser();
		userService.merge(currentUser);

		// Check if a device with that token exists.
		if (device != null) {
			currentUser.getDevices().remove(device);
		}
		else {
			log.debug("A device with token " + token + " was not found");
		}
	}

	Device findDevicesWithTokenAndType(String token, OSType osType) {
		Query query = deviceDao.createNamedQuery("Device.findDevicesWithToken");
		query.setParameter("token", token);
		query.setParameter("osType", osType);
		return (Device) query.uniqueResult();
	}
}
