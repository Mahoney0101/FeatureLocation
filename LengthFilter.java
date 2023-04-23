package featurelocation;

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

public class LengthFilter extends TokenFilter {
    private final CharTermAttribute termAttribute = addAttribute(CharTermAttribute.class);
    private final int min;

    public LengthFilter(TokenStream input, int min) {
        super(input);
        this.min = min;
    }

    @Override
    public boolean incrementToken() throws IOException {
        while (input.incrementToken()) {
            if (termAttribute.length() >= min) {
                return true;
            }
        }
        return false;
    }
}
