# Code for Short Text Topic Modeling with Flexible Word Patterns

## Usage

### 1. prepare multiterms

```
    python preprocess_multiTerm.py --data_path {data_path} --output_dir {output_dir}
```

``` data_path ``` is the path of short texts in the form of

```
    word1 word2...
    ...
```

such as

```
    python preprocess_multiTerm.py --data_path data/stackoverflow --output_dir output/stackoverflow
```

Please NOTE that the words in each short text should keep the original order.

After running, these files will be outputted in ```output_dir```:

- **multiTerms**

word_id of each multiterm.

```
    word_id word_id...
    ...
```

- **multiTerms_list**

word_id of each distinct multiterm.

```
    word_id word_id...
    ...
```

- **transformed_multiTerm_texts**

Multierms of each text. Each multiterm is made up of word ids.

``` 
    word_id word_id, word_id word_id, ...
    ...
```

- **word_index.txt**

``` 
    word_id word 
    ...
```

- **mit_id_text**

``` 
    mit_id mit_id mit_id ... 
```


### 2. run MTM

```
    java MTM.MultiTermModel {topic_num} {input_path} {output_path} {alpha} {beta} {iteration times}
```

```input_path``` is the path including the output files of ``` preprocess_multiterm.py ```.

such as

```
    java MTM.MultiTermModel 20 output/stackoverflow/ output/stackoverflow/topic_20/ 2.0 0.08 500
```

The following files will be outputted in ```output_path```:

- **top_topics**: word ids sorted by p(w|z).

- **top_topics_words**: words sorted by p(w|z).

- **pz_d**: topic distributions of each text.


Then you can evaluate the topic words with the [coherence score](https://github.com/dice-group/Palmetto).
An example of coherence score output log can be found in ``` output/stackoverflow/stackoverflow_K20 ```.


## Citation

If you want to use our code, please cite as

```
    @inproceedings{Wu2019,
        author = {Wu, Xiaobao and Li, Chunping},
        booktitle = {International Joint Conference on Neural Networks},
        title = {{Short Text Topic Modeling with Flexible Word Patterns}},
        year = {2019}
    }
```
