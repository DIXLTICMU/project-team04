package json;


import java.util.Arrays;
import java.util.List;

import org.apache.uima.jcas.JCas;

import com.google.common.collect.Lists;

import json.gson.Question;
import json.gson.QuestionType;
import json.gson.TestFactoidQuestion;
import json.gson.TestListQuestion;
import json.gson.TestQuestion;
import json.gson.TestSet;
import json.gson.TestSummaryQuestion;
import json.gson.TestYesNoQuestion;
import json.gson.TrainingFactoidQuestion;
import json.gson.TrainingListQuestion;
import json.gson.TrainingQuestion;
import json.gson.TrainingYesNoQuestion;
import edu.cmu.lti.oaqa.type.answer.Answer;
import static java.util.stream.Collectors.toList;
import edu.cmu.lti.oaqa.type.retrieval.ConceptSearchResult;
import edu.cmu.lti.oaqa.type.retrieval.Document;
import edu.cmu.lti.oaqa.type.retrieval.Passage;
import edu.cmu.lti.oaqa.type.retrieval.TripleSearchResult;
import util.TypeConstants;
import util.TypeFactory;

public class JsonCollectionReaderHelper {

	public static void main(String[] args) {
		JsonCollectionReaderHelper jsHelper = new JsonCollectionReaderHelper();
		jsHelper.testRun();
	}

	public void testRun() {
		String filePath = "/BioASQ-SampleData1B.json";
		List<TestQuestion> inputs;
		inputs = Lists.newArrayList();
	/*	InputStream stream = getClass().getResourceAsStream(filePath);
		try {
			System.out.println("stream " + stream.read());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		Object value = filePath;
		if (String.class.isAssignableFrom(value.getClass())) {
			inputs = TestSet
					.load(getClass().getResourceAsStream(
							String.class.cast(value))).stream()
					.collect(toList());
		} else if (String[].class.isAssignableFrom(value.getClass())) {
			inputs = Arrays
					.stream(String[].class.cast(value))
					.flatMap(
							path -> TestSet.load(
									getClass().getResourceAsStream(path))
									.stream()).collect(toList());
		}
		// trim question texts
		inputs.stream()
				.filter(input -> input.getBody() != null)
				.forEach(
						input -> input.setBody(input.getBody().trim()
								.replaceAll("\\s+", " ")));
		System.out.println("concepts");
		System.out.println(inputs.get(0).getConcepts());
	}

	public static void addQuestionToIndex(Question input, JCas jcas) {
		// question text and type are required
		TypeFactory.createQuestion(jcas, input.getId(), TypeConstants.SEARCH_ID_GOLD_STANDARD,
				convertQuestionType(input.getType()), input.getBody())
				.addToIndexes();
		// if documents, snippets, concepts, and triples are found in the input,
		// then add them to CAS
		if (input.getDocuments() != null) {
			input.getDocuments().stream()
					.map(uri -> TypeFactory.createGoldStandardDocument(jcas, uri))
					.forEach(Document::addToIndexes);
		}
		if (input.getSnippets() != null) {
//		  System.out.println("SNIPPETS ARE NOT NULL: " + input.getSnippets());
			input.getSnippets()
					.stream()
					.map(snippet -> TypeFactory.createGoldStandardPassage(jcas,
							snippet.getDocument(), snippet.getText(),
							snippet.getOffsetInBeginSection(),
							snippet.getOffsetInEndSection(),
							snippet.getBeginSection(), snippet.getEndSection()))
					.forEach(Passage::addToIndexes);
		}
		if (input.getConcepts() != null) {
			input.getConcepts()
					.stream()
					.map(concept -> TypeFactory.createGoldStandardConceptSearchResult(jcas,
							TypeFactory.createConcept(jcas, concept), concept))
					.forEach(ConceptSearchResult::addToIndexes);
		}
		if (input.getTriples() != null) {
			input.getTriples()
					.stream()
					.map(triple -> TypeFactory.createGoldStandardTripleSearchResult(jcas,
							TypeFactory.createTriple(jcas, triple.getS(),
									triple.getP(), triple.getO())))
					.forEach(TripleSearchResult::addToIndexes);
		}
		// add answers to CAS index
		if (input instanceof TestQuestion) {
			// test question should not have ideal or exact answers
//		  System.out.println("TEST QUESTION");
		  if (input instanceof TestFactoidQuestion) {
        List<String> answerVariants = ((TestFactoidQuestion) input)
            .getExactAnswer();
        if (answerVariants != null) {
          TypeFactory.createAnswer(jcas, answerVariants)
              .addToIndexes();
        }
      } else if (input instanceof TestListQuestion) {
        List<List<String>> answerVariantsList = ((TestListQuestion) input)
            .getExactAnswer();
        if (answerVariantsList != null) {
          answerVariantsList
              .stream()
              .map(answerVariants -> TypeFactory.createAnswer(
                  jcas, answerVariants))
              .forEach(Answer::addToIndexes);
        }
      } else if (input instanceof TestYesNoQuestion) {
        String answer = ((TestYesNoQuestion) input)
            .getExactAnswer();
        if (answer != null) {
          TypeFactory.createAnswer(jcas, answer).addToIndexes();
        }
      } else if (input instanceof TestSummaryQuestion) {
        // summary questions do not have exact answers
      }
		  
		} else if (input instanceof TrainingQuestion) {
//		  System.out.println("TRAIN QUESTION");
			if (input instanceof TrainingFactoidQuestion) {
				List<String> answerVariants = ((TrainingFactoidQuestion) input)
						.getExactAnswer();
				if (answerVariants != null) {
					TypeFactory.createAnswer(jcas, answerVariants)
							.addToIndexes();
				}
			} else if (input instanceof TrainingListQuestion) {
				List<List<String>> answerVariantsList = ((TrainingListQuestion) input)
						.getExactAnswer();
				if (answerVariantsList != null) {
					answerVariantsList
							.stream()
							.map(answerVariants -> TypeFactory.createAnswer(
									jcas, answerVariants))
							.forEach(Answer::addToIndexes);
				}
			} else if (input instanceof TrainingYesNoQuestion) {
				String answer = ((TrainingYesNoQuestion) input)
						.getExactAnswer();
				if (answer != null) {
					TypeFactory.createAnswer(jcas, answer).addToIndexes();
				}
			} else if (input instanceof TestSummaryQuestion) {
				// summary questions do not have exact answers
			}
		}
	}

	public static String convertQuestionType(QuestionType type) {
		switch (type) {
		case factoid:
			return "FACTOID";
		case list:
			return "LIST";
		case summary:
			return "OPINION";
		case yesno:
			return "YES_NO";
		default:
			return "UNCLASSIFIED";
		}
	}

}
