package com.graffitab.server.persistence.model;

import com.graffitab.server.persistence.dao.Identifiable;

/**
 * Created by david.
 */
public class User implements Identifiable<Long> {
	
	private static final long serialVersionUID = 1L;
	private Long id;
	private String externalId;
	private String username;
	private String firstName;
	private String lastName;
	private String password;
	private String email;
	private String website;
	private String about;
	private Avatar avatar;
	private Cover cover;
	
    public User() {

    }

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(Long id) {
		this.id = id;
	}
	
	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getWebsite() {
		return website;
	}

	public void setWebsite(String website) {
		this.website = website;
	}

	public String getAbout() {
		return about;
	}

	public void setAbout(String about) {
		this.about = about;
	}

	public Avatar getAvatar() {
		return avatar;
	}

	public void setAvatar(Avatar avatar) {
		this.avatar = avatar;
	}

	public Cover getCover() {
		return cover;
	}

	public void setCover(Cover cover) {
		this.cover = cover;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}
}