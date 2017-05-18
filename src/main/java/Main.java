

import sun.management.snmp.jvminstr.JvmThreadInstanceEntryImpl;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.*;


/**
 * Created by szhou on 3/29/17.
 */
/*
*Includes:
*Multi-layer Bloom Filter
*Seed-List
*Crawl List (lvl.1)
*Crawl List (lvl.2)
**/
public class Main {

    public LinkedList <String> crawler_list_lvl1 = new LinkedList<String>(); //Needs to use Collect.synchronizedlist
    public LinkedList <String> crawler_list_lvl2 = new LinkedList<String>(); // After Filtered by Bloom-filt
    public static ConcurrentLinkedQueue <String> bag_of_tasks = new ConcurrentLinkedQueue<String>();
    private static ArrayList <String> seed_list = new ArrayList<String>();
    private static ArrayList<BitSet> MLBF = new ArrayList<BitSet>(); // keeps all BF in a list, layer is defined by the list cell position.

    //parameter section
    private static int L = 15;
    private static int K = 20;
    private static int num_of_workers = 4;
    //static int pageCount = 0;

    //end of parameter section

    public static void main (String[] args) throws InterruptedException {
        long startTime = System.currentTimeMillis();

        System.out.println("Hello Idiots");
        read_seed_list();


        ArrayList <Integer> coffA = genRCoff(K);
        ArrayList <Integer> coffB = genRCoff(K);

        //Bit arrays for Bloom Filter
        //n = 1,000,000, p = 1.0E-6 (1 in 1,000,000) → m = 28,755,176 (3.43MB), k = 20
        for(int i = 0; i<=L; i++){
            BitSet BFs = new BitSet(28755176);
            BloomFilter.MLBF.add(BFs);
        }

        for (String link: seed_list) {
            bag_of_tasks.add(link);
        }
        //create_workers(num_of_workers,coffA,coffB);
        //ThreadPoolExecutor regulator = new ThreadPoolExecutor(1,num_of_workers,Long.MAX_VALUE, TimeUnit.NANOSECONDS,);
        ExecutorService regulator = Executors.newFixedThreadPool(num_of_workers);

        /*
        while (!bag_of_taks.isEmpty()){
            System.out.println("worker created");
            System.out.println("links remain: " + bag_of_taks.toString() );
            regulator.submit(new Worker(coffA,coffB));

        }
        */

        boolean not_done = true;
        while(not_done){
            long tempTime  = System.currentTimeMillis();

            //pageCount++;
            if ((tempTime-startTime)>300000){
                System.out.println("time is up: 5 mins");
                System.exit(0);
                regulator.shutdown();
            }else if (!bag_of_tasks.isEmpty()){
                regulator.submit(new Worker(coffA,coffB,bag_of_tasks.poll()));
            }else if(bag_of_tasks.isEmpty() &&  ((ThreadPoolExecutor) regulator).getActiveCount()>2){
                Thread.sleep(1000);
                //System.out.println("number of threads active:"+Thread.activeCount());
            } else{
                not_done=false;
                System.out.println("false");
            }
        }


        if(bag_of_tasks.isEmpty()){System.out.print("You are out of work to do!!");}

        regulator.shutdown();

        long endTime   = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        System.out.println("total time: "+ totalTime);
    }

   // private static void create_workers(int num_of_workers, ArrayList coffA, ArrayList coffB){
   //     for (int i = 0; i <= num_of_workers; i++){
   //         Thread t = new Thread(new Worker(coffA,coffB));  // needs to fix worker class, also naming the worker thread?
   //     }

   //    }


    //Convert the text file into an Arraylist for further processing
    private static void read_seed_list(){
        String filename = "seed-list.csv";
        try{
            BufferedReader br = new BufferedReader(new FileReader(filename));
            String line;
            int i = 0;
            while((line = br.readLine()) != null){
                seed_list.add(line);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static ArrayList<Integer> genRCoff (int n) {
        int max = 2147483647;
        ArrayList <Integer> coff = new ArrayList<Integer>();
        while (n>0) {
            Random r = new Random();
            int a = r.nextInt(max);
            while (coff.contains(a) || a<0) {
                a = r.nextInt(max);
            }

            coff.add(a);
            n--;
        }
        //System.out.println(coff);
        return coff;
    }
}
