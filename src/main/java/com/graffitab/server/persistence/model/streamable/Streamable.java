package com.graffitab.server.persistence.model.streamable;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderColumn;
import javax.persistence.Table;

import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;
import org.joda.time.DateTime;

import com.graffitab.server.persistence.dao.Identifiable;
import com.graffitab.server.persistence.model.Comment;
import com.graffitab.server.persistence.model.User;
import com.graffitab.server.persistence.model.asset.Asset;
import com.graffitab.server.persistence.util.BooleanToStringConverter;
import com.graffitab.server.persistence.util.DateTimeToLongConverter;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@NamedQueries({
	@NamedQuery(
		name = "Streamable.getNewestStreamables",
		query = "select s "
			  + "from Streamable s "
			  + "order by s.date desc"
	),
	@NamedQuery(
		name = "Streamable.getPopularStreamables",
		query = "select s "
			  + "from Streamable s "
			  + "left join s.likers l "
			  + "group by s.id "
			  + "order by count(l) desc"
	),
	@NamedQuery(
		name = "Streamable.getLikedStreamables",
		query = "select s "
			  + "from Streamable s "
			  + "join s.likers u "
			  + "where u = :currentUser "
			  + "order by s.date desc"
	),
	@NamedQuery(
		name = "Streamable.searchStreamablesAtLocation",
		query = "select s "
			  + "from Streamable s "
			  + "where s.latitude is not null and s.longitude is not null " // Check that the streamable has a location.
			  + "and s.latitude <= :neLatitude and s.latitude >= :swLatitude " // Check that the streamable is inside the required GPS rectangle.
			  + "and s.longitude >= :neLongitude and s.longitude <= :swLongitude "
			  + "order by s.date desc"
	),
	@NamedQuery(
		name = "Streamable.hashtagExistsForStreamable",
		query = "select count(*) "
			  + "from Streamable s "
			  + "join s.hashtags h "
			  + "where s = :currentStreamable and :tag in elements(h)"
	),
	@NamedQuery(
		name = "Streamable.getUserStreamables",
		query = "select s "
			  + "from User u "
			  + "join u.streamables s "
			  + "where u = :currentUser "
			  + "order by s.date desc"
	),
	@NamedQuery(
		name = "Streamable.getUserFeed",
		query = "select f "
			  + "from User u "
			  + "join u.feed f "
			  + "where u = :currentUser "
			  + "order by f.date desc"
	),
	@NamedQuery(
		name = "Streamable.getPrivateStreamables",
		query = "select s "
			  + "from User u "
			  + "join u.streamables s "
			  + "where u = :currentUser and s.isPrivate = 'Y' "
			  + "order by s.date desc"
	)
})

@Getter
@Setter
@EqualsAndHashCode
@Entity
@Table(name = "streamable")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "streamable_type", discriminatorType = DiscriminatorType.STRING)
public abstract class Streamable implements Identifiable<Long> {

	private static final long serialVersionUID = 1L;

	public enum StreamableType {
		GRAFFITI
	}

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;

	@ManyToOne(targetEntity = User.class)
	@JoinColumn(name = "user_id", insertable = false, updatable = false)
	private User user;

	@Convert(converter = DateTimeToLongConverter.class)
	@Column(name = "date", nullable = false)
	private DateTime date;

	@Enumerated(EnumType.STRING)
	@Column(name = "streamable_type", nullable = false, insertable = false, updatable = false)
	private StreamableType streamableType;

	@Convert(converter = BooleanToStringConverter.class)
	@Column(name = "is_private", nullable = false)
	private Boolean isPrivate;

	@Convert(converter = BooleanToStringConverter.class)
	@Column(name = "is_flagged", nullable = false)
	private Boolean isFlagged;

	@OneToOne(targetEntity = Asset.class, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "asset_id")
	private Asset asset;

	@ManyToMany(cascade = CascadeType.ALL)
	@JoinTable(name = "gt_like",
			   joinColumns = {@JoinColumn(name = "streamable_id")},
			   inverseJoinColumns = {@JoinColumn(name = "user_id")})
	@OrderColumn(name = "order_key")
	private List<User> likers = new ArrayList<>();

	@OneToMany(targetEntity = Comment.class, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "streamable_id", nullable = false)
	@OrderColumn(name = "order_key")
	private List<Comment> comments = new ArrayList<>();

	@ElementCollection
    @Column(name="tag")
    @CollectionTable(name="hashtag", joinColumns = @JoinColumn(name="streamable_id"))
	@OrderColumn(name = "order_key")
	private List<String> hashtags = new ArrayList<>();

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(Long id) {
		this.id = id;
	}

	public Streamable() {

	}

	public Streamable(StreamableType streamableType) {
		this.streamableType = streamableType;
		this.date = new DateTime();
		this.isFlagged = false;
		this.isPrivate = false;
	}

	/**
	 * @param liker
	 * @return true if the current user likes the specified streamable
	 */
	public boolean isLikedBy(User liker) {
		return likers.contains(liker);
	}
}
