package featurelocation;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.TokenStream;

import java.io.IOException;

public class FirstPositionIncrementFilter extends TokenFilter {
    private final PositionIncrementAttribute posIncrAtt;
    private boolean firstToken = true;

    public FirstPositionIncrementFilter(TokenStream input) {
        super(input);
        this.posIncrAtt = addAttribute(PositionIncrementAttribute.class);
    }

    @Override
    public boolean incrementToken() throws IOException {
        boolean hasNextToken = input.incrementToken();
        if (hasNextToken && firstToken) {
            posIncrAtt.setPositionIncrement(Math.max(1, posIncrAtt.getPositionIncrement()));
            firstToken = false;
        }
        return hasNextToken;
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        firstToken = true;
    }
}

