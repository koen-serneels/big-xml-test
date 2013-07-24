package be.error.bigxmltest;

import java.io.File;
import java.io.FileOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.events.XMLEvent;

import org.codehaus.stax2.XMLInputFactory2;

import com.sun.xml.internal.stream.events.StartElementEvent;

public class BigXmlTestIteratorApi {

	private static final String DOCUMENT_NS = "http://www.error.be/bigxmltest";
	private static final String BIGXMLTEST_ROOT_ELEMENT = "BigXmlTest";
	private static final String HEADER_ELEMENT = "Header";
	private static final String CONTENT_ELEMENT = "Content";
	private static final String DATA_ELEMENT = "Data";

	private final ObjectFactory objectFactory = new ObjectFactory();
	private final Marshaller marshaller;

	public static void main(String args[]) throws Exception {
		BigXmlTestIteratorApi bigXmlTest = new BigXmlTestIteratorApi();
		bigXmlTest.startSplitting();
	}

	{
		try {
			// Setup JAXB
			marshaller = JAXBContext.newInstance("be.error.bigxmltest").createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void startSplitting() throws Exception {
		XMLEventReader xmlEventReader = ((XMLInputFactory2) XMLInputFactory.newInstance())
				.createXMLEventReader(BigXmlTestIteratorApi.class.getResource("/BigXmlTest.xml"));

		// No validation. Woodstox does not support validation using the iterator API.
		// A quick workaround would be to parse the file first using the cursor API, just for validation,
		// and then parse it a second time using the iterator API.

		int fileNumber = 0;
		int dataRepetitions = 0;
		XMLEventWriter xmlEventWriter = openOutputFileAndWriteHeader(++fileNumber); // Prepare first file

		try {
			while (xmlEventReader.hasNext()) {
				XMLEvent event = xmlEventReader.nextEvent();

				if (event.isStartElement() && event.asStartElement().getName().getLocalPart().equals(CONTENT_ELEMENT)) {
					event = xmlEventReader.nextEvent();

					while (!(event.isEndElement() && event.asEndElement().getName().getLocalPart()
							.equals(CONTENT_ELEMENT))) {

						if (dataRepetitions != 0 && event.isStartElement()
								&& event.asStartElement().getName().getLocalPart().equals(DATA_ELEMENT)
								&& dataRepetitions % 2 == 0) { // %2 = just for testing: replace this by for example checking the actual size of the current
																// output file
							xmlEventWriter.close(); // Also closes any open Element(s) and the document
							xmlEventWriter = openOutputFileAndWriteHeader(++fileNumber); // Continue with next file
							dataRepetitions = 0;
						}
						// Write the current event to output
						xmlEventWriter.add(event);
						event = xmlEventReader.nextEvent();

						if (event.isEndElement() && event.asEndElement().getName().getLocalPart().equals(DATA_ELEMENT)) {
							dataRepetitions++;
						}
					}
				}
			}
		} finally {
			xmlEventReader.close();
			xmlEventWriter.close();
		}
	}

	private XMLEventWriter openOutputFileAndWriteHeader(int fileNumber) throws Exception {
		XMLEventFactory xmlEventFactory = XMLEventFactory.newInstance();
		XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
		xmlOutputFactory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, true);
		XMLEventWriter writer = xmlOutputFactory.createXMLEventWriter(new FileOutputStream(new File(System
				.getProperty("java.io.tmpdir"), "BigXmlTest." + fileNumber + ".xml")));

		writer.setDefaultNamespace(DOCUMENT_NS);
		writer.add(xmlEventFactory.createStartDocument());

		writer.add(xmlEventFactory.createStartElement(new QName(DOCUMENT_NS, BIGXMLTEST_ROOT_ELEMENT), null, null));

		HeaderType header = objectFactory.createHeaderType();
		header.setSomeHeaderElement("Something something darkside");
		marshaller.marshal(new JAXBElement<HeaderType>(new QName(DOCUMENT_NS, HEADER_ELEMENT, ""), HeaderType.class,
				HeaderType.class, header), writer);

		writer.add(new StartElementEvent(new QName(DOCUMENT_NS, CONTENT_ELEMENT)));
		return writer;
	}
}