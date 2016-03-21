package com.graffitab.server.persistence.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

import com.graffitab.server.persistence.dao.Identifiable;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@NamedQueries({
	@NamedQuery(
		name = "Location.getLocations",
		query = "select l "
			  + "from User u "
			  + "join u.locations l "
			  + "where u = :currentUser"
	)
})

@Getter
@Setter
@EqualsAndHashCode
@Entity
@Table(name = "location")
public class Location implements Identifiable<Long> {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;

	@ManyToOne(targetEntity = User.class)
	@JoinColumn(name = "user_id", insertable = false, updatable = false)
	private User user;

	@Column(name = "address", nullable = false)
	private String address;

	@Column(name = "latitude", nullable = false)
	private Double latitude;

	@Column(name = "longitude", nullable = false)
	private Double longitude;

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(Long id) {
		this.id = id;
	}

	public static Location location(String address, Double latitude, Double longitude) {
		Location location = new Location();
		location.setAddress(address);
		location.setLatitude(latitude);
		location.setLongitude(longitude);
		return location;
	}
}
