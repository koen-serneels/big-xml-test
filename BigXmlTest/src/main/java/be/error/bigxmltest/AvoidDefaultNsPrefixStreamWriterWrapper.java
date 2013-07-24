package be.error.bigxmltest;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class AvoidDefaultNsPrefixStreamWriterWrapper extends XMLStreamWriterAdapter {

	private final String defaultNs;

	public AvoidDefaultNsPrefixStreamWriterWrapper(XMLStreamWriter delegate, String defaultNs) {
		super(delegate);
		this.defaultNs = defaultNs;
	}

	@Override
	public void writeNamespace(String prefix, String namespaceURI) throws XMLStreamException {
		if (defaultNs.equals(namespaceURI)) {
			return;
		}
		super.writeNamespace(prefix, namespaceURI);
	}

	@Override
	public void setPrefix(String prefix, String uri) throws XMLStreamException {
		if (prefix.equals("xmlns")) {
			return;
		}
		super.setPrefix(prefix, uri);
	}
}