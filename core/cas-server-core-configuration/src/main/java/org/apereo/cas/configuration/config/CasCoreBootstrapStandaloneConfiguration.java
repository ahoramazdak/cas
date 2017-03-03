package org.apereo.cas.configuration.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;

import java.io.File;
import java.io.FileReader;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

/**
 * This is {@link CasCoreBootstrapStandaloneConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 5.1.0
 */
@Profile("standalone")
@ConditionalOnProperty(value = "spring.cloud.config.enabled", havingValue = "false")
@Configuration("casStandaloneBootstrapConfiguration")

public class CasCoreBootstrapStandaloneConfiguration implements PropertySourceLocator {
    private static final Logger LOGGER = LoggerFactory.getLogger(CasCoreBootstrapStandaloneConfiguration.class);

    @Override
    public PropertySource<?> locate(final Environment environment) {
        final Properties props = new Properties();
        
        final File config = CasConfigurationProperties.getStandaloneProfileConfigurationDirectory(environment);
        LOGGER.debug("Located CAS standalone configuration directory at [{}]", config);

        if (config.isDirectory() && config.exists()) {
            final Collection<File> configFiles = FileUtils.listFiles(config,
                    new RegexFileFilter("(cas|application)\\.(yml|properties)", IOCase.INSENSITIVE), TrueFileFilter.INSTANCE);

            LOGGER.debug("Configuration files found at [{}] are [{}]", config, configFiles);
            final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            configFiles.forEach(Unchecked.consumer(f -> {
                LOGGER.debug("Loading configuration file [{}]", f);
                if (f.getName().toLowerCase().endsWith("yml")) {
                    final Map pp = mapper.readValue(f, Map.class);
                    props.putAll(pp);
                } else {
                    final Properties pp = new Properties();
                    pp.load(new FileReader(f));
                    props.putAll(pp);
                }
            }));
        } else {
            LOGGER.warn("Configuration directory [{}] is not a directory or cannot be found at the specific path", config);
        }
        LOGGER.info("Found and loaded [{}] setting(s) from [{}] in standalone mode", props.size(), config);
        return new PropertiesPropertySource("standaloneCasConfigService", props);
    }
}
