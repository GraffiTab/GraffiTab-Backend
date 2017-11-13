package com.graffitab.server.service.paging;

import com.graffitab.server.api.dto.ListItemsResult;
import com.graffitab.server.api.mapper.OrikaMapper;
import com.graffitab.server.persistence.model.PagedList;
import org.hibernate.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
public class PagingService {

	public static final Integer PAGE_SIZE_DEFAULT_VALUE = 10;
	public static final Integer PAGE_SIZE_MAX_VALUE = 20;
	public static final Integer PAGE_SIZE_MAX_VALUE_LOCATION = 50;
	public static final Integer PAGE_SIZE_DEFAULT_VALUE_LOCATION = PAGE_SIZE_MAX_VALUE_LOCATION;

	@Resource
	protected OrikaMapper mapper;

	@Transactional
	public <T, K> ListItemsResult<K> getPagedItems(Class<T> targetClass, Class<K> targetDtoClass, Integer offset, Integer limit, Query query) {
		PagedList<T> items = getItems(query, offset, limit);
		return mapResults(items, targetDtoClass);
	}

	@SuppressWarnings("unchecked")
	@Transactional
	public <T> PagedList<T> getItems(Query query, Integer offset, Integer limit) {
		offset = offset != null ? Math.abs(offset) : 0;
		limit = limit != null ? Math.abs(limit) : PAGE_SIZE_DEFAULT_VALUE;

		// Guard against malicious input.
		if (limit > PAGE_SIZE_MAX_VALUE)
			limit = PAGE_SIZE_MAX_VALUE;

		query.setFirstResult(offset);
		query.setMaxResults(limit);

		return new PagedList<>((List<T>) query.list(), offset, limit);
	}


	public <T, K> ListItemsResult<K> mapResults(PagedList<T> items, Class<K> targetDtoClass) {
		// Map to list of DTOs.
		List<K> itemDtos = mapper.mapList(items, targetDtoClass);

		// Build result list.
		ListItemsResult<K> listItemsResult = new ListItemsResult<>();
		listItemsResult.setItems(itemDtos);
		listItemsResult.setResultsCount(items.getResultsCount());
		listItemsResult.setLimit(items.getLimit());
		listItemsResult.setOffset(items.getOffset());

		return listItemsResult;
	}
}
