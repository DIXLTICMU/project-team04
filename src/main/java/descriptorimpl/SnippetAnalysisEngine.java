package descriptorimpl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.json.JSONArray;
import org.json.JSONObject;

import snippet.SentenceInfo;
import snippet.scoring.adapter.CandidateAnswer;
import snippet.scoring.adapter.CandidateAnswerAdapter;
import snippet.scoring.factory.Similarity;
import snippet.scoring.factory.SimilarityFactory;
import util.TypeConstants;
import util.TypeFactory;
import util.text.TextUtils;
import util.webservice.WebAPIServiceProxy;
import util.webservice.WebAPIServiceProxyFactory;
import document.DocInfo;
import document.scoring.CollectionStatistics;
import document.stemmer.KrovetzStemmer;
import edu.cmu.lti.oaqa.type.input.Question;
import edu.cmu.lti.oaqa.type.retrieval.Document;
import edu.cmu.lti.oaqa.type.retrieval.Passage;

public class SnippetAnalysisEngine extends JCasAnnotator_ImplBase {

  KrovetzStemmer stemmer;
  private WebAPIServiceProxy service;

  @Override
  public void initialize(UimaContext aContext) throws ResourceInitializationException {
    super.initialize(aContext);
    this.service = WebAPIServiceProxyFactory.getInstance();
    stemmer = new KrovetzStemmer();
  }

  @Override
  public void process(JCas aJCas) throws AnalysisEngineProcessException {
    FSIterator<?> qit = aJCas.getAnnotationIndex(Question.type).iterator();
    Question question = null;
    if (qit.hasNext()) {
      question = (Question) qit.next();
      //System.out.println(question.getText());
      System.out.println(question.getId());
      System.out.println(question.getQuestionType());
    }
    CollectionStatistics cStat = new CollectionStatistics();
    ArrayList<SentenceInfo> allSentences = new ArrayList<SentenceInfo>();
    try {
      FSIterator<?> it;
      it = aJCas.getFSIndexRepository().getAllIndexedFS(
              aJCas.getRequiredType("edu.cmu.lti.oaqa.type.retrieval.Document"));
      while (it.hasNext()) {
        Document doc = (Document) it.next();
        if (doc.getSearchId() != null
                && doc.getSearchId().equals(TypeConstants.SEARCH_ID_GOLD_STANDARD)) {
          continue;
        }
        try {
          String pmid = doc.getDocId();
          String uri = doc.getUri();
          JSONObject docFull = service.getDocFullTextJSon(pmid);
          JSONArray sectionArr = docFull.getJSONArray("sections");
          //System.out.println(sectionArr.length());

          Map<String, String> fieldTextMap = new HashMap<String, String>();
          fieldTextMap.put("title", doc.getTitle());
          for (int i = 0; i < sectionArr.length(); i++) {
            fieldTextMap.put("section:" + i, (String) sectionArr.get(i));
          }
          DocInfo docInfo = new DocInfo(uri, pmid, fieldTextMap, null, stemmer);
          cStat.addDoc(docInfo);

          for (int i = 0; i < sectionArr.length(); i++) {
            String section = (String) sectionArr.get(i);
            List<SentenceInfo> sentences = TextUtils.stanfordSentenceTokenizer(section);
            for (SentenceInfo sentence : sentences) {
              sentence.hostDoc = docInfo;
              sentence.sectionIndex = "sections." + i;
              allSentences.add(sentence);
              //System.out.println(sentence);
            }
          }
        } catch (Exception e) {
          // TODO Auto-generated catch block
        }
      }
      cStat.finalize();
    } catch (CASException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    SentenceInfo questionInfo = new SentenceInfo(question.getText(), null, -1, -1, null);
    snippet.scoring.factory.Question query = new snippet.scoring.factory.QuestionAdapter(questionInfo);
    Similarity similarity = SimilarityFactory.getNewSimilarity(SimilarityFactory.weighted);

    for (SentenceInfo sentence : allSentences) {
      CandidateAnswer answer = new CandidateAnswerAdapter(sentence);
      sentence.score = similarity.computeSimilarity(query, answer);
    }
    
    //TODO try different similarity functions
    allSentences = (ArrayList<SentenceInfo>) allSentences.stream()
            .filter(e -> e.score >= 0.0001)
            .sorted((e1, e2) -> Double.compare(e2.score, e1.score))
            .collect(Collectors.toList())
            ;
    /*    
    createPassage(JCas jcas, String uri, double score, String text, int rank,
            String queryString, String searchId, Collection<CandidateAnswerVariant> candidateAnswers,
            String title, String docId, int offsetInBeginSection, int offsetInEndSection,
            String beginSection, String endSection, String aspects);
            */
    
    String _query = question.getText();
    allSentences.stream()
					.map(snippet -> TypeFactory.createPassage(aJCas, snippet.hostDoc.uri, snippet.score.doubleValue(), snippet.content, -1,
					        _query, null, new ArrayList<>(),
					        snippet.hostDoc.fieldTextMap.get("title"), snippet.hostDoc.pmid, snippet.startIndex, snippet.endIndex,
							"" + snippet.sectionIndex, "" + snippet.sectionIndex, TypeConstants.ASPECTS_UNKNOWN))
					.forEachOrdered(Passage::addToIndexes);
  }

  @Override
  public void collectionProcessComplete() throws AnalysisEngineProcessException {
    // TODO Auto-generated method stub
    super.collectionProcessComplete();
  }

}
