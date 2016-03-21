package com.graffitab.server.service.user;

import javax.annotation.Resource;

import org.hibernate.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graffitab.server.api.errors.RestApiException;
import com.graffitab.server.api.errors.ResultCode;
import com.graffitab.server.persistence.dao.HibernateDaoImpl;
import com.graffitab.server.persistence.model.Device;
import com.graffitab.server.persistence.model.Device.OSType;
import com.graffitab.server.persistence.model.user.User;

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
		if (device != null) {
			throw new RestApiException(ResultCode.ALREADY_EXISTS, "A device with token " + token + " already exists");
		}

		Device toAdd = Device.device(osType, token);
		currentUser.getDevices().add(toAdd);
	}

	@Transactional
	public void unregisterDevice(String token, OSType osType) {
		Device device = findDevicesWithTokenAndType(token, osType);
		User currentUser = userService.getCurrentUser();
		userService.merge(currentUser);

		// Check if a device with that token exists.
		if (device == null) {
			throw new RestApiException(ResultCode.NOT_FOUND, "A device with token " + token + " was not found");
		}

		currentUser.getDevices().remove(device);
	}

	Device findDevicesWithTokenAndType(String token, OSType osType) {
		Query query = deviceDao.createNamedQuery("Device.findDevicesWithToken");
		query.setParameter("token", token);
		query.setParameter("osType", osType);
		return (Device) query.uniqueResult();
	}
}
