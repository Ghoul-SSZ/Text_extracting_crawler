/**
 * Created by szhou on 3/29/17.
 */

import java.io.IOException;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/*
* Modules
* - Link extraction
* - Text extraction
* - Signal to Regulator
*
* Arguments
* - URL (From either Regulator or Main)
*
* Outputs
* - Text Data
* - Href links to Crawl List (lvl.1)
*   - Ignore picture, PDFs, and link outside the forum
* - Idle_signal
* */

//testing

public class Worker implements Runnable { //(added implements runnable)

    public void run(){

        // Declaration of variables
        long startTime=System.currentTimeMillis();
        int lineNumber=1;
        int totalWordCount=0;
        int wordCount;
        int maxWordCount=0;

        // Get the url and parse it into a jsoup document
        try{
            org.jsoup.nodes.Document doc=org.jsoup.Jsoup.connect("https://www.elitetrader.com/et/threads/tesla-to-raise-1-15-billion-ahead-of-its-model-3-launch.307736/").get();
            //org.jsoup.nodes.Document doc = org.jsoup.Jsoup.connect("http://www.money-talk.org/thread29673.html&sid=7e96861e745de2f19a2a23947695ea27").get();
            //org.jsoup.nodes.Document doc = org.jsoup.Jsoup.connect("http://www.marketthoughts.com/forum/best-buy-bby-t2357.html").get();
            //org.jsoup.nodes.Document doc = org.jsoup.Jsoup.connect("https://www.wallstreetoasis.com/forums/anatomy-of-the-10-k").get();


            // Remove all tags with script, style and hidden
            doc.select("div.signature, noscript, script, style, .hidden").remove();
            //doc.select("div.signature").remove();

            // Create a multimap to store the cleaned lines later with the word count as key and a list of the lines as value
            Multimap<Integer, String> wordCountedLines=HashMultimap.create();

            // Parse the document as html and delete all the newlines and useless whitespaces
            String sourceCode=doc.html();
            String[]lines=sourceCode.split("\\s*\\r?\\n\\s*");

            // Take each line of the HTML document and delete all the html tags to clean up the lines
            // If the lines are empty, skip them. Count the word of each clean line
            // And keep track of the maximum word count and the total word count
            // Save the cleaned lines into a multimap by word count
            for(String line:lines){
                String preTrimLine=line.replaceAll("\\<.*?>","");
                String cleanLine=preTrimLine.trim();
                if(cleanLine.length()>0&&!cleanLine.equals("")){
                    wordCount=countWord(cleanLine);
                    if(wordCount>2){
                        if(wordCount>maxWordCount)maxWordCount=wordCount;
                        totalWordCount=totalWordCount+wordCount;
                        //System.out.println("Line " + lineNumber + " WC: " + wordCount + " " + cleanLine);
                        wordCountedLines.put(wordCount,cleanLine);
                        lineNumber++;
                    }
                }
            }
            // Get the average word count of the html document
            int averageWords=totalWordCount/lineNumber;
            System.out.println(totalWordCount);
            System.out.println(averageWords);

            // Use the average word count to retrieve the lines with greater or equal word count from the multimap
            int numOfLines=0;
            for(int getWords=averageWords;getWords<=maxWordCount;getWords++){
                if(!wordCountedLines.get(getWords).isEmpty()){
                    System.out.println(wordCountedLines.get(getWords));
                    numOfLines++;
                }
            }

            // Print the elapsed time for the program to run
            long stopTime=System.currentTimeMillis();
            long elapsedTime=stopTime-startTime;
            System.out.println("Time in milliseconds is: "+elapsedTime);
            System.out.println("Number of lines above the average: "+numOfLines);
        }catch (IOException err){err.printStackTrace();}

    }

    // Word counter
    private static int countWord(String word) {
        if (word == null) {
            return 0;
        }
        String input = word.trim();
        int count;
        if (input.length()==0) count = 0;
        else count = input.split("\\s+").length;
        return count;
    }

}

