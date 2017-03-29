import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

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

    public static void main (String[] args) {
        System.out.println("Hello Idiots");
        read_seed_list();
    }

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
}
