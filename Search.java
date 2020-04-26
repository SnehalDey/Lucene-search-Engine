package project;


// Input / Output
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;


import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;


import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;


import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;


import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

public class Search{

  // Reads the index at "index" and writes results to "my-results.txt"
  public static void main(String[] args) throws IOException  {

      Search searcher= new Search();
     try { searcher.searcher();}
     catch (Exception e) {}
  }
  
  
    
    public void searcher() throws IOException, ParseException {
    	
    	 String index = "index";
    	   String result_path = "output.txt";
    	    IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
    	    IndexSearcher searcher = new IndexSearcher(reader);

    	   File result = new File("queryresult.results");
    	    PrintWriter writer = new PrintWriter( result_path, "UTF-8");
    	    
    


    //WhitespaceAnalyzer – Splits tokens at whitespace
    //Analyzer analyzer = new WhitespaceAnalyzer(); 0.1787

    //SimpleAnalyzer – Divides text at non letter characters and lowercases
    //Analyzer analyzer = new SimpleAnalyzer(); //0.2207

    //StopAnalyzer – Divides text at non letter characters, lowercases, and removes stop words
    //Analyzer analyzer = new StopAnalyzer(EnglishAnalyzer.getDefaultStopSet());

    //StandardAnalyzer - Tokenizes based on sophisticated grammar that recognizes e-mail addresses, acronyms, etc.; lowercases and removes stop words (optional)
    //Analyzer analyzer = new StandardAnalyzer();//0.2117
   // Analyzer analyzer = new StopAnalyzer(SnehalsAnalyzer.getDefaultStopSet()); 
    //EnglishAnalyzer - Analyzer with enhancements for stemming English words
    	Analyzer analyzer = new EnglishAnalyzer();

        


        //Vector Space Model
        //searcher.setSimilarity(new ClassicSimilarity());  0.3549


      
        searcher.setSimilarity(new customBM25() );   //0.4254


        String queriesPath = "cran/cran.qry";
        BufferedReader buffer = Files.newBufferedReader(Paths.get(queriesPath), StandardCharsets.UTF_8);
        
        HashMap<String,Float> boosts = new HashMap<String,Float>();

        boosts.put("title",   (float) 5);

        boosts.put("content",  (float) 20);


        
        MultiFieldQueryParser parser = new MultiFieldQueryParser(new String[] {"title","author","bibliography","content"}, analyzer,boosts);

        String queryString = "";
        Integer qNo = 1;
        String line;
        Boolean first = true;

        System.out.println("Reading in queries and creating search results.");

        while ((line = buffer.readLine()) != null){

          if(line.substring(0,2).equals(".I")){
              if(!first){
                Query query = parser.parse(QueryParser.escape(queryString));
                performSearch(searcher,writer,qNo,query);
                qNo+=1;
              }
              else{ first=false; }
              queryString = "";
          } else {
              queryString += " " + line;
          }
        }

        Query query = parser.parse(QueryParser.escape(queryString));
        performSearch(searcher,writer,qNo,query);

        writer.close();
        reader.close();
      
    }
      // Performs search and writes results to the writer
      public static void performSearch(IndexSearcher searcher, PrintWriter writer, Integer qNo, Query query) throws IOException {
        TopDocs results = searcher.search(query, 1100);
        ScoreDoc[] hits = results.scoreDocs;

        // Write the results for each hit
        int c=hits.length,j=0;
        while(j<c) {
          Document doc = searcher.doc(hits[j].doc);
          /*
           * Write the results in the format expected by trec_eval:
           * | Query Number | 0 | Document ID | Rank | Score | "EXP" |
           * (https://stackoverflow.com/questions/4275825/how-to-evaluate-a-search-retrieval-engine-using-trec-eval)
          */
          writer.println(qNo + " 0 " + doc.get("id") + " " + j + " " + hits[j].score + " EXP");
          j++;
        }
        System.out.println("Generating Results.");
      }

}

