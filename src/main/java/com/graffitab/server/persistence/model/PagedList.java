package com.graffitab.server.persistence.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.graffitab.server.service.paging.PagingService;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PagedList<T> extends ArrayList<T> {

	private static final long serialVersionUID = 1L;

	@JsonProperty("resultsCount")
	private Integer resultsCount;

	@JsonProperty("offset")
	private Integer offset;

	@JsonProperty("maxResultsCount")
	private Integer maxResultsCount = PagingService.PAGE_SIZE_DEFAULT_VALUE;

	public PagedList(List<T> resultsList, Integer offset, Integer maxResultsCount) {
		super(resultsList);

		this.resultsCount = resultsList.size();
		this.offset = offset;
		this.maxResultsCount = maxResultsCount;
	}

	public PagedList() {
		super();

		this.resultsCount = 0;
		this.offset = 0;
		this.maxResultsCount = PagingService.PAGE_SIZE_DEFAULT_VALUE;
	}
}
