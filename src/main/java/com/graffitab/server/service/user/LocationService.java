package com.graffitab.server.service.user;

import javax.annotation.Resource;

import org.hibernate.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graffitab.server.api.dto.ListItemsResult;
import com.graffitab.server.api.dto.location.LocationDto;
import com.graffitab.server.api.errors.RestApiException;
import com.graffitab.server.api.errors.ResultCode;
import com.graffitab.server.persistence.dao.HibernateDaoImpl;
import com.graffitab.server.persistence.model.Location;
import com.graffitab.server.persistence.model.User;
import com.graffitab.server.service.PagingService;

@Service
public class LocationService {

	@Resource
	private UserService userService;

	@Resource
	private PagingService pagingService;

	@Resource
	private HibernateDaoImpl<Location, Long> locationDao;

	@Transactional
	public Location createLocation(String address, Double latitude, Double longitude) {
		User currentUser = userService.getCurrentUser();
		userService.merge(currentUser);
		Location toAdd = Location.location(address, latitude, longitude);
		currentUser.getLocations().add(toAdd);
		Location persisted = locationDao.persist(toAdd);
		return persisted;
	}

	@Transactional
	public void deleteLocation(Long locationId) {
		Location toDelete = findLocationById(locationId);

		if (toDelete != null) {
			User currentUser = userService.getCurrentUser();
			userService.merge(currentUser);
			currentUser.getLocations().remove(toDelete);
		}
		else {
			throw new RestApiException(ResultCode.NOT_FOUND, "A location with id " + locationId+ " was not found");
		}
	}

	@Transactional
	public Location editLocation(Long locationId, String address, Double latitude, Double longitude) {
		Location toEdit = findLocationById(locationId);

		if (toEdit != null) {
			User currentUser = userService.getCurrentUser();

			if (currentUser.equals(toEdit.getUser())) {
				toEdit.setAddress(address);
				toEdit.setLatitude(latitude);
				toEdit.setLongitude(longitude);
				return toEdit;
			}
			else {
				throw new RestApiException(ResultCode.USER_NOT_OWNER, "The location with id " + locationId + " cannot be edited by user with id " + currentUser.getId());
			}
		}
		else {
			throw new RestApiException(ResultCode.COMMENT_NOT_FOUND, "Location with id " + locationId + " not found");
		}
	}

	@Transactional
	public ListItemsResult<LocationDto> getLocationsResult(Integer offset, Integer count) {
		User currentUser = userService.getCurrentUser();

		Query query = locationDao.createQuery(
				"select l "
			  + "from User u "
			  + "join u.locations l "
			  + "where u = :currentUser");
		query.setParameter("currentUser", currentUser);

		return pagingService.getPagedItemsResult(Location.class, LocationDto.class, offset, count, query);
	}

	@Transactional(readOnly = true)
	public Location findLocationById(Long id) {
		return locationDao.find(id);
	}
}
