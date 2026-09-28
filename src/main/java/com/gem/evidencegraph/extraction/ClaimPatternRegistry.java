package com.gem.evidencegraph.extraction;

import java.util.regex.Pattern;

public final class ClaimPatternRegistry {

    private ClaimPatternRegistry() {

    }

    public static final Pattern PAN_PATTERN =
            Pattern.compile("\\b([A-Z]{5}[0-9]{4}[A-Z])\\b");

    public static final Pattern GSTIN_PATTERN =
            Pattern.compile("\\b([0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z])\\b");

    public static final Pattern UDYAM_PATTERN =
            Pattern.compile("\\b(UDYAM[ -]?[A-Z]{2}[ -]?[0-9]{2}[ -]?[0-9]{7})\\b", Pattern.CASE_INSENSITIVE);

    public static final Pattern LEGAL_NAME_LABEL =
            Pattern.compile("(?i)(?:Legal Name|Name of Enterprise|Name of Firm|Name of the Entity|Company Name|M/s\\.?)\\s*[:\\-]\\s*([A-Za-z0-9\\s.,&'()\\-]{3,120})(?=\\n|\\r|$|;)");

    public static final Pattern ADDRESS_LABEL =
            Pattern.compile("(?i)(?:Registered Address|Principal Place of Business|Business Address|Address)\\s*[:\\-]\\s*([A-Za-z0-9\\s.,&'()\\-#/]{5,250}?)(?=\\n|\\r|$|;)");

    public static final Pattern REGISTRATION_DATE_LABEL =
            Pattern.compile("(?i)(?:Registration Date|Date of Registration|Date of Incorporation|Date of Issue)\\s*[:\\-]\\s*([0-9]{1,2}[/\\-.][0-9]{1,2}[/\\-.][0-9]{2,4}|[0-9]{4}[/\\-.][0-9]{1,2}[/\\-.][0-9]{1,2})");

    public static final Pattern EXPIRY_DATE_LABEL =
            Pattern.compile("(?i)(?:Expiry Date|Date of Expiry|Valid Upto|Valid To|Validity Date)\\s*[:\\-]\\s*([0-9]{1,2}[/\\-.][0-9]{1,2}[/\\-.][0-9]{2,4}|[0-9]{4}[/\\-.][0-9]{1,2}[/\\-.][0-9]{1,2})");

    public static final Pattern CERTIFICATE_NUMBER_LABEL =
            Pattern.compile("(?i)(?:Certificate No\\.?|Certificate Number|Registration No\\.?|License No\\.?)\\s*[:\\-]\\s*([A-Za-z0-9\\-_/]{4,50})");

}
