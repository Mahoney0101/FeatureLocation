package featurelocation;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class CustomAnalyzer extends Analyzer {
    private final CharArraySet stopWords;

    public CustomAnalyzer(String stopWordsFilePath) throws IOException {
        List<String> stopWordList = Files.readAllLines(Paths.get(stopWordsFilePath));
        stopWords = new CharArraySet(stopWordList, true);
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        StandardTokenizer src = new StandardTokenizer();
        TokenStream filter = new CamelCaseFilter(src);
        filter = new LowerCaseFilter(filter);
        filter = new PorterStemFilter(filter);
        filter = new ASCIIFoldingFilter(filter);
        filter = new StopFilter(filter, stopWords);
        filter = new LengthFilter(filter, 3);
        filter = new FirstPositionIncrementFilter(filter);

        return new TokenStreamComponents(src, filter);
    }
}
