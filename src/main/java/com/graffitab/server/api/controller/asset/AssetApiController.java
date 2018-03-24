package com.graffitab.server.api.controller.asset;

import com.graffitab.server.api.dto.asset.AssetDto;
import com.graffitab.server.api.dto.asset.result.AssetResult;
import com.graffitab.server.api.mapper.OrikaMapper;
import com.graffitab.server.persistence.model.asset.Asset;
import com.graffitab.server.service.asset.AssetService;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * Created by david on 26/06/2016.
 */
@RestController
@RequestMapping("/v1/assets")
@Api(value="Assets")
public class AssetApiController {

    @Resource
    private AssetService assetService;

    @Resource
    private OrikaMapper mapper;

    @ApiOperation(value = "Progress")
    @RequestMapping(value = {"/{guid}/progress"}, method = RequestMethod.GET, produces = {"application/json"})
    public AssetResult getAssetProgress(@PathVariable("guid") String assetGuid) {
        AssetResult assetResult = new AssetResult();
        Asset asset = assetService.getAsset(assetGuid);
        assetResult.setAsset(mapper.map(asset, AssetDto.class));
        return assetResult;
    }
}
