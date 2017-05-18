/**
 * Created by szhou on 3/29/17.
 */


import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    
    public Worker( ArrayList<Integer> CoffA, ArrayList<Integer> CoffB, String link){
            this.coffA=CoffA;
            this.coffB=CoffB;
            this.link=link;
    }

        public void run(){


        // Declaration of variables
        long startTime=System.currentTimeMillis();
        ArrayList<String> domainLinks = new ArrayList<String>();

            // Get the url and parse it into a jsoup document
            try{
                System.out.println("this is my link"+ link);
                URL aURL = new URL(link.toString());
                Document doc = Jsoup.connect(link).get();
                String domain = aURL.getHost();

                Elements links = doc.select("a");
                for (Element xlink : links) {
                    String absHref = xlink.attr("abs:href");
                    if(!absHref.equals(""))
                    {
                        URL nURL = new URL(absHref.toString());
                        String newLink = nURL.getHost();
                        if(newLink.equals(domain)) {
                            domainLinks.add(absHref);
                        }
                    }
                }

                // Remove all tags with script, style and hidden
                doc.select("div.signature, .hidden, head, script").remove();

                // Create a Hashmap to store the cleaned lines later with the word count as key and a list of the lines as value
                HashMap<Integer, Ratio_Content_Pair> wordCountedLines = new HashMap<Integer, Ratio_Content_Pair>();

                // Parse the document as html and delete all the newlines and useless whitespaces
                String sourceCode=doc.html();
                String[] lines=sourceCode.split("\\s*\\r?\\n\\s*");

                // Take each line of the HTML document and delete all the html tags to clean up the lines
                // If the lines are empty, skip them. Count the word of each clean line
                // And keep track of the maximum word count and the total word count
                // Save the cleaned lines into a multimap by word count
                int lineNumber=0;
                double totalTTRatio=0;
                int radius = 35;
                for(String line:lines){

                    int lineTagCounter = 0;
                    double lineWordCount = 0;
                    double ttRatio = 0;

                    Pattern tags = Pattern.compile("<.*?>");
                    Matcher tagMatch = tags.matcher(line);


                    while (tagMatch.find())
                    {
                        lineTagCounter = lineTagCounter+1;
                    }

                    String preTrimLine=line.replaceAll("<.*?>","");
                    String cleanLine=preTrimLine.trim();
                    if(cleanLine.length()>0&&!cleanLine.equals("")) {
                        lineWordCount = countWord(cleanLine);
                    }
                    if(lineWordCount>0) {

                        lineNumber++;
                        if (lineTagCounter == 0) {
                                wordCountedLines.put(lineNumber, new Ratio_Content_Pair(lineWordCount, cleanLine));
                                totalTTRatio = totalTTRatio + lineWordCount;

                        } else {
                                ttRatio = lineWordCount / lineTagCounter;
                                wordCountedLines.put(lineNumber, new Ratio_Content_Pair(ttRatio, cleanLine));
                                totalTTRatio = totalTTRatio + ttRatio;
                        }
                        
                    }
                }

                // For each loops to go through each line. After getting the line get the TTR of each line and if it
                // above the threshold, get the value for that line (the content) and print it out with its TTR.
                System.out.println("Lines above the threshold " + totalTTRatio/(2*radius+1) + "are: ");
               for (Map.Entry<Integer, Ratio_Content_Pair> entry : wordCountedLines.entrySet()) {
                    Ratio_Content_Pair innerPair = entry.getValue();
                    if (innerPair.ratio > totalTTRatio/(2*radius+1)){
                        System.out.println("Ratio: " + innerPair.ratio + " Content: " + innerPair.content);
                    }
                }

                //System.out.println("This is the link I am visiting right now "+link);
                String text = link.replaceAll("https://","");
                text = link.replaceAll("http://","");
                text = text.replaceAll("/","*");
                String ftext= text + ".txt";
                FileWriter fw = new FileWriter(new File("collected_data", ftext));
                for (Map.Entry<Integer, Ratio_Content_Pair> entry : wordCountedLines.entrySet()) {
                    Ratio_Content_Pair innerPair = entry.getValue();
                    if (innerPair.ratio > totalTTRatio/(2*radius+1)){
                        fw.write(innerPair.content);
                        fw.write(System.getProperty( "line.separator" ));
                    }
                }
                fw.close();


                // Print the elapsed time for the program to run
                long stopTime=System.currentTimeMillis();
                long elapsedTime=stopTime-startTime;
                System.out.println("Time in milliseconds is: "+elapsedTime);

                // Get arraylist of link, put links to MLBF
                // Result: filtered list
                // Send to main. (Bag of tasks)
                for (String dlink:domainLinks) {
                    if(!dlink.equals("")){
                    if (!BloomFilter.bloom_filter_query(dlink,coffA,coffB)){
                        BloomFilter.bloom_filter_insert(dlink,coffA,coffB);
                        Main.bag_of_tasks.add(dlink);
                    }
                    }
                }

            }catch (IOException err){err.printStackTrace();}
        }

        // Word counter function
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


        //Pair class for Ratio & Content pair, for each line
        private class Ratio_Content_Pair{

            public double ratio;
            public String content;

            public Ratio_Content_Pair(double ratio, String content){
                this.ratio = ratio;
                this.content = content;

            }

            public String toString(){
                return  "{ "+ ratio + "," + content + " }";
            }
        }


}

