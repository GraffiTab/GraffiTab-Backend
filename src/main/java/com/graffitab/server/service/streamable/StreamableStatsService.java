package com.graffitab.server.service.streamable;

import com.graffitab.server.api.dto.streamable.FullStreamableDto;
import com.graffitab.server.persistence.dao.HibernateDaoImpl;
import com.graffitab.server.persistence.model.streamable.Streamable;
import com.graffitab.server.service.user.UserService;
import lombok.AllArgsConstructor;
import org.hibernate.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by david on 11/11/2017.
 */

@Service
@AllArgsConstructor
public class StreamableStatsService {

    private UserService userService;
    private HibernateDaoImpl<Streamable, Long> streamableDao;

    @Transactional
    public void processStreamableStats(Streamable streamable, FullStreamableDto streamableDto) {
        Query query = streamableDao.createNamedQuery("Streamable.stats");
        query.setParameter("currentUser", userService.getCurrentUser());
        query.setParameter("streamable", streamable);
        Object[] result = (Object[]) query.uniqueResult();

        streamableDto.setCommentsCount((Long) result[0]);
        streamableDto.setLikersCount((Integer) result[1]);

        Long likedByCurrentUserInt = (Long) result[2];
        streamableDto.setLikedByCurrentUser(likedByCurrentUserInt == 1);

    }
}
