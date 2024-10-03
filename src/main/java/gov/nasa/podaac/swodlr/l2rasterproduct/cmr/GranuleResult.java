package gov.nasa.podaac.swodlr.l2rasterproduct.cmr;

public record GranuleResult(
  int cycle,
  int pass,
  int scene,
  String crid
) { }
