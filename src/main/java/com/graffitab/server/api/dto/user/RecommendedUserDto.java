package com.graffitab.server.api.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.graffitab.server.api.dto.asset.AssetDto;
import lombok.Data;

/**
 * Created by david on 17/09/2017.
 */
@Data

public class RecommendedUserDto {
    private Long id;
    private String guid;
    private String username;
    private String firstName;
    private String lastName;
    private Boolean followedByCurrentUser = Boolean.FALSE;

    @JsonProperty("avatar")
    private AssetDto avatarAsset;

    @JsonProperty("cover")
    private AssetDto coverAsset;

}
