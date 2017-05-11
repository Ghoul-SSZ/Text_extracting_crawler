import java.util.ArrayList;
import java.util.BitSet;

/**
 * Created by szhou on 5/10/17.
 */
public class BloomFilter {
    public static ArrayList<BitSet> MLBF = new ArrayList<BitSet>(); // keeps all BF in a list, layer is defined by the list cell position.

    // h(x)= (ax+b)%c
    private static long sPrime = 4294967311L;  //Slightly larger prime than max 32bit number -> C
    static int L = 5;
    static int K = 20;

    public static synchronized ArrayList<BitSet> getMLBF(){return MLBF;}

    public static synchronized void bloom_filter_insert(String link, ArrayList<Integer> coffA, ArrayList<Integer> coffB){
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

    public static boolean bloom_filter_query(String link, ArrayList<Integer> coffA, ArrayList<Integer> coffB){
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

    private static String [] link_to_layers (String link){
        link = link.replaceAll("http://","");
        link = link.replaceAll("https://","");
        return link.split("/");
    }

}
