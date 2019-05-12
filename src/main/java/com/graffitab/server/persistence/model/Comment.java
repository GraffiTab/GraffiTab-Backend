package com.graffitab.server.persistence.model;

import com.graffitab.server.persistence.dao.Identifiable;
import com.graffitab.server.persistence.model.streamable.Streamable;
import com.graffitab.server.persistence.model.user.User;
import com.graffitab.server.persistence.util.BooleanToStringConverter;
import com.graffitab.server.persistence.util.DateTimeToLongConverter;

import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;
import org.joda.time.DateTime;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@NamedQueries({
	@NamedQuery(
		name = "Comment.getComments",
		query = "select c "
			  + "from Streamable s "
			  + "join s.comments c "
			  + "where s = :currentStreamable "
			  + "and c.isDeleted = 'N' " // Enforce rules for hidden items.
			  + "order by c.createdOn desc"
	)
})

@Getter
@Setter
@EqualsAndHashCode
@Entity
@Table(name = "comment")
public class Comment implements Identifiable<Long> {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "streamable_id")
	private Streamable streamable;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@Column(name = "text", nullable = false)
	private String text;

	@Convert(converter = DateTimeToLongConverter.class)
	@Column(name = "created_on", nullable = false)
	private DateTime createdOn;

	@Convert(converter = DateTimeToLongConverter.class)
	@Column(name = "updated_on")
	private DateTime updatedOn;

	@Convert(converter = BooleanToStringConverter.class)
	@Column(name = "is_deleted", nullable = false)
	private Boolean isDeleted;

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(Long id) {
		this.id = id;
	}

	public static Comment comment() {
		Comment comment = new Comment();
		comment.setCreatedOn(new DateTime());
		comment.setIsDeleted(false);
		return comment;
	}
}
