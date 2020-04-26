package project;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/** Index all text files under a directory.
 * <p>
 * This is a command-line application demonstrating simple Lucene indexing.
 * Run it with no command-line arguments for usage information.
 */
public class index {

	public index() {}

	  /** Index all text files under a directory. */
	  public static void main(String[] args) {
	    String usage = "java org.apache.lucene.demo.IndexFiles"
	                 + " [-index INDEX_PATH] [-docs DOCS_PATH] [-update]\n\n"
	                 + "This indexes the documents in DOCS_PATH, creating a Lucene index"
	                 + "in INDEX_PATH that can be searched with SearchFiles";
	    String indexPath = "index";
	    String docsPath = "cran/cran.all.1400";
	    boolean create = true;
	    for(int i=0;i<args.length;i++) {
	      if ("-index".equals(args[i])) {
	        indexPath = args[i+1];
	        i++;
	      } else if ("-docs".equals(args[i])) {
	        docsPath = args[i+1];
	        i++;
	      } else if ("-update".equals(args[i])) {
	        create = false;
	      }
	    }

	    if (docsPath == null) {
	      System.err.println("Usage: " + usage);
	      System.exit(1);
	    }

	    final Path docDir = Paths.get(docsPath);
	    if (!Files.isReadable(docDir)) {
	      System.out.println("Document directory '" +docDir.toAbsolutePath()+ "' does not exist or is not readable, please check the path");
	      System.exit(1);
	    }
	    
	    index myindexer= new index();
	    myindexer.indexer( indexPath ,docsPath, create ,docDir );
	   
		  }
	  
	  public void indexer(String indexPath ,String docsPath,boolean create , final Path docDir ) {
		  Date start = new Date();
		    try {
		      System.out.println("Starting indexing to directory '" + indexPath + "'...");
		     
		      Directory dir = FSDirectory.open(Paths.get(indexPath));
		 	  //Analyzer analyzer = new StopAnalyzer(SnehalsAnalyzer.getDefaultStopSet()); 
		      //Analyzer analyzer = new SnehalsAnalyzer(); 
		    Analyzer analyzer = new EnglishAnalyzer();    
			    //Analyzer analyzer = new StopAnalyzer(SnehalsAnalyzer.getDefaultStopSet()); 

		       IndexWriterConfig config = new IndexWriterConfig(analyzer);
		       config.setSimilarity(new customBM25() );

		      if (create) {
		        // Create a new index in the directory, removing any
		        // previously indexed documents:
		        config.setOpenMode(OpenMode.CREATE);
		      } else {
		        // Add new documents to an existing index:
		        config.setOpenMode(OpenMode.CREATE_OR_APPEND);
		      }

		      // Optional: for better indexing performance, if you
		      // are indexing many documents, increase the RAM
		      // buffer.  But if you do this, increase the max heap
		      // size to the JVM (eg add -Xmx512m or -Xmx1g):
		      //
		       config.setRAMBufferSizeMB(256.0);

		      IndexWriter writer = new IndexWriter(dir, config);
		      indexFiles(writer, docDir);

		      // NOTE: if you want to maximize search performance,
		      // you can optionally call forceMerge here.  This can be
		      // a terribly costly operation, so generally it's only
		      // worth it when your index is relatively static (ie
		      // you're done adding documents to it):
		      //
		      // writer.forceMerge(1);

		      writer.close();

		      Date end = new Date();
		      System.out.println(end.getTime() - start.getTime() + "milliseconds taken");

		    } catch (IOException exception) {
		      System.out.println(" caught a " + exception.getClass() +
		       "\n with message: " + exception.getMessage());
		    }
		  }

		  /**
		   * Indexes the given file using the given writer, or if a directory is given,
		   * recurses over files and directories found under the given directory.
		   * 
		   * NOTE: This method indexes one document per input file.  This is slow.  For good
		   * throughput, put multiple documents into your input file(s).  An example of this is
		   * in the benchmark module, which can create "line doc" files, one document per line,
		   * using the
		   * <a href="../../../../../contrib-benchmark/org/apache/lucene/benchmark/byTask/tasks/WriteLineDocTask.html"
		   * >WriteLineDocTask</a>.
		   *  
		   * @param writer Writer to the index where the given file/dir info will be stored
		   * @param path The file to index, or the directory to recurse into to find files to index
		   * @throws IOException If there is a low-level I/O error
		   */
		  static Boolean isHeading(String line){
			    String s = line.substring(0,2);
			    return (s.equals(".I") || s.equals(".T") || s.equals(".A") || s.equals(".B") || s.equals(".W"));
			  }

			  // Creates a document with the fields specified to be written to an index
			  static Document createDocument(String id, String title, String author, String bibliography, String content){
			    Document doc = new Document();
			    doc.add(new StringField("id", id, Field.Store.YES));
			    doc.add(new StringField("path", id, Field.Store.YES));
			    doc.add(new TextField("title", title, Field.Store.YES));
			    doc.add(new TextField("author", author, Field.Store.YES));
			    doc.add(new TextField("bibliographyliography", bibliography, Field.Store.YES));
			    doc.add(new TextField("content", content, Field.Store.YES));
			    return doc;
			  }

			  /** Indexes the cranfield collection */
			  static void indexFiles(IndexWriter writer, Path file) throws IOException {
			    try (InputStream stream = Files.newInputStream(file)) {

			      BufferedReader buffer = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));

			      String id = "", title = "", author = "", bibliography = "", content = "", state = "";
			      Boolean first = true;
			      String line;

			      System.out.println("Indexing documents.");

			      // Read in lines from the cranfield collection and create indexes for them
			      while ((line = buffer.readLine()) != null){
			        switch(line.substring(0,2)){
			          case ".I":
			            if(!first){
			              Document d = createDocument(id,title,author,bibliography,content);
			              writer.addDocument(d);
			            }
			            else{ first=false; }
			            title = ""; author = ""; bibliography = ""; content = "";
			            id = line.substring(3,line.length()); break;
			          case ".T":
			          case ".A":
			          case ".B":
			          case ".W":
			            state = line;
			            break;
			          default:
			            switch(state){
			              case ".T": title += line + " "; break;
			              case ".A": author += line + " "; break;
			              case ".B": bibliography += line + " "; break;
			              case ".W": content += line + " "; break;
			            }
			        }
			      }
			      
			      Document d = createDocument(id,title,author,bibliography,content);
			      writer.addDocument(d);
			    }
	  }
	  
}

