import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.Random;

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
    public LinkedList <String> crawler_list_lvl2 = new LinkedList<String>(); // After Filtered by Bloom-filter
    private static ArrayList <String> seed_list = new ArrayList<String>();
    public static ArrayList<BitSet> MLBF = new ArrayList<BitSet>(); // keeps all BF in a list, layer is defined by the list cell position.


    //parameter section
    public static int L = 5;
    public static int K = 20;
    public static int num_of_workers = 5;
    //end of parameter section

    public static void main (String[] args) {
        System.out.println("Hello Idiots");
        read_seed_list();

        create_workers(num_of_workers);

        //Bit arrays for Bloom Filter
        //n = 1,000,000, p = 1.0E-6 (1 in 1,000,000) → m = 28,755,176 (3.43MB), k = 20
        for(int i = 0; i<=L; i++){
            BitSet BFs = new BitSet(28755176);
            MLBF.add(BFs);
        }

        String [] test = link_to_layers("https://www.123.com/abc/kk/22");
        for (String s:test) {System.out.println(s);}

        ArrayList <Integer> coffA = genRCoff(K);
        ArrayList <Integer> coffB = genRCoff(K);


    }

    private static void create_workers(int num_of_workers){
        for (int i = 0; i <= num_of_workers; i++){
            Thread t = new Thread(new Worker());  // needs to fix worker class, also naming the worker thread?
            t.start();
        }

    }

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
            System.out.println(seed_list);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void bloom_filter_insert(String link, ArrayList<Integer> coffA, ArrayList<Integer> coffB){
        String[] a = link_to_layers(link);  //needs to be smaller than L
        long Address;
        int[] LAddr = new int[K];
        for (int c=0; c<L; c++){
            for (int j=0; j<K;j++){
                // hash and flip the bit
                // h(x)= (ax+b)%c
                Address = (coffA.get(j)*a[c].hashCode()+coffB.get(j))%sPrime;
                MLBF.get(c).set((int) Address);

                //LAddr[j] = LAddr[j] xor Addr
                LAddr[j] = LAddr[j] ^ (int) Address;

            }
        }
        for (int j=0 ;j<K; j++){
            MLBF.get(L).set(LAddr[j]);
        }
    }

    private static boolean bloom_filter_query(String link, ArrayList<Integer> coffA, ArrayList<Integer> coffB){
        String[] a = link_to_layers(link);
        long Address;
        int[] LAddr = new int[K];
        for (int i=0; i<L; i++){
            for (int j=0; j<K; j++){
                Address = (coffA.get(j)*a[i].hashCode()+coffB.get(j))%sPrime;
                if (!MLBF.get(i).get((int) Address)){return false;}
                //LAddr[j] = LAddr[j] xor Addr
                LAddr[j] = LAddr[j] ^ (int) Address;
            }
        }

        for (int j=0; j<K; j++){
            if (!MLBF.get(L).get(LAddr[j])){
                return false;
            }
        }

        return true;
    }

    public static String [] link_to_layers (String link){
        link = link.replaceAll("http://","");
        link = link.replaceAll("https://","");
        String [] link_layers = link.split("/");
        return link_layers;
    }


    // h(x)= (ax+b)%c
    static long sPrime = 4294967311L;  //Slightly larger prime than max 32bit number -> C
    static int max = 2147483647;
    public static ArrayList<Integer> genRCoff (int n) {
        ArrayList <Integer> coff = new ArrayList<Integer>();
        while (n>0) {
            Random r = new Random();
            int a = r.nextInt(max);
            while (coff.contains(a)) {
                a = r.nextInt(max);
            }

            coff.add(a);
            n--;
        }
        //System.out.println(coff);
        return coff;
    }



}
