package MTM;

import java.util.ArrayList;

public class MultiTerm{
    ArrayList<Integer> words;
    int word_z;

    public static void main(String[] args){

    }

    public MultiTerm(Integer[] words){
        this.words = new ArrayList<Integer>();
        for(int i=0; i<words.length; i++){
            this.words.add(words[i]);
        }
        word_z = -1;
    }

    public ArrayList<Integer> get_words(){
        return words;
    }

    public void set_topic(int z){
        word_z = z;
    }

    public int get_topic(){
        return word_z;
    }

    public void reset_topic(){
        word_z = -1;
    }
}