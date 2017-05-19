package com.scraper.app;

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

/**
 *
 *
 *
 * The Worker class implements an application that
 * fetches web-links from a queue to crawl through
 * while extracting text.
 *
 * It uses the JSOUP Framework for the basic crawling,
 * as well as for the DOM manipulation of the HTML.
 *
 * @author Steven Shidi Zhou
 * @author Michael Palma Alegria
 *
 * @version 1.0
 * @since 2017-05-15
 *
 *
 */


public class Worker implements Runnable {
    ArrayList <Integer> coffA;
    ArrayList <Integer> coffB;
    String link;
    int lineCounter = 0;
    int contentThreshold = 7;
    int radius = 25;

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
                System.out.println("Threshold " + totalTTRatio/(2*radius+1));
                for (Map.Entry<Integer, Ratio_Content_Pair> entry : wordCountedLines.entrySet()) {
                    Ratio_Content_Pair innerPair = entry.getValue();
                    if (innerPair.ratio > totalTTRatio/(2*radius+1)){
                        lineCounter++;
                    }
                }

                System.out.println("This is the link I am visiting right now "+link);
                if(lineCounter>contentThreshold){

                    String text = link.replaceAll("https://", "");
                    text = link.replaceAll("http://", "");
                    text = text.replaceAll("/", "*");
                    String ftext = text + ".txt";
                    FileWriter fw = new FileWriter(new File("collected_data", ftext));
                    for (Map.Entry<Integer, Ratio_Content_Pair> entry : wordCountedLines.entrySet()) {
                        Ratio_Content_Pair innerPair = entry.getValue();
                        if (innerPair.ratio > totalTTRatio / (2 * radius + 1)) {
                            fw.write(innerPair.content);
                            fw.write(System.getProperty("line.separator"));
                        }
                    }
                    fw.close();

                }else{
                    System.out.println("Not a thread page: " + link);
                }

                // Print the elapsed time for the program to run
                long stopTime=System.currentTimeMillis();
                long elapsedTime=stopTime-startTime;
                System.out.println("Time in milliseconds is: "+elapsedTime);
                System.out.println("---------------------------------------------------------------------------");

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

        /*!
        *
        * This method is used to count the words in a string
        *  or line of a document.
        *
        * @param word A string of words to be processed
        * @return The number of words in the processed string
        * @warning S.M Don't be "Stupid Mike"!!
        */
        public static int countWord(String word) {
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
        public class Ratio_Content_Pair{

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

