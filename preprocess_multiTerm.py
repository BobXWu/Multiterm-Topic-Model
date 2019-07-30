import os
import codecs
import argparse
import regex
import numpy as np
import nltk
from nltk.stem import WordNetLemmatizer
from nltk.corpus import stopwords
from collections import Counter
import json


parser = argparse.ArgumentParser()
parser.add_argument('--data_path', default="data/Tweet")
parser.add_argument('--output_dir', default="data")
args = parser.parse_args()

lemmatizer = WordNetLemmatizer()
stopwords_list = list(set(stopwords.words('english')))


def load_data(data_path):
    texts = list()
    with open(data_path) as file:
        for line in file:
            texts.append(line)
    return texts


NP_pattern = """
NP: {<JJ>*<NN.*>+}
"""


NN_pattern = """
NN: {<NN.*>+}
"""

NP_parser = nltk.RegexpParser(NP_pattern)
NN_parser = nltk.RegexpParser(NN_pattern)

def get_text_multiTerm(text):
    words = text.split()
    tag_words = nltk.pos_tag(words)

    NP_result = NP_parser.parse(tag_words)
    NN_result = NN_parser.parse(tag_words)

    multiTerm_list = list()
    not_noun_phrase = list()
    not_noun_phrase_list = list()
    noun_phrase_list = list()
    only_noun_phrase_list = list()

    for subtree in NP_result:
        if type(subtree) == nltk.tree.Tree:
            if not_noun_phrase:
                not_noun_phrase_list.append(' '.join(not_noun_phrase))
                not_noun_phrase = list()

            if subtree.label() == 'NP':
                term = ' '.join([w for w, pos in subtree.leaves()])
                if term:
                    noun_phrase_list.append(term)
        else:
            not_noun_phrase.append(subtree[0])

    for subtree in NN_result.subtrees():
        if subtree.label() == 'NN':
            term = ' '.join([w for w, pos in subtree.leaves()])
            if term:
                only_noun_phrase_list.append(term)

    if not_noun_phrase:
        not_noun_phrase_list.append(' '.join(not_noun_phrase))

    for mit in noun_phrase_list:
        multiTerm_list.append(mit)

    for mit in not_noun_phrase_list:
        multiTerm_list.append(mit)

    noun_phrase_size = len(noun_phrase_list)
    for i in range(noun_phrase_size - 2):
        multiTerm_list.append("{} {}".format(noun_phrase_list[i], noun_phrase_list[i+1]))

    only_noun_phrase_words = " ".join(only_noun_phrase_list).split()
    words_len = len(only_noun_phrase_words)
    for i in range(words_len):
        for j in range(i+1, words_len):
            multiTerm_list.append("{} {}".format(only_noun_phrase_words[i], only_noun_phrase_words[j]))

    return multiTerm_list


def preprocess_data():
    # load data
    texts = load_data(args.data_path)

    # counter for multiTerms
    counter = Counter()
    words = list()
    transformed_multiTerm_texts = list()
    all_multiTerm_list = list()
    all_length = len(texts)

    for i in range(all_length):

        print("{}/{}".format(i+1, all_length), end='\r')

        text_multiTerms = get_text_multiTerm(texts[i])

        transformed_multiTerm_texts.append(','.join(text_multiTerms))

        all_multiTerm_list.extend(text_multiTerms)

        counter.update(text_multiTerms)

    print("\nsaving files...")

    multiTerm_counter_dict = dict(counter)
    multiTerm_list = list(counter.keys())
    multiTerm_size = len(multiTerm_list)
    multiTerm_index = dict(zip(multiTerm_list, range(multiTerm_size)))

    # sort multiTerm_list according to the id
    # multiTerm_list.sort(key=lambda mit: multiTerm_index[mit])

    multiTerm_number = list()
    for mit in multiTerm_list:
        multiTerm_number.append(str(multiTerm_counter_dict[mit]))

    # get all words in muliTerms
    for mit in multiTerm_list:
        words += mit.split()

    words = list(set(words))
    voca_size = len(words)
    word_dict = dict(zip(words, range(voca_size)))

    with open(os.path.join(args.output_dir, 'multiTerm_number'), 'w') as file:
        file.write('\n'.join(multiTerm_number))

    with open(os.path.join(args.output_dir, "multiTerms_words"), 'w') as f:
        f.write('\n'.join(all_multiTerm_list))

    all_multiTerm_list = list(map(
        lambda mit: ' '.join([str(word_dict[word]) for word in mit.split()]),
        all_multiTerm_list
    ))

    multiTerm_list = list(map(
        lambda mit: ' '.join([str(word_dict[word]) for word in mit.split()]),
        multiTerm_list
    ))

    mit_id_text = list(map(
        lambda text: ' '.join(str(multiTerm_index[mit]) for mit in text.split(',')),
        transformed_multiTerm_texts
    ))

    transformed_multiTerm_texts = list(map(
        lambda text: ','.join([' '.join([str(word_dict[word]) for word in mit.split()]) for mit in text.split(',')]),
        transformed_multiTerm_texts
    ))


    word_index = dict()
    for key in word_dict:
        word_index[word_dict[key]] = key

    with open(os.path.join(args.output_dir, "multiTerms"), 'w') as f:
        f.write('\n'.join(all_multiTerm_list))

    with open(os.path.join(args.output_dir, "multiTerms_list"), 'w') as f:
        f.write('\n'.join(multiTerm_list))

    with open(os.path.join(args.output_dir, "mit_id_text"), 'w') as f:
        f.write('\n'.join(mit_id_text))

    with open(os.path.join(args.output_dir, "transformed_multiTerm_texts"), 'w') as f:
        f.write('\n'.join(transformed_multiTerm_texts))

    word_index_string = list()
    for key in word_index:
        word_index_string.append('{} {}\n'.format(key, word_index[key]))

    with open(os.path.join(args.output_dir, 'word_index.txt'), 'w') as file:
        file.write(''.join(word_index_string))

    print("done.")


if __name__ == "__main__":
    preprocess_data()
