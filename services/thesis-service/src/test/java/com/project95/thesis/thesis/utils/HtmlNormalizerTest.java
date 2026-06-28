package com.project95.thesis.thesis.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HtmlNormalizerTest {

  @Test
  void sanitizeHtml_StripsNoiseAndPrunesDocument() {
    String html = "<html>"
        + "<head><style>body { color: red; }</style></head>"
        + "<body>"
        + "  <header>Navigation Bar</header>"
        + "  <main>"
        + "    <h1>Open Thesis Positions</h1>"
        + "    <script>console.log('hello');</script>"
        + "    <p>We are looking for motivated students.</p>"
        + "  </main>"
        + "  <footer>Copyright 2026</footer>"
        + "</body></html>";

    String sanitized = HtmlNormalizer.sanitizeHtml(html);
    
    assertThat(sanitized).contains("Open Thesis Positions");
    assertThat(sanitized).contains("We are looking for motivated students.");
    
    // Header, footer, script, and style must be stripped
    assertThat(sanitized).doesNotContain("Navigation Bar");
    assertThat(sanitized).doesNotContain("console.log");
    assertThat(sanitized).doesNotContain("body { color: red; }");
    assertThat(sanitized).doesNotContain("Copyright 2026");
  }

  @Test
  void sanitizeHtml_InlinesHyperlinksWithAbsoluteUrls() {
    String html = "<p>Please contact <a href=\"/people/john-doe\">John Doe</a> or read the <a href=\"https://example.com/guidelines.pdf\">guidelines</a>.</p>";
    String baseUrl = "https://tum.de/chair";

    String sanitized = HtmlNormalizer.sanitizeHtml(html, baseUrl);

    // John Doe's relative URL must be resolved against baseUrl
    assertThat(sanitized).contains("John Doe (https://tum.de/people/john-doe)");
    // The guidelines link must remain absolute
    assertThat(sanitized).contains("guidelines (https://example.com/guidelines.pdf)");
  }

  @Test
  void getNormalizedText_CompressesWhitespacesAndTrims() {
    String html = "<h1>Title</h1> \n\n <p>Some    text    with  spaces.   </p>";
    String sanitized = HtmlNormalizer.sanitizeHtml(html);
    String normalized = HtmlNormalizer.getNormalizedText(sanitized);

    assertThat(normalized).isEqualTo("Title Some text with spaces.");
  }
}
