package com.example.socialmedia.util;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

public class HtmlSanitizerUtil {

    private static final PolicyFactory POLICY = new HtmlPolicyBuilder()
            .allowElements("b", "i", "u", "em", "strong", "a", "p", "br")
            .allowUrlProtocols("http", "https")
            .allowAttributes("href").onElements("a")
            .requireRelNofollowOnLinks()
            .toFactory();

    private static final PolicyFactory STRICT_TEXT_ONLY_POLICY = new HtmlPolicyBuilder()
            .toFactory();

    public static String sanitize(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }
        return POLICY.sanitize(input);
    }

    public static String sanitizeTextOnly(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }
        return STRICT_TEXT_ONLY_POLICY.sanitize(input);
    }
}
