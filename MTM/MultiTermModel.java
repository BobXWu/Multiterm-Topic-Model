package MTM;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collections;
import java.util.Random;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.Math;

import MTM.MultiTerm;

public class MultiTermModel{

    public ArrayList<MultiTerm> multiTerms;
    public ArrayList<ArrayList<Integer>> multiTerms_list;
    public ArrayList<ArrayList<Integer>> mit_id_text;
    public ArrayList<ArrayList<ArrayList<Integer>>> text_multiTerms;
    public int topic_num;
    public int top_words_num;
    public int iter_times;
    public String input_dir;
    public String output_dir;
    public double alpha;
    public double beta;
    public int n_iter;
    public int text_size;
    public int voca_size;
    public int multiTerm_size;
    public int multiTerm_number;
    public int[] n_z;
    public int[][] nwz;
    public int[] n_sumw_z;
    public ArrayList<HashMap<Integer, Integer>> cache_count;
    public HashMap<Integer, String> word_index;

    double[] p_z;
    double[][] pz_d;
    double[][] pz_m;
    double[][] pw_z;

    private Random random;

    public MultiTermModel(
        int topic_num,
        int top_words_num,
        String input_dir,
        String output_dir,
        double alpha,
        double beta,
        int iter_times
    ){
        this.multiTerms = new ArrayList<MultiTerm>();
        this.multiTerms_list = new ArrayList<ArrayList<Integer>>();
        this.mit_id_text = new ArrayList<ArrayList<Integer>>();
        this.text_multiTerms = new ArrayList<ArrayList<ArrayList<Integer>>>();
        this.topic_num = topic_num;
        this.iter_times = iter_times;
        this.input_dir = input_dir;
        this.output_dir = output_dir;
        this.alpha = alpha;
        this.beta = beta;
        this.n_iter = 1;
        this.top_words_num = top_words_num;
        word_index = new HashMap<Integer, String>();
        random = new Random();
    }

    public static void main(String[] args){

        int topic_num = Integer.parseInt(args[0]);
        String input_dir = args[1];
        String output_dir = args[2];
        double alpha = Double.parseDouble(args[3]);
        double beta = Double.parseDouble(args[4]);
        int iter_times = 500;
        if(args.length >= 6){
            iter_times = Integer.parseInt(args[5]);
        }

        int top_words_num = 20;

        System.out.println("alpha: " + alpha);
        System.out.println("beta: " + beta);

        MultiTermModel model = new MultiTermModel(topic_num, top_words_num, input_dir, output_dir, alpha, beta, iter_times);
        try{
            model.init_model();
        }catch(IOException e){
            e.printStackTrace();
        }
        model.run_model();
        model.save_pw_z();
        model.save_pz_d();
    }

    private static long getCurrTime() {
        return System.currentTimeMillis();
    }

    public void run_model(){
        System.out.println("\nBegin iteration...");

        while(n_iter <= iter_times){
            System.out.print("\r" + n_iter + "/" + iter_times);
            for(MultiTerm mit : multiTerms){
                update_multiTerm(mit);
            }
            n_iter++;
        }
        System.out.println();

    }

    public void update_multiTerm(MultiTerm mit){
        reset_multiTerm(mit);
        double[] pz = new double[topic_num];
        compute_pz(mit, pz);
        int topic_id = mult_sample(pz);
        assign_multiTerm_topic(mit, topic_id);
    }


    public void compute_pz(MultiTerm mit, double[] pz){
        ArrayList<Integer> words = mit.get_words();
        int word_num = words.size();
        double[] pwk = new double[word_num];
        
        // nwz, n_z and n_sumw_z have already been updated in reset_multiterm(), so we don't need to exclude the mit in them.
        for(int k=0; k<topic_num; k++){
            for(int l=0; l<word_num; l++){
                pwk[l] = (nwz[k][words.get(l)] + beta) / (n_sumw_z[k] + voca_size * beta + l);
            }
            pz[k] = (n_z[k] + alpha) / (multiTerm_size + topic_num * alpha);
            for(int i=0; i<pwk.length; i++){
                pz[k] *= pwk[i];
            }
        }
    }


    public int mult_sample(double[] pz){
        for(int i=1; i<topic_num; i++){
            pz[i] += pz[i-1];
        }
        double u = random.nextDouble();
        int k = 0;
        for(; k<topic_num; k++){
            if(pz[k] >= u * pz[topic_num - 1]){
                break;
            }
        }
        if(k == topic_num){
            k--;
        }
        return k;
    }


    public void reset_multiTerm(MultiTerm mit){
        int k = mit.get_topic();
        ArrayList<Integer> words = mit.get_words();
        n_z[k]--;
        n_sumw_z[k] -= words.size();
        for(int i=0; i<words.size(); i++){
            nwz[k][words.get(i)]--;
        }
        mit.reset_topic();
    }


    public MultiTerm build_multiTerm(Integer[] words){
        return new MultiTerm(words);
    }


    public void assign_multiTerm_topic(MultiTerm mit, int topic_id){
        ArrayList<Integer> words = mit.get_words();
        mit.set_topic(topic_id);
        n_z[topic_id]++;
        n_sumw_z[topic_id] += words.size();
        for(int i=0; i<words.size(); i++){
            nwz[topic_id][words.get(i)]++;
        }
    }


    public void init_model() throws IOException{

        BufferedReader reader;
        String line;
        try{
            reader = new BufferedReader(new FileReader(input_dir + "multiTerms"));
            while((line = reader.readLine()) != null){
                String[] words = line.split("\\s+");
                Integer[] words_Integer = new Integer[words.length];
                for(int i=0; i<words.length; i++){
                    words_Integer[i] = Integer.parseInt(words[i]);
                }
                multiTerms.add(build_multiTerm(words_Integer));
            }

            multiTerm_size = multiTerms.size();
        }catch(IOException e) {
            e.printStackTrace();
        }

        try{
            reader = new BufferedReader(new FileReader(input_dir + "multiTerms_list"));
            while((line = reader.readLine()) != null){
                String[] words = line.split("\\s+");
                ArrayList<Integer> mit = new ArrayList<Integer>();
                for(int i=0; i<words.length; i++){
                    mit.add(Integer.parseInt(words[i]));
                }
                multiTerms_list.add(mit);
            }

            multiTerm_number = multiTerms_list.size();
        }catch(IOException e) {
            e.printStackTrace();
        }


        try{
            reader = new BufferedReader(new FileReader(input_dir + "transformed_multiTerm_texts"));
            while((line = reader.readLine()) != null){
                String[] mit_strings = line.split(",");
                ArrayList<ArrayList<Integer>> text_mit = new ArrayList<ArrayList<Integer>>();
                for(int i=0; i < mit_strings.length; i++){
                    String[] words = mit_strings[i].split("\\s+");
                    ArrayList<Integer> mit = new ArrayList<Integer>();
                    for(int j=0; j<words.length; j++){
                        mit.add(Integer.parseInt(words[j]));
                    }
                    text_mit.add(mit);
                }
                text_multiTerms.add(text_mit);
            }
        }catch(IOException e) {
            e.printStackTrace();
        }

        try{
            reader = new BufferedReader(new FileReader(input_dir + "word_index.txt"));
            while((line = reader.readLine()) != null){
                String[] items = line.split("\\s+");
                word_index.put(Integer.parseInt(items[0]), items[1]);
        }
        }catch(IOException e) {
            e.printStackTrace();
        }

        voca_size = word_index.size();
        text_size = text_multiTerms.size();
        n_z = new int[topic_num];
        nwz = new int[topic_num][voca_size];
        n_sumw_z = new int[topic_num];

        try{
            reader = new BufferedReader(new FileReader(input_dir + "mit_id_text"));
            while((line = reader.readLine()) != null){
                String[] ids = line.split("\\s+");
                ArrayList<Integer> mit_ids = new ArrayList<Integer>();
                for(int i=0; i<ids.length; i++){
                    mit_ids.add(Integer.parseInt(ids[i]));
                }
                mit_id_text.add(mit_ids);
            }
        }catch(IOException e) {
            e.printStackTrace();
        }


        for(MultiTerm mit : multiTerms){
            int k = random.nextInt(topic_num);
            assign_multiTerm_topic(mit, k);
        }
    }


    public void save_pw_z(){
        System.out.println("\nsave_pw_z");

        ArrayList<String> top_topics = new ArrayList<String>();
        ArrayList<String> top_topics_words = new ArrayList<String>();

        for(int k=0; k<topic_num; k++){
            System.out.print("\r" + (k+1) + "/" + topic_num);

            ArrayList<Integer> word_dis = new ArrayList<Integer>();
            for(int i=0; i<voca_size; i++){
                word_dis.add(i);
            }

            int[] nw = nwz[k];
            Collections.sort(word_dis, (w1, w2) -> nw[w2] - nw[w1]);

            String[] top_topics_k = new String[top_words_num];
            String[] top_topics_words_k = new String[top_words_num];
            for(int i=0; i < top_words_num; i++){
                top_topics_words_k[i] = word_index.get(word_dis.get(i));
                top_topics_k[i] = word_dis.get(i).toString();
            }

            top_topics.add(String.join(" ", top_topics_k) + "\n");
            top_topics_words.add(String.join(" ", top_topics_words_k) + "\n");
        }

        try {
             FileWriter writer = new FileWriter(output_dir + "top_topics");
             writer.write(String.join("", top_topics));
             writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
             FileWriter writer = new FileWriter(output_dir + "top_topics_words");
             writer.write(String.join("", top_topics_words));
             writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void save_pz_d(){
        System.out.println("\nsave_pz_d");

        pw_z = new double[topic_num][voca_size];
        pz_d = new double[text_size][topic_num];
        p_z = new double[topic_num];
        pz_m = new double[multiTerm_number][topic_num];

        for(int k=0; k<topic_num; k++){
            p_z[k] = (n_z[k] + alpha) / (multiTerm_size + topic_num * alpha);

            for(int w=0; w<voca_size; w++){
                pw_z[k][w] = (nwz[k][w] + beta) / (2*n_z[k] + voca_size * beta);
            }
        }

        infer_sum_multiTerm();

        try {
             FileWriter writer = new FileWriter(output_dir + "pz_d");
             for(int t=0; t<text_size; t++){
                String[] pz_str = new String[topic_num];
                for(int k=0; k<topic_num; k++){
                    pz_str[k] = String.valueOf(pz_d[t][k]);
                }
                writer.write(String.join(" ", pz_str) + "\n");
             }
             writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void infer_sum_multiTerm(){

        System.out.println("\ninfer_sum_multiTerm");

        for(int m=0; m<multiTerm_number; m++){
            double pz_m_sum = 0.0;
            ArrayList<Integer> word_ids = multiTerms_list.get(m);
            for(int k=0; k<topic_num; k++){
                pz_m[m][k] = p_z[k];
                for(Integer w : word_ids){
                    pz_m[m][k] *= pw_z[k][w];
                }
                pz_m_sum += pz_m[m][k];
            }
            for(int k=0; k<topic_num; k++){
                pz_m[m][k] /= pz_m_sum;
            }
        }

        for(int t=0; t<text_size; t++){
            System.out.print("\r" + (t+1) + "/" + text_size);
            ArrayList<Integer> id_list = mit_id_text.get(t);
            for(int k=0; k<topic_num; k++){
                for(int i=0; i<id_list.size(); i++){
                    pz_d[t][k] += pz_m[ id_list.get(i) ][k];
                }
                pz_d[t][k] /= id_list.size();
            }
        }
        System.out.println();
    }

}
