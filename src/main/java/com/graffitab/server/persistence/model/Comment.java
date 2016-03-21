package com.graffitab.server.persistence.model;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;
import org.joda.time.DateTime;

import com.graffitab.server.persistence.dao.Identifiable;
import com.graffitab.server.persistence.model.streamable.Streamable;
import com.graffitab.server.persistence.model.user.User;
import com.graffitab.server.persistence.util.DateTimeToLongConverter;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@NamedQueries({
	@NamedQuery(
		name = "Comment.getComments",
		query = "select c "
			  + "from Streamable s "
			  + "join s.comments c "
			  + "where s = :currentStreamable"
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

	@ManyToOne(targetEntity = Streamable.class)
	@JoinColumn(name = "streamable_id", nullable = false, insertable = false, updatable = false)
	private Streamable streamable;

	@OneToOne(targetEntity = User.class)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "text", nullable = false)
	private String text;

	@Convert(converter = DateTimeToLongConverter.class)
	@Column(name = "date", nullable = false)
	private DateTime date;

	@Convert(converter = DateTimeToLongConverter.class)
	@Column(name = "edit_date")
	private DateTime editDate;

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
		comment.setDate(new DateTime());
		return comment;
	}
}
