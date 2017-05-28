package com.graffitab.server.config.web;


import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
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

            Locale currentLocale;

            if (request.getParameter("lang") != null) {
                String currentLanguage = request.getParameter("lang");
                currentLocale = Locale.forLanguageTag(currentLanguage);
            } else {
                // This is the value of the Accept-Language header
                String acceptHeaderValue = request.getHeader("Accept-Language");


                if (!StringUtils.hasText(acceptHeaderValue)) {
                    log.warn("No locale found for this request in Accept-Language Header -- falling back to English");
                    return Locale.ENGLISH;
                }

                String lang = acceptHeaderValue;
                if (acceptHeaderValue.contains("-")) {
                    lang = acceptHeaderValue.split("-")[0];
                } else if (acceptHeaderValue.contains("_")) {
                    lang = acceptHeaderValue.split("_")[0];
                }

                currentLocale = new Locale(lang);
            }

            if (supportedLanguagesList.contains(currentLocale.getLanguage())) {
                return currentLocale;
            } else {
                log.warn("The requested language " + currentLocale.getLanguage() + " is not supported -- falling back to English");
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
