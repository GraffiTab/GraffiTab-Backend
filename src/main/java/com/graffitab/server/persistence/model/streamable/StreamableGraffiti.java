package com.graffitab.server.persistence.model.streamable;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Getter
@Setter
@Entity
@DiscriminatorValue("GRAFFITI")
public class StreamableGraffiti extends Streamable {

	private static final long serialVersionUID = 1L;

	@Column(name = "latitude")
	private Double latitude;

	@Column(name = "longitude")
	private Double longitude;

	@Column(name = "roll")
	private Double roll;

	@Column(name = "yaw")
	private Double yaw;

	@Column(name = "pitch")
	private Double pitch;

	public StreamableGraffiti() {
		super(StreamableType.GRAFFITI);
	}

	public StreamableGraffiti(Double latitude, Double longitude, Double roll, Double yaw, Double pitch) {
		super(StreamableType.GRAFFITI);

		this.latitude = latitude;
		this.longitude = longitude;
		this.roll = roll;
		this.yaw = yaw;
		this.pitch = pitch;
	}

	public StreamableGraffiti(Double latitude, Double longitude, Double roll, Double yaw, Double pitch, String text) {
		super(StreamableType.GRAFFITI);
		super.text = text;
		this.latitude = latitude;
		this.longitude = longitude;
		this.roll = roll;
		this.yaw = yaw;
		this.pitch = pitch;
	}
}
