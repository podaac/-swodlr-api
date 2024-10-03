package gov.nasa.podaac.swodlr;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties("swodlr")
@ConstructorBinding
public record SwodlrProperties(
    Map<String, String> teaMapping,
    String productCreateQueueUrl,
    String availableTilesTableName,
    String cmrGraphqlEndpoint,
    String rasterCollectionConceptId
) { }
