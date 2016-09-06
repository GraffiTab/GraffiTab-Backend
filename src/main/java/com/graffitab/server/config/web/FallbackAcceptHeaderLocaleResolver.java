package com.graffitab.server.config.web;


import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Created by david on 06/09/2016.
 */
@Log4j2
public class FallbackAcceptHeaderLocaleResolver extends AcceptHeaderLocaleResolver {

    @Value("${i18n.supportedLanguages:en}")
    private String supportedLanguages;

    @Override
    public Locale resolveLocale(HttpServletRequest request) {

        if (supportedLanguages != null) {

            String[] languages = supportedLanguages.split(",");
            List<String> supportedLanguagesList = Arrays.asList(languages);
            Locale requestLocale = request.getLocale();

            String currentLanguage = requestLocale.getLanguage();
            if (supportedLanguagesList.contains(currentLanguage)) {
                return requestLocale;
            } else {
                log.warn("The requested language " + currentLanguage + " is not supported -- falling back to English");
                return Locale.ENGLISH;
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Supported languages property not present, falling back to English locale");

            }
            return Locale.ENGLISH;
        }
    }
}
