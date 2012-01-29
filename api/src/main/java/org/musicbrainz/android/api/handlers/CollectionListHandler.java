package org.musicbrainz.android.api.handlers;

import java.util.LinkedList;

import org.musicbrainz.android.api.data.EditorCollectionStub;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class CollectionListHandler extends MBHandler {

    private LinkedList<EditorCollectionStub> collections = new LinkedList<EditorCollectionStub>();
    private EditorCollectionStub stub;

    public LinkedList<EditorCollectionStub> getResults() {
        return collections;
    }

    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {

        if (localName.equalsIgnoreCase("collection")) {
            stub = new EditorCollectionStub();
            stub.setMbid(atts.getValue("id"));
        } else if (localName.equalsIgnoreCase("name")) {
            sb = new StringBuilder();
        } else if (localName.equalsIgnoreCase("editor")) {
            sb = new StringBuilder();
        } else if (localName.equalsIgnoreCase("release-list")) {
            stub.setCount(Integer.parseInt(atts.getValue("count")));
        }
    }

    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {

        if (localName.equalsIgnoreCase("name")) {
            stub.setName(sb.toString());
        } else if (localName.equalsIgnoreCase("editor")) {
            stub.setEditor(sb.toString());
        } else if (localName.equalsIgnoreCase("collection")) {
            collections.add(stub);
        }
    }

}
