package gov.nasa.podaac.swodlr.l2rasterproduct;

import gov.nasa.podaac.swodlr.rasterdefinition.GridType;
import gov.nasa.podaac.swodlr.user.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hibernate.type.BooleanType;
import org.hibernate.type.IntegerType;
import org.hibernate.type.LocalDateTimeType;
import org.hibernate.type.StringType;
import org.hibernate.type.UUIDCharType;

public class L2RasterProductQueryImpl implements L2RasterProductQuery {
  @PersistenceContext
  private EntityManager entityManager;

  /*
   * This implementation is utilized to workaround an JPA issue with the
   * PostgreSQL dialect in Hibernate where Hibernate does not parameterize
   * null values as their original datatype and instead parameterizes as
   * bytea's. This is a known issue of the PostgreSQL driver/dialect
   * 
   * This can lead to the exceptions:
   *    - "ERROR: could not determine data type of parameter $1"
   *    - "ERROR: operator does not exist: uuid = bytea"
   * 
   * Solutions researched include casting values, however, by casting
   * in the query, we loose the benefits of parameterization in our
   * prepared statements and create a statement which is harder to read.
   * Workarounds such as COALESCE and testing other PostgreSQL dialects
   * available in Hibernate were tried, but were found to either not
   * solve the issue or still require the use of CASTs in queries
   * 
   * The hope is to one day remove this code in favor of Spring Data JPA
   * queries, pending the PostgreSQL/Hibernate teams' cooperation with
   * one another
   * 
   * Relevant discussions:
   *    - https://stackoverflow.com/a/64223435
   *    - https://stackoverflow.com/a/62680643
   *    - https://github.com/pgjdbc/pgjdbc/issues/247#issuecomment-78213991
   */
  @Override
  public List<L2RasterProduct> findByUser(
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
  ) {
    @SuppressWarnings("LineLength")
    String statement =
        """
        SELECT \"L2RasterProducts\".* FROM \"L2RasterProducts\"
        JOIN \"ProductHistory\" ON \"ProductHistory\".\"rasterProductId\" = \"L2RasterProducts\".id
        WHERE
          (:cycle is NULL OR \"cycle\" = :cycle) AND
          (:pass is NULL OR \"pass\" = :pass) AND
          (:scene is NULL OR \"scene\" = :scene) AND
          (:outputGranuleExtentFlag is NULL OR \"outputGranuleExtentFlag\" = :outputGranuleExtentFlag) AND
          (:outputSamplingGridType is NULL OR \"outputSamplingGridType\" = :outputSamplingGridType) AND
          (:rasterResolution is NULL OR \"rasterResolution\" = :rasterResolution) AND
          (:utmZoneAdjust is NULL OR \"utmZoneAdjust\" = :utmZoneAdjust) AND
          (:mgrsBandAdjust is NULL OR \"mgrsBandAdjust\" = :mgrsBandAdjust) AND
          (CAST(:beforeTimestamp as TIMESTAMP) is NULL OR \"ProductHistory\".timestamp <= :beforeTimestamp) AND
          (CAST(:afterTimestamp as TIMESTAMP) is NULL OR \"ProductHistory\".timestamp >= :afterTimestamp) AND
          (\"ProductHistory\".\"requestedById\" = CAST(:userId as UUID)) AND
          (
            (:after is NULL)
            OR
            (\"ProductHistory\".timestamp, \"ProductHistory\".\"rasterProductId\") < (SELECT timestamp, \"rasterProductId\" FROM \"ProductHistory\" WHERE \"requestedById\" = CAST(:userId as UUID) AND \"rasterProductId\" = CAST(:after as UUID))
          )
          ORDER BY \"ProductHistory\".timestamp DESC, \"ProductHistory\".\"rasterProductId\" DESC LIMIT :limit
        """;

    Session session = entityManager.unwrap(Session.class);
    Query<L2RasterProduct> query = session.createNativeQuery(statement, L2RasterProduct.class);
    query.setParameter("userId", user.getId(), UUIDCharType.INSTANCE);
    query.setParameter("cycle", cycle, IntegerType.INSTANCE);
    query.setParameter("pass", pass, IntegerType.INSTANCE);
    query.setParameter("scene", scene, IntegerType.INSTANCE);
    query.setParameter("outputGranuleExtentFlag", outputGranuleExtentFlag, BooleanType.INSTANCE);
    query.setParameter("outputSamplingGridType", outputSamplingGridType != null
        ? outputSamplingGridType.toString() : null, StringType.INSTANCE);
    query.setParameter("rasterResolution", rasterResolution, IntegerType.INSTANCE);
    query.setParameter("utmZoneAdjust", utmZoneAdjust, IntegerType.INSTANCE);
    query.setParameter("mgrsBandAdjust", mgrsBandAdjust, IntegerType.INSTANCE);
    query.setParameter("beforeTimestamp", beforeTimestamp, LocalDateTimeType.INSTANCE);
    query.setParameter("afterTimestamp", afterTimestamp, LocalDateTimeType.INSTANCE);
    query.setParameter("after", after, UUIDCharType.INSTANCE);
    query.setParameter("limit", limit, IntegerType.INSTANCE);

    return query.getResultList();
  }

  @Override
  public List<L2RasterProduct> findByParameters(
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
    ) {
      @SuppressWarnings("LineLength")
      String statement =
        """
        SELECT \"L2RasterProducts\".* FROM \"L2RasterProducts\"
        WHERE
          (:cycle is NULL OR \"cycle\" = :cycle) AND
          (:pass is NULL OR \"pass\" = :pass) AND
          (:scene is NULL OR \"scene\" = :scene) AND
          (:outputGranuleExtentFlag is NULL OR \"outputGranuleExtentFlag\" = :outputGranuleExtentFlag) AND
          (:outputSamplingGridType is NULL OR \"outputSamplingGridType\" = :outputSamplingGridType) AND
          (:rasterResolution is NULL OR \"rasterResolution\" = :rasterResolution) AND
          (:utmZoneAdjust is NULL OR \"utmZoneAdjust\" = :utmZoneAdjust) AND
          (:mgrsBandAdjust is NULL OR \"mgrsBandAdjust\" = :mgrsBandAdjust) AND
          (:after is NULL OR \"id\" > CAST(:after as UUID))
          ORDER BY id DESC LIMIT :limit
        """;

    Session session = entityManager.unwrap(Session.class);
    Query<L2RasterProduct> query = session.createNativeQuery(statement, L2RasterProduct.class);
    query.setParameter("cycle", cycle, IntegerType.INSTANCE);
    query.setParameter("pass", pass, IntegerType.INSTANCE);
    query.setParameter("scene", scene, IntegerType.INSTANCE);
    query.setParameter("outputGranuleExtentFlag", outputGranuleExtentFlag, BooleanType.INSTANCE);
    query.setParameter("outputSamplingGridType", outputSamplingGridType != null
        ? outputSamplingGridType.toString() : null, StringType.INSTANCE);
    query.setParameter("rasterResolution", rasterResolution, IntegerType.INSTANCE);
    query.setParameter("utmZoneAdjust", utmZoneAdjust, IntegerType.INSTANCE);
    query.setParameter("mgrsBandAdjust", mgrsBandAdjust, IntegerType.INSTANCE);
    query.setParameter("after", after, UUIDCharType.INSTANCE);
    query.setParameter("limit", limit, IntegerType.INSTANCE);

    return query.getResultList();
  }
}
