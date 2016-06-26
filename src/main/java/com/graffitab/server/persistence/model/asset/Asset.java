package com.graffitab.server.persistence.model.asset;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.graffitab.server.persistence.dao.Identifiable;
import com.graffitab.server.util.GuidGenerator;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

@NamedQueries({
		@NamedQuery(
				name = "Asset.findInState",
				query = "select a "
						+ "from Asset a "
						+ "where a.state = :state"
		),
		@NamedQuery(
				name = "Asset.findByGuid",
				query = "select a "
						+ "from Asset a "
						+ "where a.guid = :guid"
		)
})
@Getter
@Setter
@EqualsAndHashCode
@Entity
@Table(name = "asset")
public class Asset implements Identifiable<Long> {

	private static final long serialVersionUID = 1L;

	public enum AssetType {
		IMAGE
	}

	public enum AssetState {
		RESIZING, PROCESSING, COMPLETED;
	}

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;

	@Column(name = "guid", nullable = false)
	private String guid;

	@Enumerated(EnumType.STRING)
	@Column(name = "asset_type", nullable = false)
	private AssetType assetType;

	@Column(name = "width")
	private Integer width;

	@Column(name = "height")
	private Integer height;

	@Column(name = "thumbnail_width")
	private Integer thumbnailWidth;

	@Column(name = "thumbnail_height")
	private Integer thumbnailHeight;

	@Enumerated(EnumType.STRING)
	@Column(name = "state", nullable = false)
	private AssetState state;


	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(Long id) {
		this.id = id;
	}

	public static Asset asset(AssetType type) {
		Asset asset = new Asset();
		asset.setGuid(GuidGenerator.generate());
		asset.setAssetType(type);
		asset.setState(AssetState.PROCESSING);
		return asset;
	}
}
