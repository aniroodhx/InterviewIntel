package com.interviewintel.service;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Normalizes raw company names and role titles into canonical search keys.
 *
 * "Amex"  → "american express"
 * "SDE1"  → "software engineer 1"
 * "FAANG" → handled separately
 */
@Component
public class QueryNormalizer {

    //Company alias map
    private static final Map<String, String> COMPANY_ALIASES = new HashMap<>();
    static {
        COMPANY_ALIASES.put("amex", "american express");
        COMPANY_ALIASES.put("americanexpress", "american express");
        COMPANY_ALIASES.put("ms", "microsoft");
        COMPANY_ALIASES.put("msft", "microsoft");
        COMPANY_ALIASES.put("goog", "google");
        COMPANY_ALIASES.put("googl", "google");
        COMPANY_ALIASES.put("fb", "meta");
        COMPANY_ALIASES.put("facebook", "meta");
        COMPANY_ALIASES.put("amzn", "amazon");
        COMPANY_ALIASES.put("aws", "amazon");
        COMPANY_ALIASES.put("aapl", "apple");
        COMPANY_ALIASES.put("nflx", "netflix");
        COMPANY_ALIASES.put("uber", "uber");
        COMPANY_ALIASES.put("lyft", "lyft");
        COMPANY_ALIASES.put("jpm", "jpmorgan");
        COMPANY_ALIASES.put("jpmorgan chase", "jpmorgan");
        COMPANY_ALIASES.put("bofa", "bank of america");
        COMPANY_ALIASES.put("bny", "bny mellon");
        COMPANY_ALIASES.put("cts", "cognizant");
        COMPANY_ALIASES.put("tcs", "tata consultancy services");
        COMPANY_ALIASES.put("infy", "infosys");
        COMPANY_ALIASES.put("wipro", "wipro");
        COMPANY_ALIASES.put("hcl", "hcl technologies");
        COMPANY_ALIASES.put("qcom", "qualcomm");
        COMPANY_ALIASES.put("nvda", "nvidia");
        COMPANY_ALIASES.put("intc", "intel");
    }

    //Role alias map
    private static final Map<String, String> ROLE_ALIASES = new HashMap<>();
    static {
        ROLE_ALIASES.put("sde1", "software engineer 1");
        ROLE_ALIASES.put("sde 1", "software engineer 1");
        ROLE_ALIASES.put("sde-1", "software engineer 1");
        ROLE_ALIASES.put("sde2", "software engineer 2");
        ROLE_ALIASES.put("sde 2", "software engineer 2");
        ROLE_ALIASES.put("sde-2", "software engineer 2");
        ROLE_ALIASES.put("sde3", "software engineer 3");
        ROLE_ALIASES.put("swe", "software engineer");
        ROLE_ALIASES.put("swe1", "software engineer 1");
        ROLE_ALIASES.put("swe2", "software engineer 2");
        ROLE_ALIASES.put("se1", "software engineer 1");
        ROLE_ALIASES.put("se2", "software engineer 2");
        ROLE_ALIASES.put("mts1", "member of technical staff 1");
        ROLE_ALIASES.put("mts2", "member of technical staff 2");
        ROLE_ALIASES.put("sdet", "software development engineer in test");
        ROLE_ALIASES.put("qa", "quality assurance engineer");
        ROLE_ALIASES.put("ml engineer", "machine learning engineer");
        ROLE_ALIASES.put("mle", "machine learning engineer");
        ROLE_ALIASES.put("ds", "data scientist");
        ROLE_ALIASES.put("de", "data engineer");
        ROLE_ALIASES.put("pm", "product manager");
        ROLE_ALIASES.put("tpm", "technical program manager");
        ROLE_ALIASES.put("em", "engineering manager");
        ROLE_ALIASES.put("ios", "ios engineer");
        ROLE_ALIASES.put("android", "android engineer");
        ROLE_ALIASES.put("fe", "frontend engineer");
        ROLE_ALIASES.put("be", "backend engineer");
        ROLE_ALIASES.put("fullstack", "full stack engineer");
        ROLE_ALIASES.put("full stack", "full stack engineer");
    }

    private static final Pattern SPACES = Pattern.compile("\\s+");

    public String normalizeCompany(String raw) {
        if (raw == null) return "";
        String cleaned = SPACES.matcher(raw.trim().toLowerCase()).replaceAll(" ");
        String noSpaces = cleaned.replace(" ", "");
        // Check both forms
        if (COMPANY_ALIASES.containsKey(noSpaces)) return COMPANY_ALIASES.get(noSpaces);
        if (COMPANY_ALIASES.containsKey(cleaned)) return COMPANY_ALIASES.get(cleaned);
        return cleaned;
    }

    public String normalizeRole(String raw) {
        if (raw == null) return "";
        String cleaned = SPACES.matcher(raw.trim().toLowerCase()).replaceAll(" ");
        String noSpaces = cleaned.replace(" ", "");
        if (ROLE_ALIASES.containsKey(noSpaces)) return ROLE_ALIASES.get(noSpaces);
        if (ROLE_ALIASES.containsKey(cleaned)) return ROLE_ALIASES.get(cleaned);
        return cleaned;
    }

    public String buildCacheKey(String company, String role, String location) {
        return normalizeCompany(company) + "::" + normalizeRole(role) + "::" + (location != null ? location.toUpperCase() : "ALL");
    }

    /**
     * Returns display-friendly canonical name.
     * e.g. "american express" → "American Express"
     */
    public String toDisplayName(String normalized) {
        if (normalized == null || normalized.isBlank()) return "";
        String[] words = normalized.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1))
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }
}
