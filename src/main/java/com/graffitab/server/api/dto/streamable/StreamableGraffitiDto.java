package com.graffitab.server.api.dto.streamable;

import com.graffitab.server.api.dto.asset.AssetDto;
import lombok.Data;

@Data
public class StreamableGraffitiDto {

	private Double latitude;
	private Double longitude;
	private Double roll;
	private Double yaw;
	private Double pitch;
	private AssetDto asset;
	private String text;
}
