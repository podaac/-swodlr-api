package gov.nasa.podaac.swodlr.l2rasterproduct;

import gov.nasa.podaac.swodlr.rasterdefinition.GridType;
import gov.nasa.podaac.swodlr.user.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface L2RasterProductQuery {
  List<L2RasterProduct> findByUser(
      User user,
      Integer cycle,
      Integer pass,
      Integer scene,
      Boolean outputGranuleExtentFlag,
      GridType outputSamplingGridType,
      Integer rasterResolution,
      Integer utmZoneAdjust,
      Integer mgrsBandAdjust,
      LocalDateTime beforeTimestamp,
      LocalDateTime afterTimestamp,
      UUID after,
      int limit
  );

  List<L2RasterProduct> findByParameters(
      Integer cycle,
      Integer pass,
      Integer scene,
      Boolean outputGranuleExtentFlag,
      GridType outputSamplingGridType,
      Integer rasterResolution,
      Integer utmZoneAdjust,
      Integer mgrsBandAdjust,
      UUID after,
      int limit
  );
}
