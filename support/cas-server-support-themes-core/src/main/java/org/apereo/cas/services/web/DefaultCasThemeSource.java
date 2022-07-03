package org.apereo.cas.services.web;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.util.HttpRequestUtils;
import org.apereo.cas.util.ResourceUtils;
import org.apereo.cas.web.view.CasReloadableMessageBundle;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.lambda.Unchecked;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.ui.context.Theme;
import org.springframework.ui.context.support.ResourceBundleThemeSource;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * This is {@link DefaultCasThemeSource}.
 *
 * @author Misagh Moayyed
 * @since 6.6.0
 */
@RequiredArgsConstructor
@Slf4j
public class DefaultCasThemeSource extends ResourceBundleThemeSource {
    private final CasConfigurationProperties casProperties;

    @Override
    protected MessageSource createMessageSource(
        @Nonnull
        final String basename) {
        return casProperties.getView().getTemplatePrefixes()
            .stream()
            .map(prefix -> StringUtils.appendIfMissing(prefix, "/").concat(basename).concat(".properties"))
            .filter(ResourceUtils::doesResourceExist)
            .findFirst()
            .map(Unchecked.function(path -> {
                try (val is = ResourceUtils.getRawResourceFrom(path).getInputStream()) {
                    val source = new StaticMessageSource();
                    val properties = new Properties();
                    properties.load(is);
                    properties.forEach((key, value) -> {
                        LOGGER.trace("Loading theme property [{}] from [{}]", key, path);
                        source.addMessage(key.toString(), Locale.getDefault(), value.toString());
                    });
                    return source;
                }
            }))
            .map(MessageSource.class::cast)
            .orElseGet(() -> super.createMessageSource(basename));
    }
}
