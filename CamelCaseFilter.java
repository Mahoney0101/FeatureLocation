package featurelocation;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

public class CamelCaseFilter extends TokenFilter {
    private final CharTermAttribute termAttribute = addAttribute(CharTermAttribute.class);
    private final PositionIncrementAttribute positionIncrementAttribute = addAttribute(PositionIncrementAttribute.class);
    private final Queue<String> tokens = new LinkedList<>();

    public CamelCaseFilter(TokenStream input) {
        super(input);
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (!tokens.isEmpty()) {
            String token = tokens.poll();
            termAttribute.setEmpty().append(token);
            positionIncrementAttribute.setPositionIncrement(0);
            return true;
        }

        while (input.incrementToken()) {
            String term = termAttribute.toString();
            String[] parts = term.split("(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])|(?<=\\d)(?=\\D)|(?<=\\D)(?=\\d)|_");

            if (parts.length > 0) {
                termAttribute.setEmpty().append(parts[0]);
                positionIncrementAttribute.setPositionIncrement(1);
                for (int i = 1; i < parts.length; i++) {
                    tokens.offer(parts[i]);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        tokens.clear();
    }
}
