package com.graffitab.server.api.mapper;

import com.graffitab.server.api.dto.asset.AssetDto;
import com.graffitab.server.persistence.model.asset.Asset;
import com.graffitab.server.service.store.DatastoreService;
import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MappingContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;

@Component
public class AssetMapper extends CustomMapper<Asset, AssetDto> {

	@Resource
	private DatastoreService datastoreService;

	@Override
	public void mapAtoB(Asset asset, AssetDto assetDto, MappingContext context) {

		// Map directly url if present

		if (StringUtils.hasText(asset.getUrl())) {
			assetDto.setLink(asset.getUrl());
		} else {
			assetDto.setLink(datastoreService.generateDownloadLink(asset.getGuid()));
		}

		if (StringUtils.hasText(asset.getThumbnailUrl())) {
			assetDto.setThumbnailLink(asset.getThumbnailUrl());
		} else {
			assetDto.setThumbnail(datastoreService.generateThumbnailLink(asset.getGuid()));
		}
	}

	@Override
	public void mapBtoA(AssetDto assetDto, Asset asset, MappingContext context) {

		if (StringUtils.hasText(assetDto.getLink())) {
			asset.setUrl(assetDto.getLink());
		}

		if (StringUtils.hasText(assetDto.getThumbnailLink())) {
			asset.setThumbnailUrl(assetDto.getThumbnailLink());
		}
	}
}
