package com.graffitab.server.persistence.model.user;

import com.graffitab.server.persistence.dao.Identifiable;
import com.graffitab.server.persistence.model.Device;
import com.graffitab.server.persistence.model.Location;
import com.graffitab.server.persistence.model.activity.Activity;
import com.graffitab.server.persistence.model.asset.Asset;
import com.graffitab.server.persistence.model.externalprovider.ExternalProvider;
import com.graffitab.server.persistence.model.notification.Notification;
import com.graffitab.server.persistence.model.streamable.Streamable;
import com.graffitab.server.persistence.util.BooleanToStringConverter;
import com.graffitab.server.persistence.util.DateTimeToLongConverter;

import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;
import org.joda.time.DateTime;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import javax.persistence.Transient;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by david.
 */

@NamedQueries({
	@NamedQuery(
		name = "User.findUsersWithEmail",
		query = "select u "
			  + "from User u "
			  + "where u.email = :email and u.id != :userId"
	),
	@NamedQuery(
		name = "User.findUsersWithUsername",
		query = "select u "
			  + "from User u "
			  + "where u.username = :username and u.id != :userId"
	),
	@NamedQuery(
		name = "User.findUsersWithMetadataValues",
		query = "select u "
			  + "from User u "
			  + "join u.metadataItems mi "
			  + "where index(mi) = :metadataKey and :metadataValue in elements(mi)"
	),
	@NamedQuery(
		name = "User.getFollowerIds",
		query = "select f.id "
			  + "from User u "
			  + "join u.followers f "
			  + "where u = :currentUser"
	),
	@NamedQuery(
		name = "User.searchUser",
		query = "select u "
			  + "from User u "
			  + "where (u.username like :username "
			  + "or u.firstName like :firstName "
			  + "or u.lastName like :lastName) "
			  + "and u.accountStatus != 'PENDING_ACTIVATION'"
	),
	@NamedQuery(
		name = "User.getMostActiveUsers",
		query = "select u "
			  + "from User u "
			  + "left join u.streamables s "
			  + "where u.accountStatus != 'PENDING_ACTIVATION' "
			  + "group by u.id "
			  + "order by count(s) desc"
	),
	@NamedQuery(
		name = "User.getLikers",
		query = "select u "
			  + "from Streamable s "
			  + "join s.likers u "
			  + "where s = :currentStreamable "
			  + "and u.accountStatus != 'PENDING_ACTIVATION'"
	),
	@NamedQuery(
		name = "User.getFollowers",
		query = "select f "
			  + "from User u "
			  + "join u.followers f "
			  + "where u = :currentUser "
			  + "and u.accountStatus != 'PENDING_ACTIVATION'"
	),
	@NamedQuery(
		name = "User.getFollowing",
		query = "select f "
			  + "from User u "
			  + "join u.following f "
			  + "where u = :currentUser "
			  + "and u.accountStatus != 'PENDING_ACTIVATION'"

	),
	@NamedQuery(
		name = "User.isFollowedByCurrentUser",
		query = "select u.id "
			  + "from User u "
			  + "join u.following f "
			  + "where f = :otherUser and "
			  + "u = :currentUser"

	),
	@NamedQuery(
		name = "User.stats",
		query = "select (select count(*) from u.streamables where isDeleted = 'N') as streamablesCount, "
				+ "u.followers.size as followersCount, "
				+ "u.following.size as followingCount "
				+ "from User u "
				+ "where u = :user"

	),
	@NamedQuery(
		name = "User.findExternalProviderForUser",
		query = "select e "
			  + "from User u "
			  + "join u.externalProviders e "
			  + "where e.externalProviderType = :externalProviderType "
			  + "and u = :currentUser"
	),
	@NamedQuery(
		name = "User.findUserWithExternalProvider",
		query = "select u "
			  + "from User u "
			  + "join u.externalProviders e "
			  + "where e.externalProviderType = :externalProviderType "
			  + "and e.externalUserId = :externalUserId"
	),
	@NamedQuery(
		name = "User.findWhoToFollow",
		query = "select u "
				+ "from User u "
				+ "where u.isRecommendation = 'Y' "
                + "and u.accountStatus != 'PENDING_ACTIVATION' "
				+ "and u != :currentUser "
				+ "and u.id not in (select f.id from User fu join fu.following f where fu.id = :currentUserId) "
                + "order by u.recommendationRank desc"

	)
})

@Getter
@Setter
@Entity
@Table(name="gt_user")
@Builder
@AllArgsConstructor
public class User implements Identifiable<Long>, UserDetails {

	public enum AccountStatus {
		PENDING_ACTIVATION, ACTIVE, SUSPENDED, RESET_PASSWORD;
	}

	private static final long serialVersionUID = 1L;
	public User() {}

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;

	@Column(name = "guid", nullable = false)
	private String guid;

	@Column(name = "username", nullable = false)
	private String username;

	@Column(name = "firstname", nullable = false)
	private String firstName;

	@Column(name = "lastname", nullable = false)
	private String lastName;

	@Column(name = "password", nullable = false)
	private String password;

	@Column(name = "email", nullable = false)
	private String email;

	@Column(name = "website", nullable = true)
	private String website;

	@Column(name = "about", nullable = true)
	private String about;

	@Convert(converter = DateTimeToLongConverter.class)
	@Column(name = "created_on", nullable = false)
	private DateTime createdOn;

	@Convert(converter = DateTimeToLongConverter.class)
	@Column(name = "updated_on")
	private DateTime updatedOn;

    @Column(name = "failed_logins")
    private Integer failedLogins;

	@Enumerated(EnumType.STRING)
	@Column(name = "status")
	private AccountStatus accountStatus = AccountStatus.PENDING_ACTIVATION;

	@Convert(converter = BooleanToStringConverter.class)
	@Column(name = "is_recommendation", nullable = false)
	private Boolean isRecommendation = Boolean.FALSE;

	@Column(name = "recommendation_rank")
	private Integer recommendationRank;

	@OneToOne(targetEntity = Asset.class, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "avatar_asset_id")
	private Asset avatarAsset;

	@OneToOne(targetEntity = Asset.class, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "cover_asset_id")
	private Asset coverAsset;

	@Transient
	private Boolean followedByCurrentUser = Boolean.FALSE;

	@ManyToMany(cascade = CascadeType.ALL)
	@JoinTable(name = "following",
			   joinColumns = {@JoinColumn(name = "following_id")},
			   inverseJoinColumns = {@JoinColumn(name = "user_id")})
	@OrderColumn(name = "order_key")
	private List<User> followers = new ArrayList<>();

	@ManyToMany(cascade = CascadeType.ALL)
	@JoinTable(name = "following",
			   joinColumns = {@JoinColumn(name = "user_id")},
			   inverseJoinColumns = {@JoinColumn(name = "following_id")})
	@OrderColumn(name = "order_key")
	private List<User> following = new ArrayList<>();

	@OneToMany(targetEntity = Device.class, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "user_id", nullable = false)
	@OrderColumn(name = "order_key")
	private List<Device> devices = new ArrayList<>();

	@OneToMany(targetEntity = Notification.class, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "user_id", nullable = false)
	@OrderColumn(name = "order_key")
	private List<Notification> notifications = new ArrayList<>();

	@OneToMany(targetEntity = ExternalProvider.class, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "user_id", nullable = false)
	@OrderColumn(name = "order_key")
	private List<ExternalProvider> externalProviders = new ArrayList<>();

	@ElementCollection
    @MapKeyColumn(name = "metadata_key")
    @Column(name="metadata_value")
    @CollectionTable(name="gt_user_metadata", joinColumns = @JoinColumn(name="user_id"))
	private Map<String, String> metadataItems = new HashMap<>();

	@OneToMany(targetEntity = Streamable.class, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "user_id", nullable = false)
	@OrderColumn(name = "order_key")
	private List<Streamable> streamables = new ArrayList<>();

	@OneToMany(targetEntity = Location.class, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "user_id", nullable = false)
	@OrderColumn(name = "order_key")
	private List<Location> locations = new ArrayList<>();

	@OneToMany(targetEntity = Activity.class, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "user_id", nullable = false)
	@OrderColumn(name = "order_key")
	private List<Activity> activity = new ArrayList<>();

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		GrantedAuthority g = new SimpleGrantedAuthority("ROLE_USER");

		return Collections.singletonList(g);
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public String getUsername() {
		return username;
	}

	public String getEmail() {
		return email.toLowerCase();
	}

	public void setEmail(String email) {
		this.email = email.toLowerCase();
	}

	/**
	 * @param followee
	 * @return true if the current user is following the specified user
	 */
	public boolean isFollowing(User followee) {
		// TODO: Evaluate using a query.
		return following.contains(followee);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		User other = (User) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
}
