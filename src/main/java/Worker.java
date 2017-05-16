/**
 * Created by szhou on 3/29/17.
 */


import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.jsoup.select.Elements;

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
*
*
*
* https://www.elitetrader.com/et/threads/tesla-to-raise-1-15-billion-ahead-of-its-model-3-launch.307736
http://www.money-talk.org/thread29673.html&sid=7e96861e745de2f19a2a23947695ea27
http://www.marketthoughts.com/forum/best-buy-bby-t2357.html
https://www.wallstreetoasis.com/forums/anatomy-of-the-10-k
*
*
*
*
*
* */

//testing

public class Worker implements Runnable { //(added implements runnable)
    ArrayList <Integer> coffA;
    ArrayList <Integer> coffB;
    String link;
    int pageCount;
    public Worker( ArrayList<Integer> CoffA, ArrayList<Integer> CoffB, String link){
            this.coffA=CoffA;
            this.coffB=CoffB;
            this.link=link;
            //this.pageCount=pageCount;
    }

        public void run(){

        // Declaration of variables
        long startTime=System.currentTimeMillis();
        int lineNumber=1;
        int totalWordCount=0;
        int wordCount;
        int maxWordCount=0;
        ArrayList<String> domainLinks = new ArrayList<String>();

        // Get the url and parse it into a jsoup document
            try{
                System.out.println("this is my link"+ link);
                URL aURL = new URL(link.toString());
                Document doc = Jsoup.connect(link).get();
                String domain = aURL.getHost();
                //System.out.println("This is my forum domain " + domain);

                Elements links = doc.select("a");
                for (Element xlink : links) {
                    String absHref = xlink.attr("abs:href");
                    //System.out.println(absHref);
                    if(!absHref.equals(""))
                    {
                        URL nURL = new URL(absHref.toString());
                        String newLink = nURL.getHost();
                        //System.out.println("New link domain " + newLink);
                        if(newLink.equals(domain)) {
                            domainLinks.add(absHref);
                        }
                    }
                }

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
                        //System.out.println(wordCountedLines.get(getWords));
                        numOfLines++;
                    }
                }

                //System.out.println("This is the link I am visiting right now "+link);
                String text = link.replaceAll("https://","");
                text = link.replaceAll("http://","");
                text = text.replaceAll("/","-");
                String ftext= text + ".txt";
                FileWriter fw = new FileWriter(new File("collected_data", ftext));
                for(int getWords=averageWords;getWords<=maxWordCount;getWords++) {
                    Collection<String> myCollection = wordCountedLines.get(getWords);
                    if (!wordCountedLines.get(getWords).isEmpty()) {
                        if(myCollection.iterator().hasNext()) {
                            fw.write(myCollection.toString());
                            fw.write("\n");
                        }
                    }
                }
                fw.close();

            // Print the elapsed time for the program to run
                long stopTime=System.currentTimeMillis();
                long elapsedTime=stopTime-startTime;
                System.out.println("Time in milliseconds is: "+elapsedTime);
                System.out.println("Number of lines above the average: "+numOfLines);

                // get arraylist of links
                // Put links to MLBF
                // result: filterad list
                // send to main. (Bag of tasks)
                for (String dlink:domainLinks) {
                    if(!dlink.equals("")){
                    if (!BloomFilter.bloom_filter_query(dlink,coffA,coffB)){
                        BloomFilter.bloom_filter_insert(dlink,coffA,coffB);
                        System.out.println("new links:"+dlink);
                        Main.bag_of_taks.add(dlink);
                    }
                    }
                }

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

