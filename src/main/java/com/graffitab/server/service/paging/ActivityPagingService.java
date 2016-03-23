package com.graffitab.server.service.paging;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graffitab.server.api.dto.ListItemsResult;
import com.graffitab.server.persistence.model.PagedList;
import com.graffitab.server.persistence.model.activity.Activity;
import com.graffitab.server.persistence.model.activity.ActivityContainer;
import com.graffitab.server.service.PagingService;

@Service
public class ActivityPagingService extends PagingService {

	public static final Integer NUM_GROUP_ITEMS_DEFAULT_VALUE = 5;
	public static final Integer NUM_GROUP_ITEMS_MAX_VALUE = 20;

	@Transactional
	public <T, K> ListItemsResult<K> getPagedItems(Class<T> targetClass, Class<K> targetDtoClass, Integer numberOfItemsInGroup, Integer offset, Integer count, Query query) {
		numberOfItemsInGroup = numberOfItemsInGroup != null ? Math.abs(numberOfItemsInGroup) : NUM_GROUP_ITEMS_DEFAULT_VALUE;

		// Guard against malicious input.
		if (numberOfItemsInGroup > NUM_GROUP_ITEMS_MAX_VALUE)
			numberOfItemsInGroup = NUM_GROUP_ITEMS_MAX_VALUE;

		// Get list of entities.
		PagedList<T> items = getItems(query, offset, count);
		@SuppressWarnings("unchecked")
		List<ActivityContainer> groupedActivities = groupActivities((List<Activity>) items, numberOfItemsInGroup);

		// Map to list of DTOs.
		List<K> itemDtos = mapper.mapList(groupedActivities, targetDtoClass);

		// Build result list.
		ListItemsResult<K> listItemsResult = new ListItemsResult<>();
		listItemsResult.setItems(itemDtos);
		listItemsResult.setResultsCount(items.getResultsCount());
		listItemsResult.setMaxResultsCount(items.getMaxResultsCount());
		listItemsResult.setOffset(items.getOffset());

		return listItemsResult;
	}

	private List<ActivityContainer> groupActivities(List<Activity> activities, Integer numberOfItemsInGroup) {
    	List<ActivityContainer> groups = new ArrayList<>();

    	if (activities.size() > 0) {
    		Activity previous = activities.get(0);

    		// There's at least one activity item, so setup the first container for it.
    		ActivityContainer container = new ActivityContainer();
    		container.setActivityType(previous.getActivityType());
    		container.setUser(previous.getActivityUser());
    		container.setDate(previous.getDate());
    		groups.add(container);

    		for (Activity activity : activities) {
    			if (activity.isSameTypeOfActivity(previous) && container.getActivities().size() < numberOfItemsInGroup) {
    				if (!activity.isSameActivity(previous)) {
	    				// The current activity item is the same as the previous one, so add it to the current container.
	    				container.getActivities().add(activity);
    				}
    			}
    			else {
    				// The current activity item is not the same as the previous one or the max items count has been reached for the container, so create a new container and add the activity to it.
    				container = new ActivityContainer();
    				container.setActivityType(activity.getActivityType());
    	    		container.setUser(activity.getActivityUser());
    	    		container.setDate(activity.getDate());
    	    		container.getActivities().add(activity);
    	    		groups.add(container);
    			}

    			previous = activity;
    		}
    	}

    	return groups;
    }
}
