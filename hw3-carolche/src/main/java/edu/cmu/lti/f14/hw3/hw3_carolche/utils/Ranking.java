package edu.cmu.lti.f14.hw3.hw3_carolche.utils;

/**
 * Ranking class is for making ranking of documents by cosine similarity. In order to create a
 * Ranking object, cosine similarity, relevance and text of documents are needed.
 * 
 * @author Carol Cheng
 * 
 */

public class Ranking implements Comparable<Ranking> {
  private double cosine;

  private Integer rel;

  // in convenience of printing the text of a document
  private String text;

  /**
   * Constructor.
   * 
   * @param cosine
   *          Cosine similarity between a query and a document
   * @param relevance
   *          Binary relevance assessment of a document, 1 = relevant and 0 = irrelevant
   * @param text
   *          Text of a document
   */
  public Ranking(double cosine, Integer relevance, String text) {
    this.cosine = cosine;
    this.rel = relevance;
    this.text = text;
  }

  /**
   * Get cosine similarity between a query and a document
   */
  public double getCosine() {
    return cosine;
  }

  /**
   * Get binary relevance assessment of a document, 1 = relevant and 0 = irrelevant
   */
  public Integer getRel() {
    return rel;
  }

  /**
   * Get text of a document
   */
  public String getText() {
    return text;
  }

  /**
   * Compare a ranking object with another one to make ranking. The method is implemented for using
   * Collections.sort(List<T> aList). The return value depends primarily on cosine similarity and
   * secondly on relevance.
   * 
   * @param otherRanking
   *          Another ranking to compare with
   */
  @Override
  public int compareTo(Ranking otherRanking) {
    if (cosine == otherRanking.getCosine()) { // Tie
      // Rank relevant documents higher than not relevant
      if (rel > otherRanking.getRel()) {
        return -1;
      } else if (rel < otherRanking.getRel()) {
        return 1;
      } else
        return 0;
    } else if (cosine > otherRanking.getCosine()) {
      return -1;
    } else
      return 1; // cosine < otherRanking.getCosine()
  }
}
