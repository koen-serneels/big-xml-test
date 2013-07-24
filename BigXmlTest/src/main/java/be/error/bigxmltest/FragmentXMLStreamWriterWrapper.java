package be.error.bigxmltest;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;

/**
 * When using the JDK (internally packaged) apache Xalan transformer for StAX, it will always assume it is writing to a stand-alone stream. This means it will
 * ignore any {@link OutputKeys#OMIT_XML_DECLARATION} or {@link OutputKeys#STANDALONE} output properties. If one is adding XML fragments (somewhere in an
 * already initialized XML stream) this will result in adding the XML declaration twice which leads to exceptions. The same goes if you want to append multiple
 * fragments; the writer will close the document disallowing any further content to be written after the first fragment. In this wrapper we intercept
 * {@link #writeStartDocument()} and overloaded versions and {@link #writeEndDocument()} to do nothing.
 * <p>
 * 
 * Attention: using this writer you will have to start/end the document yourself on the corresponding {@link XMLStreamWriter} delegate
 * 
 * @author Koen Serneels
 */
public class FragmentXMLStreamWriterWrapper extends XMLStreamWriterAdapter {

	public FragmentXMLStreamWriterWrapper(XMLStreamWriter delegate) {
		super(delegate);
	}

	@Override
	public void writeStartDocument() throws XMLStreamException {
		// NOOP
	}

	@Override
	public void writeStartDocument(String version) throws XMLStreamException {
		// NOOP
	}

	@Override
	public void writeStartDocument(String encoding, String version) throws XMLStreamException {
		// NOOP
	}

	@Override
	public void writeEndDocument() throws XMLStreamException {
		// NOOP
	}
}
