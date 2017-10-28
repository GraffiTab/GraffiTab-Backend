package com.graffitab.server.service;

import com.graffitab.server.api.errors.RestApiException;
import com.graffitab.server.api.errors.ResultCode;
import com.graffitab.server.persistence.model.Comment;
import com.graffitab.server.persistence.model.streamable.Streamable;
import com.graffitab.server.persistence.model.user.User;
import com.graffitab.server.service.job.JobService;
import com.graffitab.server.service.notification.NotificationService;
import com.graffitab.server.service.streamable.StreamableService;
import com.graffitab.server.service.user.RunAsUser;
import com.graffitab.server.service.user.UserService;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;

import lombok.extern.log4j.Log4j;

/**
 * Created by david on 20/09/2017.
 */
@Log4j
@Service
public class TextUtilsService {

    private static final Pattern HASH_PATTERN = Pattern.compile("#(\\w+|\\W+)");
    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\w+|\\W+)");

    @Resource
    private UserService userService;

    @Resource
    private StreamableService streamableService;

    @Resource
    private NotificationService notificationService;

    @Resource
    private TransactionUtils transactionUtils;

    @Resource
    private JobService jobService;

    public void parseCommentForSpecialSymbols(Comment comment, Streamable streamable) {
        User currentUser = userService.getCurrentUser();
        jobService.execute(() -> {
            parseText(currentUser, comment.getText(), streamable, comment);
        });
    }

    public void parseStreamableTextForSpecialSymbols(Streamable streamable) {
        User currentUser = userService.getCurrentUser();
        jobService.execute(() -> {
            parseText(currentUser, streamable.getText(), streamable, null);
        });
    }

    private void parseText(User currentUser, String text, Streamable streamable, Comment comment) {
        try {
            RunAsUser.set(currentUser);

            Matcher mentionMatcher = MENTION_PATTERN.matcher(text);
            Matcher hashtagMatcher = HASH_PATTERN.matcher(text);

            // Parse for mentions.
            while(mentionMatcher.find()) {
                transactionUtils.executeInTransaction(() -> {

                    String match = mentionMatcher.group(1);
                    User foundUser = userService.findByUsername(match);

                    if (foundUser != null) {
                        if (!foundUser.equals(currentUser)) { // User can mention himself without notifications.

                            if (comment != null) {
                                notificationService.addMentionNotification(foundUser, currentUser, streamable, comment, true);
                            } else {
                                notificationService.addMentionNotification(foundUser, currentUser, streamable, true);
                            }
                        }
                    } else if (log.isDebugEnabled()) {
                        log.debug("Non-existing user found '" + match + "'");
                    }
                });
            }

            // Parse for hashtags.
            while (hashtagMatcher.find()) {
                transactionUtils.executeInTransaction(() -> {
                    String match = hashtagMatcher.group(1);

                    if (!streamableService.hashtagExistsForStreamable(match, streamable)) {
                        Streamable inner = streamableService.findStreamableById(streamable.getId());
                        inner.getHashtags().add(match);
                    }
                });
            }

            if (log.isDebugEnabled()) {
                log.debug("Finished parsing text");
            }
        } catch (Throwable t) {
            log.error("Error parsing text", t);
        } finally {
            RunAsUser.clear();
        }
    }

    public void validateText(String text) {
        if (StringUtils.hasText(text)) {
            if (text.length() > 5000) {
                throw new RestApiException(ResultCode.TEXT_TOO_LONG, "Text sent is too long");
            }
        }
    }
}
