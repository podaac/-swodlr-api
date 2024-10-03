package gov.nasa.podaac.swodlr.l2rasterproduct.cmr;

import java.util.Map;

public class CmrGraphqlRequest {
  private static final String QUERY =
  """
  query($params: GranulesInput) {
    granules(params: $params) {
      items {
        granuleUr
      }
    }
  }
  """;

  private final Map<String, Object> variables;

  public CmrGraphqlRequest(String collectionConceptId, String conceptId) {
    variables = Map.of("params", Map.of(
      "conceptId", conceptId,
      "collectionConceptId", collectionConceptId
    ));
  }

  public final String getQuery() {
    return QUERY;
  }

  public final Map<String, Object> getVariables() {
    return variables;
  }
}
