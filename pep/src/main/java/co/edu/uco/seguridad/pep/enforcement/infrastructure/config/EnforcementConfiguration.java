package co.edu.uco.seguridad.pep.enforcement.infrastructure.config;

import co.edu.uco.seguridad.pep.enforcement.application.port.secondary.DecisionPort;
import co.edu.uco.seguridad.pep.enforcement.application.rule.EnforceAccessRequestMustBeCompleteRule;
import co.edu.uco.seguridad.pep.enforcement.application.rule.impl.EnforceAccessRequestMustBeCompleteRuleImpl;
import co.edu.uco.seguridad.pep.enforcement.application.rulesvalidator.EnforceAccessRulesValidator;
import co.edu.uco.seguridad.pep.enforcement.application.rulesvalidator.impl.EnforceAccessRulesValidatorImpl;
import co.edu.uco.seguridad.pep.enforcement.application.usecase.EnforceAccessUseCase;
import co.edu.uco.seguridad.pep.enforcement.application.usecase.impl.EnforceAccessUseCaseImpl;
import co.edu.uco.seguridad.pep.enforcement.infrastructure.adapter.secondary.http.WebClientDecisionAdapter;
import co.edu.uco.seguridad.pep.enforcement.infrastructure.properties.PdpClientProperties;
import co.edu.uco.seguridad.pep.enforcement.infrastructure.properties.ProxyProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.netty.handler.ssl.SslContextBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ProxyProperties.class, PdpClientProperties.class})
class EnforcementConfiguration {

    @Bean
    WebClient pdpWebClient(PdpClientProperties properties, WebClient.Builder builder) throws javax.net.ssl.SSLException {
        HttpClient transport = HttpClient.create().followRedirect(false)
                .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(properties.connectTimeout().toMillis()))
                .responseTimeout(properties.timeout());
        if ("https".equals(properties.baseUrl().getScheme())) {
            SslContextBuilder tls = SslContextBuilder.forClient();
            if (properties.caCertificate() != null) tls.trustManager(properties.caCertificate().toFile());
            if (properties.clientCertificate() != null)
                tls.keyManager(properties.clientCertificate().toFile(), properties.clientKey().toFile());
            var context = tls.build();
            transport = transport.secure(spec -> spec.sslContext(context));
        }
        return builder.clone().baseUrl(properties.baseUrl().toString())
                .clientConnector(new ReactorClientHttpConnector(transport))
                .codecs(codecs -> {
                    codecs.defaultCodecs().maxInMemorySize(properties.maxResponseBytes());
                    codecs.defaultCodecs().jacksonJsonDecoder(new org.springframework.http.codec.json.JacksonJsonDecoder(
                            tools.jackson.databind.json.JsonMapper.builder()
                                    .enable(tools.jackson.core.StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                                    .disable(tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build()));
                }).build();
    }

    @Bean
    DecisionPort decisionPort(WebClient pdpWebClient, PdpClientProperties properties, MeterRegistry metrics) {
        return new WebClientDecisionAdapter(pdpWebClient, properties, metrics);
    }

    @Bean
    EnforceAccessRequestMustBeCompleteRule enforceAccessRequestMustBeCompleteRule() {
        return new EnforceAccessRequestMustBeCompleteRuleImpl();
    }

    @Bean
    EnforceAccessRulesValidator enforceAccessRulesValidator(EnforceAccessRequestMustBeCompleteRule rule) {
        return new EnforceAccessRulesValidatorImpl(rule);
    }

    @Bean
    EnforceAccessUseCase enforceAccessUseCase(DecisionPort port, EnforceAccessRulesValidator rules) {
        return new EnforceAccessUseCaseImpl(port, rules);
    }

}
