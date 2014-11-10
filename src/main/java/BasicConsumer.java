import java.io.IOException;
import java.util.ArrayList;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import util.TypeConstants;
import edu.cmu.lti.oaqa.type.input.Question;
import edu.cmu.lti.oaqa.type.retrieval.ConceptSearchResult;
import edu.cmu.lti.oaqa.type.retrieval.Document;
import edu.cmu.lti.oaqa.type.retrieval.Passage;
import edu.cmu.lti.oaqa.type.retrieval.TripleSearchResult;
import edu.stanford.nlp.io.EncodingPrintWriter.out;



public class BasicConsumer extends CasConsumer_ImplBase {
  
  public void initialize() throws ResourceInitializationException {
  }


  @Override
  public void processCas(CAS aCas) throws ResourceProcessException {
    JCas aJCas;
    try {
      aJCas = aCas.getJCas();
    } catch (CASException e) {
      throw new ResourceProcessException(e);
    }

    System.out.println(aJCas);
    
    FSIterator<?> qit = aJCas.getAnnotationIndex(Question.type).iterator();
    Question question = null;
    if(qit.hasNext()) {
      question = (Question) qit.next();
      System.out.println(question.getText());
      System.out.println(question.getId());
      System.out.println(question.getQuestionType());
    }
    
    ArrayList<String> documents = new ArrayList<String>();    
    ArrayList<String> gsDocuments = new ArrayList<String>();    
    
    try {
      FSIterator<?> it;
      it = aJCas.getFSIndexRepository().getAllIndexedFS(aJCas.getRequiredType("edu.cmu.lti.oaqa.type.retrieval.Document"));
      while (it.hasNext()) {
        Document doc = (Document) it.next();
        if(doc.getSearchId() != null && doc.getSearchId().equals(TypeConstants.SEARCH_ID_GOLD_STANDARD)) {
          gsDocuments.add(doc.getUri());
        } else {
          documents.add(doc.getUri());
        }
      }
      System.out.println("gsDocuments");
      System.out.println(gsDocuments);
      System.out.println("documents");
      System.out.println(documents);
    } catch (CASException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }   
    
//    try {
//      FSIterator<?> it;
//      it = aJCas.getFSIndexRepository().getAllIndexedFS(aJCas.getRequiredType("edu.cmu.lti.oaqa.type.retrieval.ConceptSearchResult"));
//
//      while (it.hasNext()) {
//        ConceptSearchResult concept = (ConceptSearchResult) it.next();
//        System.out.println(concept);
//      }
//    } catch (CASException e) {
//      // TODO Auto-generated catch block
//      e.printStackTrace();
//    }
    
//    try {
//      FSIterator<?> it;
//      it = aJCas.getFSIndexRepository().getAllIndexedFS(aJCas.getRequiredType("edu.cmu.lti.oaqa.type.retrieval.TripleSearchResult"));
//
//      while (it.hasNext()) {
//        TripleSearchResult triple = (TripleSearchResult) it.next();
//        System.out.println(triple);
//      }
//    } catch (CASException e) {
//      // TODO Auto-generated catch block
//      e.printStackTrace();
//    }
    
//    try {
//      FSIterator<?> it;
//      it = aJCas.getFSIndexRepository().getAllIndexedFS(aJCas.getRequiredType("edu.cmu.lti.oaqa.type.retrieval.Passage"));
//      while (it.hasNext()) {
//        Passage snippet = (Passage) it.next();
//        System.out.println(snippet);
//      }
//    } catch (CASException e) {
//      // TODO Auto-generated catch block
//      e.printStackTrace();
//    }

    System.out.println("---------");

  }
  
  @Override
  public void collectionProcessComplete(ProcessTrace arg0) throws ResourceProcessException,
          IOException {

    
  }

}
