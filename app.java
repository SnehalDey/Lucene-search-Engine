package project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class app {
	
	
	
	
	  public static void main(String[] args) throws IOException {
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
		   
		    Search searcher= new Search();
  			try { searcher.searcher();}
  			catch (Exception e) {}
}
		    
			  }
	
	
	  			
