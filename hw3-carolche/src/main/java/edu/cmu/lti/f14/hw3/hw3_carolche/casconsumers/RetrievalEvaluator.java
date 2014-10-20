package edu.cmu.lti.f14.hw3.hw3_carolche.casconsumers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import edu.cmu.lti.f14.hw3.hw3_carolche.typesystems.Document;
import edu.cmu.lti.f14.hw3.hw3_carolche.typesystems.Token;
import edu.cmu.lti.f14.hw3.hw3_carolche.utils.Ranking;
import edu.cmu.lti.f14.hw3.hw3_carolche.utils.Utils;

/**
 * RetrievalEvaluator computes the cosine similarity between the query sentence and each of the
 * subsequent document sentence. Then it uses provided relevance assessments, compute a reciprocal
 * rank for each query. Finally, these reciprocal ranks are averaged to obtain the Mean Reciprocal
 * Rank and save the output information to the file report.txt.
 * 
 * @author Carol Cheng
 * 
 */

public class RetrievalEvaluator extends CasConsumer_ImplBase {
  /** Name of configuration parameter of output file path. **/
  public static final String PARAM_OUTPUTDIR = "output";

  /** special marker for a query **/
  public static final Integer QUERY = 99;

  /** special marker for relevant documents **/
  public static final Integer RELEVANT = 1;

  /** query id number **/
  public ArrayList<Integer> qIdList;

  /** sorted query id number **/
  public ArrayList<Integer> sortedQueryId;

  /** query and text relevant values **/
  public ArrayList<Integer> relList;

  /** text of documents **/
  public ArrayList<String> textList;

  /** vectors of documents **/
  public ArrayList<HashMap<String, Integer>> freqList;

  /**
   * The method initializes the fields of the annotator.
   * 
   * @throws ResourceInitializationException
   * 
   */
  public void initialize() throws ResourceInitializationException {

    qIdList = new ArrayList<Integer>();

    sortedQueryId = new ArrayList<Integer>();

    relList = new ArrayList<Integer>();

    textList = new ArrayList<String>();

    freqList = new ArrayList<HashMap<String, Integer>>();
  }

  /**
   * The method gets documents from CAS and save queryId, relevance, text and word frequency of a
   * document to the fields of the annotator.
   * 
   * @throws ResourceProcessException
   * @param aCas
   */
  @Override
  public void processCas(CAS aCas) throws ResourceProcessException {

    JCas jcas;
    try {
      jcas = aCas.getJCas();
    } catch (CASException e) {
      throw new ResourceProcessException(e);
    }
    // Get iterator of documents
    FSIterator it = jcas.getAnnotationIndex(Document.type).iterator();

    if (it.hasNext()) {
      Document doc = (Document) it.next();

      // Retrieve token list from CAS
      FSList fsTokenList = doc.getTokenList();
      // Transform FSList to ArrayList
      ArrayList<Token> tokenList = Utils.fromFSListToCollection(fsTokenList, Token.class);
      // Transform ArrayList of Tokens to HashMap of word frequency
      HashMap<String, Integer> freqMap = new HashMap<String, Integer>();
      for (Token t : tokenList) {
        freqMap.put(t.getText(), t.getFrequency());
      }
      qIdList.add(doc.getQueryID());
      relList.add(doc.getRelevanceValue());
      textList.add(doc.getText());
      freqList.add(freqMap);
    }

  }

  /**
   * The method computes Cosine Similarity, rank the retrieved sentences, compute Mean Reciprocal
   * Rank and output the result.
   * 
   * @throws ResourceProcessException
   *           , IOException
   * @param arg0
   */
  @Override
  public void collectionProcessComplete(ProcessTrace arg0) throws ResourceProcessException,
          IOException {
    super.collectionProcessComplete(arg0);

    // Set up file and FileWriter
    File file = new File(((String) getUimaContext().getConfigParameterValue(PARAM_OUTPUTDIR)));
    FileWriter fw = new FileWriter(file);

    // Create a HashMap to organize the computed result
    HashMap<Integer, ArrayList<Ranking>> result = new HashMap<Integer, ArrayList<Ranking>>();

    Integer qIndex = 0;
    for (int i = 0; i < qIdList.size(); i++) {
      if (relList.get(i) == QUERY) {
        // QUERY
        // qIndex records the index of query sentences
        qIndex = i;
        // Prepare a sorted query id list
        sortedQueryId.add(qIdList.get(i));
        // Prepare a key for saving the result of cosine similarity and raking
        result.put(qIdList.get(i), new ArrayList<Ranking>());
      } else {
        // DOCUMENT
        // Compute the cosine similarity
        double cosine = computeCosineSimilarity(freqList.get(qIndex), freqList.get(i));
        // Save cosine similarity into a Ranking object
        // @see edu.cmu.lti.f14.hw3.hw3_carolche.utils.Ranking
        result.get(qIdList.get(i)).add(new Ranking(cosine, relList.get(i), textList.get(i)));
      }
    }
    // Sort query id list
    Collections.sort(sortedQueryId);

    // Compute the the summation of reciprocal rank
    double reciprocal_rank = 0.0;
    for (int i = 0; i < sortedQueryId.size(); i++) {
      Integer id = sortedQueryId.get(i);
      ArrayList<Ranking> rankings = result.get(id);
      // Sort Ranking objects by cosine similarity
      Collections.sort(rankings);
      // Find the relevant document
      for (int index = 0; index < rankings.size(); index++) {
        Ranking aRanking = rankings.get(index);
        if (aRanking.getRel() == RELEVANT) {
          reciprocal_rank += 1 / (double) (index + 1);
          // Write the info of a relevant document to output file
          String str = String.format("cosine=%.4f\trank=%d\tqid=%d\trel=%d\t%s\n",
                  aRanking.getCosine(), index + 1, id, aRanking.getRel(), aRanking.getText());
          fw.write(str);
          // Reciprocal rank has been computed, so the loop can break
          break;
        }
      }
    }

    // Compute mean reciprocal rank
    int query_num = result.size();
    double metric_mrr = compute_mrr(reciprocal_rank, query_num);
    System.out.println(" (MRR) Mean Reciprocal Rank ::" + metric_mrr);
    // Write MRR to output file and close the file
    fw.write(String.format("MRR=%.4f\n", metric_mrr));
    fw.close();
  }

  /**
   * Compute cosine similarity between a query and a document.
   * 
   * @param queryVector
   *          The term vector of the given query
   * @param docVector
   *          The term vector of the given document
   * @return cosine_similarity
   */
  private double computeCosineSimilarity(Map<String, Integer> queryVector,
          Map<String, Integer> docVector) {
    double cosine_similarity = 0.0;
    double dot_product = 0.0;
    double norm_product = computeMagnitude(queryVector) * computeMagnitude(docVector);
    // Get common tokens
    HashSet<String> commonTokens = getCommonTokens(queryVector, docVector);
    for (String s : commonTokens) {
      dot_product += queryVector.get(s) * docVector.get(s);
    }
    cosine_similarity = dot_product / norm_product;
    return cosine_similarity;
  }

  /**
   * Find common tokens between a query and a document.
   * 
   * @param queryVector
   *          The term vector of the given query
   * @param docVector
   *          The term vector of the given document
   * @return commonTokens
   */
  private HashSet<String> getCommonTokens(Map<String, Integer> queryVector,
          Map<String, Integer> docVector) {
    HashSet<String> commonTokens = new HashSet<String>();
    for (String s : queryVector.keySet()) {
      if (docVector.containsKey(s)) {
        commonTokens.add(s);
      }
    }
    return commonTokens;
  }

  /**
   * Calculate the magnitude of a vector.
   * 
   * @param vector
   *          The given vector
   * @return magnitude
   */
  private double computeMagnitude(Map<String, Integer> vector) {
    double magnitude = 0.0;
    for (Integer i : vector.values()) {
      magnitude += Math.pow(i, 2);
    }
    magnitude = Math.sqrt(magnitude);
    return magnitude;
  }

  /**
   * Compute Mean Reciprocal Rank (MRR) of the text collection
   * 
   * @param reciprocal_rank
   *          The summation of the reciprocal rank of relevant documents
   * @param query_num
   *          The number of queries
   * @return mrr
   */
  private double compute_mrr(double reciprocal_rank, int query_num) {
    double metric_mrr = 0.0;
    metric_mrr = reciprocal_rank / query_num;
    return metric_mrr;
  }
}
