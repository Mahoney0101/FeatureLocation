package featurelocation;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.ByteBuffersDirectory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class FeatureLocation {
	public static void main(String[] args) throws IOException, ParseException {
		//Add stopwords and creat CustomAnalyser for Stemming Length etc..
		String stopWordsFilePath = "src/main/resources/stop-words-english-total.txt";
	    Analyzer analyzer = new CustomAnalyzer(stopWordsFilePath);
	    // Create Lucene index
	    Directory index = new ByteBuffersDirectory();
	    IndexWriterConfig config = new IndexWriterConfig(analyzer);
	    IndexWriter indexWriter = new IndexWriter(index, config);
	
	    // Index the Java files from JabRef
	    File javaFilesDirectory = new File("src/main/resources/JabRef/sources/3.7/src/");
	    Set<String> projectTypes = new HashSet<>();
	    collectProjectTypes(javaFilesDirectory, projectTypes);
	    processJavaFiles(javaFilesDirectory, indexWriter, projectTypes);
	
	    System.out.println("Total indexed documents: " + indexWriter.numRamDocs());
	    indexWriter.close();
	    // Read user query
	    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
	    System.out.println("Enter your query: ");
	    String queryStr = reader.readLine();
	
	    QueryParser commentsParser = new QueryParser("comments", analyzer);
	    QueryParser artifactNamesParser = new QueryParser("artifact_names", analyzer);
	
	    Query commentsQuery = commentsParser.parse(queryStr);
	    Query artifactNamesQuery = artifactNamesParser.parse(queryStr);
	
	    DirectoryReader indexReader = DirectoryReader.open(index);
	    IndexSearcher searcher = new IndexSearcher(indexReader);
	
	    TopDocs commentsResults = searcher.search(commentsQuery, 10);
	    TopDocs artifactNamesResults = searcher.search(artifactNamesQuery, 10);

	    System.out.println("Results in set A (comments): " + commentsResults.scoreDocs.length + " hits");
	    System.out.println("Results in set B (artifact_names): " + artifactNamesResults.scoreDocs.length + " hits");

	    Set<String> setA = new HashSet<>();
	    Set<String> setB = new HashSet<>();
	    Set<String> setC = new HashSet<>();
	
	    for (ScoreDoc scoreDoc : commentsResults.scoreDocs) {
	        Document doc = searcher.doc(scoreDoc.doc);
	        String identifier = doc.get("identifier");
	        setA.add(identifier);
	    }
	
	    for (ScoreDoc scoreDoc : artifactNamesResults.scoreDocs) {
	        Document doc = searcher.doc(scoreDoc.doc);
	        String identifier = doc.get("identifier");
	        setB.add(identifier);
	        if (setA.contains(identifier)) {
	            setC.add(identifier);
	        }
	    }
	    // Print the common documents (set C)
	    System.out.println("\nCommon documents in set A and set B (set C):");
	    for (String identifier : setC) {
	        System.out.println(identifier);
	    }
	    // Close resources
	    indexReader.close();
	    reader.close();
	}
	
	private static String readJavaFile(File javaFile) throws IOException {
	    StringBuilder builder = new StringBuilder();
	    try (BufferedReader reader = new BufferedReader(new FileReader(javaFile))) {
	        String line;
	        while ((line = reader.readLine()) != null) {
	            builder.append(line).append(System.lineSeparator());
	        }
	    }
	    return builder.toString();
	}
	
	private static boolean isValidType(String type, Set<String> projectTypes) {
	    return projectTypes.contains(type);
	}
	
	private static void processJavaFiles(File directory, IndexWriter indexWriter, Set<String> projectTypes) throws IOException {
		for (File file : directory.listFiles()) {
	        if (file.isDirectory()) {
	            processJavaFiles(file, indexWriter, projectTypes);
	        } else if (file.getName().endsWith(".java")) {
	            // Read the contents of the Java file
	            String javaFileContents = readJavaFile(file);

	            // Extract the comments, artifact names, and identifier
	            String comments = extractComments(javaFileContents);
	            String artifactNames = extractArtifactNames(file, projectTypes);
	            String identifier = file.getAbsolutePath();
	            
	            System.out.println("Extracted artifact names for file " + file.getName() + ": " + artifactNames);

	            // Create Lucene document
	            Document doc = new Document();
	            doc.add(new TextField("comments", comments, Field.Store.YES));
	            doc.add(new TextField("artifact_names", artifactNames, Field.Store.YES));
	            doc.add(new StringField("identifier", identifier, Field.Store.YES));

	            // Add the document to the index
	            indexWriter.addDocument(doc);

	            System.out.println("Added document to the index: " + doc);
	        }
	    }
	}

	
	private static String extractComments(String javaFileContents) {
	    StringBuilder comments = new StringBuilder();
	    boolean inMultilineComment = false;
	    
	    String[] lines = javaFileContents.split("\n");
	    for (String line : lines) {
	        line = line.trim();

	        // Extract comments
	        int singleLineCommentIndex = line.indexOf("//");
	        if (singleLineCommentIndex != -1) {
	            comments.append(line.substring(singleLineCommentIndex + 2)).append(" ");
	        }

	        for (int i = 0; i < line.length(); i++) {
	            char currentChar = line.charAt(i);
	            if (currentChar == '/' && i < line.length() - 1 && line.charAt(i + 1) == '*') {
	                inMultilineComment = true;
	                i++; // Skip the next character
	            } else if (currentChar == '*' && i < line.length() - 1 && line.charAt(i + 1) == '/') {
	                inMultilineComment = false;
	                i++; // Skip the next character
	            } else if (inMultilineComment) {
	                comments.append(currentChar);
	            }
	        }
	    }
	    
	    return comments.toString();
	}


	private static String extractArtifactNames(File javaFile, Set<String> projectTypes) throws IOException {
	    StringBuilder artifactNames = new StringBuilder();
	    String javaFileContents = readJavaFile(javaFile);

	    JavaParser parser = new JavaParser();
	    ParseResult<CompilationUnit> result = parser.parse(javaFileContents);
	    result.ifSuccessful(cu -> {
	        cu.accept(new VoidVisitorAdapter<Void>() {
	            @Override
	            public void visit(ClassOrInterfaceDeclaration cid, Void arg) {
	                artifactNames.append(cid.getNameAsString()).append(" ");
	                super.visit(cid, arg);
	            }

	            @Override
	            public void visit(MethodDeclaration md, Void arg) {
	                artifactNames.append(md.getNameAsString()).append(" ");
	                md.getParameters().forEach(param -> {
	                    if (param.getType() instanceof ReferenceType) {
	                        String typeName = ((ReferenceType) param.getType()).asString();
	                        if (isValidType(typeName, projectTypes)) {
	                            artifactNames.append(param.getNameAsString()).append(" ");
	                        }
	                    }
	                });
	                super.visit(md, arg);
	            }

	            @Override
	            public void visit(FieldDeclaration fd, Void arg) {
	                if (fd.getElementType() instanceof ReferenceType) {
	                    String typeName = ((ReferenceType) fd.getElementType()).asString();
	                    if (isValidType(typeName, projectTypes)) {
	                        fd.getVariables().forEach(var -> {
	                            artifactNames.append(var.getNameAsString()).append(" ");
	                        });
	                    }
	                }
	                super.visit(fd, arg);
	            }

	            @Override
	            public void visit(VariableDeclarator vd, Void arg) {
	                super.visit(vd, arg);
	            }
	        }, null);
	    });

	    return artifactNames.toString();
	}
	
	
	private static void collectProjectTypes(File directory, Set<String> projectTypes) throws IOException {
	    for (File file : directory.listFiles()) {
	        if (file.isDirectory()) {
	            collectProjectTypes(file, projectTypes);
	        } else if (file.getName().endsWith(".java")) {
	            String javaFileContents = readJavaFile(file);
	            JavaParser parser = new JavaParser();
	            ParseResult<CompilationUnit> result = parser.parse(javaFileContents);
	            result.ifSuccessful(cu -> {
	                cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classOrInterface -> {
	                    projectTypes.add(classOrInterface.getNameAsString());
	                });
	            });
	        }
	    }
	}
}
