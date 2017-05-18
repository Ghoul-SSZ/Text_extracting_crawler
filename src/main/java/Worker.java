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
import java.util.Calendar;

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

                // Parse the document as html and delete all the newlines and useless whitespaces
                String sourceCode=doc.html();
                String[]lines=sourceCode.split("\\s*\\r?\\n\\s*");

                // Take each line of the HTML document and delete all the html tags to clean up the lines

                ArrayList<Double> H_CTTD_list = new ArrayList<Double>();
                ArrayList<String> TimeStamps = new ArrayList<String>();
                for(String line:lines){
                    //Extract time stamp
                    Pattern p1 = Pattern.compile("(\\w{3}\\s\\d{2}.\\s\\d{4})");
                    Pattern p2 = Pattern.compile("(\\w{3}\\s\\d{2}\\s\\d{4})");

                    Pattern p3 = Pattern.compile("(\\w{3}\\s\\d{1}.\\s\\d{4})");
                    Pattern p4 = Pattern.compile("(\\w{3}\\s\\d{1}\\s\\d{4})");


                    //you could also use "d\\^(.*)Z" as your regex patern
                    Matcher m1 = p1.matcher(line);
                    Matcher m2 = p2.matcher(line);
                    Matcher m3 = p3.matcher(line);
                    Matcher m4 = p4.matcher(line);

                    if (m1.find() ) {
                        //System.out.println(m1.group(1)); //print out the timestamp
                        TimeStamps.add(m1.group(1));
                    }

                    if (m2.find()){
                        TimeStamps.add(m2.group(1));
                    }

                    if (m3.find()){
                        TimeStamps.add(m3.group(1));
                    }

                    if (m4.find()){
                        TimeStamps.add(m4.group(1));
                    }


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

                //Filter the time stamps
                Calendar min_date = Calendar.getInstance();
                Calendar max_date = Calendar.getInstance();
                max_date.set(1000,1,1);


                for (String e:TimeStamps) {
                    String[] month_date_year = e.split(" ");
                    String month = month_date_year[0];
                    String date = month_date_year[1].replace(",","");
                    Calendar temp_date = Calendar.getInstance();

                    int month_int = getMonth(month);
                    int date_int =  Integer.parseInt(date);
                    int year_int = Integer.parseInt(month_date_year[2]);

                    temp_date.set(year_int,month_int,date_int);

                    if (temp_date.before(min_date)){
                        min_date = temp_date;
                    }

                    if (temp_date.after(max_date)){
                        max_date = temp_date;
                    }

                }

                /*End of Algorithm 1*/
                /*Start of Algorithm 2 -- Smoothing*/
                ArrayList <Double> H_CTTD_list_Smooth = new ArrayList<Double>();
                for(int j=0; j<H_CTTD_list.size(); j++){
                    if (j<=1){
                        H_CTTD_list_Smooth.add(H_CTTD_list.get(j));
                    }else if(j>H_CTTD_list.size()-3){
                        H_CTTD_list_Smooth.add(H_CTTD_list.get(j));
                    }else{
                        //CCTD_Smooth = 0.1(CTTD_j-2 + 2* CTTD_j-1 + 4*CTTD_j +2*CTTD_j+1 +CTTD_j+2)
                        double CCTD_Smooth = 0.1*(H_CTTD_list.get(j-2) + 2*H_CTTD_list.get(j-1) + 4*H_CTTD_list.get(j) + 2*H_CTTD_list.get(j+1) + H_CTTD_list.get(j+2));
                        H_CTTD_list_Smooth.add(CCTD_Smooth);
                    }
                }
                //System.out.println("Algorithm 2");

                /*End of Algorithm 2*/
                /*Start of Algorithm 3 -- Find candidates*/

                double CTTD_min = Integer.MAX_VALUE;
                double CTTD_max = Integer.MIN_VALUE;

                for (double m:H_CTTD_list_Smooth) {
                    if (m>CTTD_max){CTTD_max=m;}
                    if (m<CTTD_min){CTTD_min=m;}
                }
                double CTTD_theta= CTTD_min + 0.5*(CTTD_max - CTTD_min);

                int Gap_theta = 10; // ask mike tomorrow

                //TB?
                ArrayList<Pair<Integer,Integer>> TBs = new ArrayList<Pair<Integer, Integer>>();
                int k = 0; //used to count the number of candidates
                int start=0;
                int gap;
                int TC_i;
                int TC_n=0;

                for (int i =0; i<lines.length;i++){
                    int end=0;
                    if (H_CTTD_list_Smooth.get(i)>CTTD_theta){
                         start = i;
                         gap = 0;
                         TC_i= countWord(lines[i]);

                         for (int a = start+1; a<lines.length; a++){
                             if (countTags(lines[a]) == 0){
                                 continue;
                             }else if ((H_CTTD_list_Smooth.get(a) < CTTD_theta) && (gap>=Gap_theta)){
                                 end=a;
                                 break;
                             }else if ((H_CTTD_list_Smooth.get(a) < CTTD_theta)){
                                 gap++;
                             }
                             TC_n =TC_n + TC_i;
                             end=a;


                         }
                    }


                    if(end!=0){
                        Pair TB = new Pair(start,end-1);
                        //System.out.println("Start "+TB.x.toString());
                        //System.out.println("End "+TB.y.toString());
                        //System.out.println("Textbox: " + TBs.size());
                        if (!TBs.contains(TB)) {
                            TBs.add(TB);
                            //System.out.println(TBs.toString());
                        }
                        k++;
                    }

                    if (TC_n<1){continue;}


                }
                System.out.println("Number of TextBoxes: "+k);

                //Constructing Text boxes
                ArrayList<ArrayList<String>> cTB = new ArrayList<ArrayList<String>>();

                for (Pair TB:TBs) {
                    ArrayList<String> TB_lines = new ArrayList<String>();

                    int TB_start = (Integer) TB.x;
                    int TB_end = (Integer) TB.y;

                    for (int s=TB_start; s <= TB_end;s++){
                        //System.out.println("S here" + s);
                        TB_lines.add(lines[s]);
                        //System.out.println("TEXT: " + lines[s]);
                    }
                    cTB.add(TB_lines);
                }
                //System.out.println("TBs: " + cTB);
                //System.out.println("Algorithm 3");

                /*
                for (ArrayList e:cTB) {
                    System.out.println("CHECIKING..." + e.toString());
                }*/

                /*End of Algorithm 3*/

                /*Algorithm 4: Text verification*/
                ArrayList<ArrayList<String>> verified_textbox = new ArrayList<ArrayList<String>>();
                int counter = 0;

                for (int at=0; at< cTB.size(); at++){
                //for (ArrayList t:cTB) {
                    int Ll_number=0;
                    int TC_threshold = 30;
                    int Tv_wordcount= 0;
                    int link_wordcount = 0;
                    boolean copyrighted = false;
                    //System.out.println(cTB.size());
                    //System.out.println(counter++);
                    for(int att=0; att<cTB.get(at).size(); att++){
                    //for (Object e: cTB.get(at)) {
                        Tv_wordcount = Tv_wordcount+countWord(cTB.get(at).get(att));
                        link_wordcount= link_wordcount+countLinkWord(cTB.get(at).get(att));
                        if (cTB.get(at).get(att).contains("</p>")){Ll_number++;}
                        if (cTB.get(at).get(att).contains("</br>")){Ll_number++;}
                        if (cTB.get(at).get(att).contains("copyright")){copyrighted=true;}
                        if (cTB.get(at).get(att).contains("All Rights Reserved")){copyrighted=true;}

                    }
                    //System.out.println("WC: "+ Tv_wordcount);

                    //rule 1
                    if (Tv_wordcount>TC_threshold){
                        //rule 2 link density should be < 0.5
                        if ((link_wordcount/(link_wordcount+Tv_wordcount))<0.5){
                            //rule 3
                            if (Ll_number/cTB.get(at).size()<0.5){
                                //rule 4  rewerite
                                if (!copyrighted){
                                    //System.out.println("Adddddddddddddd");
                                    //System.out.println(cTB.get(at));
                                    verified_textbox.add(cTB.get(at));
                                }
                            }
                        }
                    }





                }
                //System.out.println("4 done");
                //System.out.println("vtb size: " + verified_textbox.size());


                /*End of Algorithm 4*/

                //System.out.println("This is the link I am visiting right now "+link);
                String text = link.replaceAll("https://","");
                text = text.replaceAll("http://","");
                text = text.replaceAll("/","*");
                String ftext= text + ".txt";
                if (!TimeStamps.isEmpty() || verified_textbox.size()>= 2 ){
                FileWriter fw = new FileWriter(new File("collected_data", ftext));

                /*for (String tStamp: TimeStamps) {
                    fw.write(tStamp);
                    fw.write("\n");
                }*/

                fw.write(max_date.getTime().toString());
                fw.write("\n");
                fw.write(min_date.getTime().toString());
                fw.write("\n");

                for (ArrayList a:verified_textbox) {
                    //System.out.println("***********************************************************************");
                    String cline;
                    for (Object s:a) {
                        cline = s.toString().replaceAll("<.*?>","");
                        fw.write(cline);
                        fw.write("\n");
                    }
                }


                fw.close();}

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
                        System.out.println("new links:"+dlink);
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

        private static int getMonth (String input ){
            String month = input.toUpperCase();
            int month_int;
            switch (month) {
                case "JAN":  month_int = 1;
                    break;
                case "FEB":  month_int = 2;
                    break;
                case "MAR":  month_int = 3;
                    break;
                case "APR":  month_int = 4;
                    break;
                case "MAY":  month_int = 5;
                    break;
                case "JUN":  month_int = 6;
                    break;
                case "JUL":  month_int = 7;
                    break;
                case "AUG":  month_int = 8;
                    break;
                case "SEP":  month_int = 9;
                    break;
                case "OCT": month_int = 10;
                    break;
                case "NOV": month_int = 11;
                    break;
                case "DEC": month_int = 12;
                    break;
                default: month_int = 0;
                    break;
            }

            return month_int;
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
