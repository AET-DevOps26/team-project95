package com.project95.thesis.thesis.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;

public final class HtmlNormalizer {

  private HtmlNormalizer() {
    // Private constructor to prevent instantiation
  }

  /**
   * Sanitizes the HTML to keep only semantic text structures and elements,
   * while removing styles, classes, scripts, header/footer/nav layout elements.
   * Also converts hyperlinks to inline text format so URL values are preserved.
   */
  public static String sanitizeHtml(String rawHtml) {
    return sanitizeHtml(rawHtml, null);
  }

  public static String sanitizeHtml(String rawHtml, String baseUrl) {
    if (rawHtml == null || rawHtml.isBlank()) {
      return "";
    }

    Document doc = baseUrl != null ? Jsoup.parse(rawHtml, baseUrl) : Jsoup.parse(rawHtml);
    
    // Remove layout and noise tags
    doc.select("script, style, iframe, noscript, svg, picture, header, footer, nav, aside, form").remove();
    
    // Convert hyperlink tags so URL metadata survives plain text extraction
    preserveHyperlinks(doc);
    
    // Retain only semantic structure
    Safelist safelist = Safelist.relaxed()
        .addAttributes("a", "href")
        .removeProtocols("a", "href", "ftp", "http", "https", "mailto");

    return baseUrl != null ? Jsoup.clean(doc.body().html(), baseUrl, safelist) : Jsoup.clean(doc.body().html(), safelist);
  }

  /**
   * Extract normalized plain text from sanitized HTML for hashing.
   */
  public static String getNormalizedText(String sanitizedHtml) {
    if (sanitizedHtml == null || sanitizedHtml.isBlank()) {
      return "";
    }
    Document doc = Jsoup.parse(sanitizedHtml);
    String text = doc.body().text();
    return text.replaceAll("\\s+", " ").trim();
  }

  private static void preserveHyperlinks(Document doc) {
    for (Element a : doc.select("a[href]")) {
      String href = a.absUrl("href").trim();
      String text = a.text().trim();
      
      if (href.isEmpty() || href.startsWith("#") || href.startsWith("javascript:")) {
        continue;
      }
      
      if (!href.equalsIgnoreCase(text)) {
        a.text(text + " (" + href + ")");
      }
    }
  }
}
