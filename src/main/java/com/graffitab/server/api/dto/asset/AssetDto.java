package com.graffitab.server.api.dto.asset;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonInclude(Include.NON_NULL)
public class AssetDto {

	private String guid;

	@JsonProperty("type")
	private String assetType;

	private String link;
	private String thumbnail;
	private Integer width;
	private Integer height;
	private Integer thumbnailWidth;
	private Integer thumbnailHeight;
	private String state;
}
