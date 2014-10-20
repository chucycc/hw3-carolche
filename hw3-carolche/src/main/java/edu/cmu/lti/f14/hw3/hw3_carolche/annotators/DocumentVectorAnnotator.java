package edu.cmu.lti.f14.hw3.hw3_carolche.annotators;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.tcas.Annotation;

import edu.cmu.lti.f14.hw3.hw3_carolche.typesystems.Document;
import edu.cmu.lti.f14.hw3.hw3_carolche.typesystems.Token;
import edu.cmu.lti.f14.hw3.hw3_carolche.utils.Utils;

public class DocumentVectorAnnotator extends JCasAnnotator_ImplBase {

  @Override
  public void process(JCas jcas) throws AnalysisEngineProcessException {

    FSIterator<Annotation> iter = jcas.getAnnotationIndex().iterator();
    if (iter.isValid()) {
      iter.moveToNext();
      Document doc = (Document) iter.get();
      createTermFreqVector(jcas, doc);
    }

  }

  /**
   * A basic white-space tokenizer, it deliberately does not split on punctuation!
   *
   * @param doc
   *          input text
   * @return a list of tokens.
   */

  List<String> tokenize0(String doc) {
    List<String> res = new ArrayList<String>();

    for (String s : doc.split("\\s+"))
      res.add(s);
    return res;
  }

  /**
   * Construct a vector of tokens and update the tokenList in CAS.
   * 
   * @param jcas
   * @param doc
   */

  private void createTermFreqVector(JCas jcas, Document doc) {

    String docText = doc.getText();
    // Tokenize the text
    List<String> tokens = tokenize0(docText);
    // Use HashMap to record frequency of tokens
    Map<String, Integer> freqMap = new HashMap<String, Integer>();

    for (String s : tokens) {
      if (!freqMap.containsKey(s)) {
        freqMap.put(s, 1);
      } else {
        Integer value = freqMap.get(s) + 1;
        freqMap.put(s, value);
      }
    }
    // Create a collection(ArrayList) to store Tokens
    List<Token> tokenList = new ArrayList<Token>();
    // Retrieve frequency from freqMap to create Tokens
    for (String s : freqMap.keySet()) {
      Token t = new Token(jcas);
      t.setText(s);
      t.setFrequency(freqMap.get(s));
      tokenList.add(t);
    }
    // Transform ArrayList to FSList
    FSList fsList = Utils.fromCollectionToFSList(jcas, tokenList);
    // Update the tokenList in CAS
    doc.setTokenList(fsList);
  }

}
