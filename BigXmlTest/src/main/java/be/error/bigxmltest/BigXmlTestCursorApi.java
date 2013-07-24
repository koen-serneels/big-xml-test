package be.error.bigxmltest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.Transformer;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stax.StAXSource;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLStreamReader2;
import org.codehaus.stax2.validation.ValidationProblemHandler;
import org.codehaus.stax2.validation.XMLValidationException;
import org.codehaus.stax2.validation.XMLValidationProblem;
import org.codehaus.stax2.validation.XMLValidationSchema;
import org.codehaus.stax2.validation.XMLValidationSchemaFactory;

import com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl;

public class BigXmlTestCursorApi {

	private static final String DOCUMENT_NS = "http://www.error.be/bigxmltest";
	private static final String BIGXMLTEST_ROOT_ELEMENT = "BigXmlTest";
	private static final String HEADER_ELEMENT = "Header";
	private static final String CONTENT_ELEMENT = "Content";
	private static final String DATA_ELEMENT = "Data";

	private final ObjectFactory objectFactory = new ObjectFactory();
	private final Marshaller marshaller;
	private final Transformer transformer;

	public static void main(String args[]) throws Exception {
		BigXmlTestCursorApi bigXmlTest = new BigXmlTestCursorApi();
		bigXmlTest.startSplitting();
	}

	{
		try {
			// Setup the sun internal (very important) transformer
			transformer = new TransformerFactoryImpl().newTransformer();

			// Setup JAXB
			marshaller = JAXBContext.newInstance("be.error.bigxmltest").createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void startSplitting() throws Exception {
		XMLStreamReader2 xmlStreamReader = ((XMLInputFactory2) XMLInputFactory.newInstance())
				.createXMLStreamReader(BigXmlTestCursorApi.class.getResource("/BigXmlTest.xml"));
		PrintWriter validationResults = enableValidationHandling(xmlStreamReader);

		int fileNumber = 0;
		int dataRepetitions = 0;
		XMLStreamWriter xmlStreamWriter = openOutputFileAndWriteHeader(++fileNumber); // Prepare first file

		try {
			while (xmlStreamReader.hasNext()) {
				xmlStreamReader.next();

				if (xmlStreamReader.getEventType() == XMLEvent.START_ELEMENT
						&& xmlStreamReader.getLocalName().equals(DATA_ELEMENT)) {

					if (dataRepetitions != 0 && dataRepetitions % 2 == 0) { // %2 = just for testing: replace this by for example checking the actual size of
																			// the current output file
						xmlStreamWriter.close(); // Also closes any open Element(s) and the document
						xmlStreamWriter = openOutputFileAndWriteHeader(++fileNumber); // Continue with next file
						dataRepetitions = 0;
					}
					// Transform the input stream at current position to the output stream
					transformer.transform(new StAXSource(xmlStreamReader), new StAXResult(
							new FragmentXMLStreamWriterWrapper(new AvoidDefaultNsPrefixStreamWriterWrapper(
									xmlStreamWriter, DOCUMENT_NS))));
					dataRepetitions++;
				}
			}
		} finally {
			xmlStreamReader.close();
			xmlStreamWriter.close();
			validationResults.close();
		}
	}

	private XMLStreamWriter openOutputFileAndWriteHeader(int fileNumber) throws Exception {
		XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
		xmlOutputFactory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, true);
		XMLStreamWriter writer = xmlOutputFactory.createXMLStreamWriter(new FileOutputStream(new File(System
				.getProperty("java.io.tmpdir"), "BigXmlTest." + fileNumber + ".xml")));
		writer.setDefaultNamespace(DOCUMENT_NS);
		writer.writeStartDocument();
		writer.writeStartElement(DOCUMENT_NS, BIGXMLTEST_ROOT_ELEMENT);
		writer.writeDefaultNamespace(DOCUMENT_NS);

		HeaderType header = objectFactory.createHeaderType();
		header.setSomeHeaderElement("Something something darkside");
		marshaller.marshal(new JAXBElement<HeaderType>(new QName(DOCUMENT_NS, HEADER_ELEMENT, ""), HeaderType.class,
				HeaderType.class, header), writer);

		writer.writeStartElement(CONTENT_ELEMENT);
		return writer;
	}

	private PrintWriter enableValidationHandling(XMLStreamReader2 xmlStreamReader) throws XMLStreamException,
			IOException {
		XMLValidationSchemaFactory xmlValidationSchemaFactory = XMLValidationSchemaFactory
				.newInstance(XMLValidationSchema.SCHEMA_ID_W3C_SCHEMA);
		XMLValidationSchema xmlValidationSchema = xmlValidationSchemaFactory.createSchema(BigXmlTestCursorApi.class
				.getResource("/BigXmlTest.xsd"));

		final PrintWriter validationResults = new PrintWriter(new BufferedWriter(new FileWriter(new File(
				System.getProperty("java.io.tmpdir"), "ValidationResults.txt"))));

		xmlStreamReader.setValidationProblemHandler(new ValidationProblemHandler() {
			@Override
			public void reportProblem(XMLValidationProblem validationError) throws XMLValidationException {
				validationResults.write(validationError.getMessage()
						+ "Location:"
						+ ToStringBuilder.reflectionToString(validationError.getLocation(),
								ToStringStyle.SHORT_PREFIX_STYLE) + "\r\n");
			}
		});

		xmlStreamReader.validateAgainst(xmlValidationSchema);
		return validationResults;
	}
}
