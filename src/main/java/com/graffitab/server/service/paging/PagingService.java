package com.graffitab.server.service.paging;

import java.util.List;

import javax.annotation.Resource;

import org.hibernate.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graffitab.server.api.dto.ListItemsResult;
import com.graffitab.server.api.mapper.OrikaMapper;
import com.graffitab.server.persistence.model.PagedList;

@Service
public class PagingService {

	public static final Integer PAGE_SIZE_DEFAULT_VALUE = 10;
	public static final Integer PAGE_SIZE_MAX_VALUE = 20;

	@Resource
	protected OrikaMapper mapper;

	@Transactional
	public <T, K> ListItemsResult<K> getPagedItems(Class<T> targetClass, Class<K> targetDtoClass, Integer offset, Integer count, Query query) {
		// Get list of entities.
		PagedList<T> items = getItems(query, offset, count);

		// Map to list of DTOs.
		List<K> itemDtos = mapper.mapList(items, targetDtoClass);

		// Build result list.
		ListItemsResult<K> listItemsResult = new ListItemsResult<>();
		listItemsResult.setItems(itemDtos);
		listItemsResult.setResultsCount(items.getResultsCount());
		listItemsResult.setMaxResultsCount(items.getMaxResultsCount());
		listItemsResult.setOffset(items.getOffset());

		return listItemsResult;
	}

	@SuppressWarnings("unchecked")
	protected <T> PagedList<T> getItems(Query query, Integer offset, Integer count) {
		offset = offset != null ? Math.abs(offset) : 0;
		count = count != null ? Math.abs(count) : PAGE_SIZE_DEFAULT_VALUE;

		// Guard against malicious input.
		if (count > PAGE_SIZE_MAX_VALUE)
			count = PAGE_SIZE_MAX_VALUE;

		query.setFirstResult(offset);
		query.setMaxResults(count);

		return new PagedList<>((List<T>) query.list(), offset, count);
	}
}
