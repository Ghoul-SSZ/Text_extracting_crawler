/**
 * Created by szhou on 3/29/17.
 */


import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
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
    double lamda = 0.5; // needs to fix it
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

        ArrayList<String> domainLinks = new ArrayList<String>();

        // Get the url and parse it into a jsoup document
            try{
                System.out.println("this is my link "+ link);
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
                /*Start of Algorithm 1*/
                /* In the preprocessing stage, the header parts (<head> ...</head>) are removed,
                as well as the content of all <style> and <script> tags. PHP and JAVA source code are also removed
                because they are not text content.*/
                doc.select("div.signature, head, noscript, script, style, .hidden, php").remove();


                // Create a multimap to store the cleaned lines later with the word count as key and a list of the lines as value
                Multimap<Integer, String> wordCountedLines=HashMultimap.create();

                // Parse the document as html and delete all the newlines and useless whitespaces
                String sourceCode=doc.html();
                String[]lines=sourceCode.split("\\s*\\r?\\n\\s*");

                // Take each line of the HTML document and delete all the html tags to clean up the lines

                ArrayList<Double> H_CTTD_list = new ArrayList<Double>();
                for(String line:lines){
                    //Count Word
                    int TC = countWord(line);
                    //Count Link Word
                    int LTC = countLinkWord(line);
                    //Count Tags
                    int TG = countTags(line);
                    //Count Count Tag Page Count
                    int P = countTagP(line);

                    //Hi CTTD
                    double CTTD;
                    if (LTC+TC==0){
                        CTTD=0;
                        H_CTTD_list.add(CTTD);
                    }else {
                        CTTD= TC+(lamda*LTC)+P-TG;
                        H_CTTD_list.add(CTTD);
                    }

                }

                /*End of Algorithm 1*/
                /*Start of Algorithm 2 -- Smoothing*/
                ArrayList <Double> H_CTTD_list_Smooth = new ArrayList<Double>();
                for(int j=0; j<H_CTTD_list.size(); j++){
                    if (j<1){
                        H_CTTD_list_Smooth.add(H_CTTD_list.get(j));
                    }else{
                        //CCTD_Smooth = 0.1(CTTD_j-2 + 2* CTTD_j-1 + 4*CTTD_j +2*CTTD_j+1 +CTTD_j+2)
                        double CCTD_Smooth = 0.1*(H_CTTD_list.get(j-2) + 2*H_CTTD_list.get(j-1) + 4*H_CTTD_list.get(j) + 2*H_CTTD_list.get(j+1) + H_CTTD_list.get(j+2));
                        H_CTTD_list_Smooth.add(CCTD_Smooth);
                    }
                }
                /*End of Algorithm 2*/
                /*Start of Algorithm 3 -- Find candidates*/

                double CTTD_min = Integer.MAX_VALUE;
                double CTTD_max = Integer.MIN_VALUE;

                for (double m:H_CTTD_list_Smooth) {
                    if (m>CTTD_max){CTTD_max=m;}
                    if (m<CTTD_min){CTTD_min=m;}
                }
                double CTTD_theta= CTTD_min + 0.5*(CTTD_max - CTTD_min);

                int Gap_theta = 3; // ask mike tomorrow

                //TB?
                ArrayList<Pair<Integer,Integer>> TBs = new ArrayList<Pair<Integer, Integer>>();
                int k = 0; //used to count the number of candidates
                int start=0;
                int gap=0;
                int TC_i;
                int TC_n=0;

                for (int i =0; i<lines.length;i++){
                    if (H_CTTD_list_Smooth.get(i)>CTTD_theta){
                         start = i;
                         gap = 0;
                         TC_i= countWord(lines[i]);

                         for (int a =i+1; a<lines.length; a++){
                             if (countTags(lines[i])==0){
                                 continue;
                             }else if (H_CTTD_list_Smooth.get(i)<=CTTD_theta && gap>=Gap_theta){
                                 break;
                             }else if (H_CTTD_list_Smooth.get(i)<=CTTD_theta){
                                 gap++;
                             }
                             TC_n += TC_i;
                         }
                    }

                    if (countWord(lines[i])<1){
                        continue;
                    }

                    Pair TB = new Pair(start,i-1);
                    TBs.add(TB);
                }
                /*End of Algorithm 3*/


                //System.out.println("This is the link I am visiting right now "+link);
                String text = link.replaceAll("https://","");
                text = text.replaceAll("http://","");
                text = text.replaceAll("/","*");
                String ftext= text + ".txt";
                FileWriter fw = new FileWriter(new File("collected_data", ftext));


                fw.close();

            // Print the elapsed time for the program to run
                long stopTime=System.currentTimeMillis();
                long elapsedTime=stopTime-startTime;
                System.out.println("Time in milliseconds is: "+elapsedTime);

                // get arraylist of links
                // Put links to MLBF
                // result: filterad list
                // send to main. (Bag of tasks)
                for (String dlink:domainLinks) {
                    if(!dlink.equals("")){
                    if (!BloomFilter.bloom_filter_query(dlink,coffA,coffB)){
                        BloomFilter.bloom_filter_insert(dlink,coffA,coffB);
                        //System.out.println("new links:"+dlink);
                        Main.bag_of_tasks.add(dlink);
                    }
                    }
                }

            }catch (IOException err){err.printStackTrace();}
        }

        // Word counter
        private static int countWord(String line) {
            //Count Word
            String preTrimLine=line.replaceAll("\\<.*?>","");
            String cleanLine=preTrimLine.trim();

            if (cleanLine == null) {
                return 0;
            }
            String input = cleanLine.trim();
            int count;
            if (input.length()==0) {count = 0;}
            else {count = input.split("\\s+").length;}
            return count;
        }

        private static int countLinkWord(String line){
            int linkWordCount;
            //<a href="url">link text</a>
            if (line.contains("</a>")){

                Pattern pattern = Pattern.compile(".*?<a.*?>(.*?)</a>");
                Matcher matcher = pattern.matcher(line);
                if (matcher.find())
                {
                    String preTrimLine =matcher.group(1);
                    preTrimLine=preTrimLine.replaceAll("<.*?>.*?<.*?>","");
                    String cleanLine=preTrimLine.trim();
                    //System.out.println(cleanLine);
                    linkWordCount=cleanLine.split("\\s+").length;
                    return linkWordCount;
                }else{return 0;}

            }
                return 0;
            }
        private static int countTags(String line){
            int tagCount = 0;
            Pattern pattern = Pattern.compile("<(.*?)>");
            Matcher matcher = pattern.matcher(line);

            while (matcher.find()){tagCount++;}
            return tagCount;
        }

        private static int countTagP(String line){
            Pattern pattern = Pattern.compile("<p.*?>");
            Matcher matcher = pattern.matcher(line);
            int pCount = 0;

            while (matcher.find()){pCount++;}
            return pCount;
        }


}

 class Pair<S, T> {
    public final S x;
    public final T y;

    public Pair(S x, T y) {
        this.x = x;
        this.y = y;
    }
}
