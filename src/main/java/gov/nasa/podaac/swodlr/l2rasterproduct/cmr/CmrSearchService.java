package gov.nasa.podaac.swodlr.l2rasterproduct.cmr;

import gov.nasa.podaac.swodlr.SwodlrProperties;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class CmrSearchService {
  private Logger logger = LoggerFactory.getLogger(getClass());
  private Pattern granuleUrRe = Pattern.compile("SWOT_L2_HR_Raster_\\d{3}m_\\w+?_(?<cycle>\\d{3})_(?<pass>\\d{3})_(?<scene>\\d{3})F_\\d+T\\d+_\\d+T\\d+_(?<crid>\\w+?)_\\d{2}");

  @Autowired
  private SwodlrProperties properties;

  public Mono<GranuleResult> findL2RasterProductById(String id) {
    return getUserToken()
      .flatMap((token) -> {
        return Mono.defer(() -> {
          return WebClient.create()
            .post()
            .uri(URI.create(properties.cmrGraphqlEndpoint()))
            .header("Authorization", "Bearer " + token)
            .bodyValue(new CmrGraphqlRequest(
              properties.rasterCollectionConceptId(),
              id
            ))
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
        });
      })
      .map((response) -> {
        if (response.get("errors") != null) {
          for (var error : (List<Object>) response.get("errors")) {
            logger.error("GraphQL query error: %s", error.toString());
          }

          throw new RuntimeException("GraphQL search returned an error");
        }

        if (response.get("data") == null) {
          throw new RuntimeException("No data found in CMR response");
        }

        var data = (Map<String, Object>) response.get("data");
        var granules = (Map<String, Object>) data.get("granules");
        var items = (List<Map<String, Object>>) granules.get("items");

        if (items.size() != 1) {
          return null;
        }

        var result = items.get(0);
        var granuleUr = (String) result.get("granuleUr");
        var matcher = granuleUrRe.matcher(granuleUr);
        
        if (!matcher.matches()) {
          throw new RuntimeException("Regex failed to match granuleUr");
        }

        return new GranuleResult(
          Integer.parseInt(matcher.group("cycle")),
          Integer.parseInt(matcher.group("pass")),
          Integer.parseInt(matcher.group("scene")),
          matcher.group("crid")
        );
      });
  }

  private Mono<String> getUserToken() {
    return ReactiveSecurityContextHolder
      .getContext()
      .map((securityContext) -> securityContext.getAuthentication())
      .filter((authentication) -> authentication != null)
      .cast(JwtAuthenticationToken.class)
      .map((jwt) -> jwt.getToken().getTokenValue());
  }
}
