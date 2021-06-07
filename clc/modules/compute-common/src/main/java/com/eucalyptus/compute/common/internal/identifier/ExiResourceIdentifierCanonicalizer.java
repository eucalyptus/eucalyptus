package com.eucalyptus.compute.common.internal.identifier;

import java.util.Map;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;

/**
 * Canonicalizer for A?I -> E?I conversions.
 */
public class ExiResourceIdentifierCanonicalizer implements ResourceIdentifierCanonicalizer {

  private static final Map<String,String> prefixMap = ImmutableMap.of(
      "aki", "eki",
      "ami", "emi",
      "ari", "eri"
  );

  @Override
  public String getName() {
    return "exi";
  }

  @Override
  public String canonicalizePrefix( final String prefix ) {
    return MoreObjects.firstNonNull( prefixMap.get( prefix ), prefix );
  }

  @Override
  public String canonicalizeHex( final String hex ) {
    return hex;
  }
}
