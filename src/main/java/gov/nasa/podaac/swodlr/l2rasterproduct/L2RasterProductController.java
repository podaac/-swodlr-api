package gov.nasa.podaac.swodlr.l2rasterproduct;

import gov.nasa.podaac.swodlr.exception.SwodlrException;
import gov.nasa.podaac.swodlr.l2rasterproduct.cmr.CmrSearchService;
import gov.nasa.podaac.swodlr.rasterdefinition.GridType;
import gov.nasa.podaac.swodlr.status.State;
import gov.nasa.podaac.swodlr.status.Status;
import gov.nasa.podaac.swodlr.status.StatusRepository;
import gov.nasa.podaac.swodlr.user.User;
import gov.nasa.podaac.swodlr.user.UserReference;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Controller
public class L2RasterProductController {
  private Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  private CmrSearchService cmrSearchService;

  @Autowired
  private L2RasterProductService l2RasterProductService;

  @Autowired
  private L2RasterProductRepository l2RasterProductRepository;

  @Autowired
  private StatusRepository statusRepository;

  @MutationMapping
  public Mono<L2RasterProduct> generateL2RasterProduct(
      @ContextValue UserReference userRef,
      @Argument int cycle,
      @Argument int scene,
      @Argument int pass,
      @Argument boolean outputGranuleExtentFlag,
      @Argument @NotNull GridType outputSamplingGridType,
      @Argument int rasterResolution,
      @Argument Integer utmZoneAdjust,
      @Argument Integer mgrsBandAdjust
  ) {
    return Mono.defer(() -> {
      User user = userRef.fetch();

      return l2RasterProductService.getL2RasterProduct(
        user,
        cycle,
        pass,
        scene,
        outputGranuleExtentFlag,
        outputSamplingGridType,
        rasterResolution,
        utmZoneAdjust,
        mgrsBandAdjust
      )
        .switchIfEmpty(Mono.defer(() -> l2RasterProductService.createL2RasterProduct(
          user,
          cycle,
          pass,
          scene,
          outputGranuleExtentFlag,
          outputSamplingGridType,
          rasterResolution,
          utmZoneAdjust,
          mgrsBandAdjust
        )));
    });
  }

  @MutationMapping
  public Mono<L2RasterProduct> generateL2RasterProductByConceptId(
      @ContextValue UserReference userRef,
      @Argument String conceptId,
      @Argument boolean outputGranuleExtentFlag,
      @Argument @NotNull GridType outputSamplingGridType,
      @Argument int rasterResolution,
      @Argument Integer utmZoneAdjust,
      @Argument Integer mgrsBandAdjust
  ) {
    return Mono.defer(() -> {
      User user = userRef.fetch();

      return cmrSearchService
        .findL2RasterProductById(conceptId)
        .flatMap((result) -> {
          return l2RasterProductService
            .getL2RasterProduct(
              user,
              result.cycle(),
              result.pass(),
              result.scene(),
              outputGranuleExtentFlag,
              outputSamplingGridType,
              rasterResolution,
              utmZoneAdjust,
              mgrsBandAdjust
            )
            .switchIfEmpty(
              Mono.defer(() -> l2RasterProductService.createL2RasterProduct(
                user,
                result.cycle(),
                result.pass(),
                result.scene(),
                outputGranuleExtentFlag,
                outputSamplingGridType,
                rasterResolution,
                utmZoneAdjust,
                mgrsBandAdjust
              ))
            );
        });
    });
  }

  @PreAuthorize("hasRole(\"ROLE_Administrator\")")
  @MutationMapping
  @Transactional
  public L2RasterProduct invalidateProduct(@Argument UUID id) {
    var result = l2RasterProductRepository.findById(id);
    if (result.isEmpty()) {
      logger.debug("No products found with id: {}", id.toString());
      return null;
    }

    L2RasterProduct product = result.get();
    Status invalidatedStatus = new Status(product, State.UNAVAILABLE);
    statusRepository.save(invalidatedStatus);

    return product;
  }

  @QueryMapping
  public L2RasterProduct l2RasterProduct(@Argument UUID id) {
    var result = l2RasterProductRepository.findById(id);
    if (result.isPresent()) {
      return result.get();
    }

    return null;
  }

  @SchemaMapping(typeName = "Status", field = "product")
  public L2RasterProduct getProductForStatus(Status status) {
    return status.getProduct();
  }

  @SchemaMapping(typeName = "User", field = "products")
  public List<L2RasterProduct> getProductsForUser(
      @ContextValue UserReference userRef,
      @Argument Integer cycle,
      @Argument Integer pass,
      @Argument Integer scene,
      @Argument Boolean outputGranuleExtentFlag,
      @Argument GridType outputSamplingGridType,
      @Argument Integer rasterResolution,
      @Argument Integer utmZoneAdjust,
      @Argument Integer mgrsBandAdjust,
      @Argument String beforeTimestamp,
      @Argument String afterTimestamp,
      @Argument UUID after,
      @Argument int limit
  ) {
    LocalDateTime beforeDate = null;
    LocalDateTime afterDate = null;

    if (beforeTimestamp != null) {
      try {
        beforeDate = LocalDateTime.parse(beforeTimestamp);
      } catch (DateTimeParseException ex) {
        throw new SwodlrException("Invalid \'beforeTimestamp\' - should be ISO8601");
      }
    }

    if (afterTimestamp != null) {
      try {
        afterDate = LocalDateTime.parse(afterTimestamp);
      } catch (DateTimeException ex) {
        throw new SwodlrException("Invalid \'afterTimestamp\' - should be ISO8601");
      }
    }

    return l2RasterProductRepository.findByUser(
        userRef.fetch(),
        cycle,
        pass,
        scene,
        outputGranuleExtentFlag,
        outputSamplingGridType,
        rasterResolution,
        utmZoneAdjust,
        mgrsBandAdjust,
        beforeDate,
        afterDate,
        after,
        limit
    );
  }
}
