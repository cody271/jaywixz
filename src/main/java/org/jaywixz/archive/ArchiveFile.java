package org.jaywixz.archive;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.apache.commons.compress.utils.IOUtils;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.suggest.document.SuggestIndexSearcher;
import org.apache.lucene.search.suggest.document.TopSuggestDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import org.apache.log4j.Logger;
import org.tukaani.xz.SeekableXZInputStream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


public class ArchiveFile {

    public static String titleToSuggest(String title) {
        String[] titles = title.split("/");
        String suggest = titles[titles.length - 1];
        suggest = suggest.substring(0, suggest.length() - 5);
        suggest = suggest.replace("_", " ");
        return suggest;
    }

    private static final int MAX_RESULTS = 50;
    private static final String FOOTER_NAME = "L/footer.json";

    private static final Logger log = Logger.getLogger(ArchiveFile.class);

    private XZBlockCache xz;

    private Directory directory;
    private IndexReader reader;
    private SuggestIndexSearcher searcher;

    public ArchiveFile(String archivePath) throws IOException {
        open(archivePath);
    }

    private void open(String archivePath) throws IOException {
        xz = new XZBlockCache(archivePath);
        log.debug(xz);
        openDirectory(archivePath);
        openIndex(archivePath);
    }

    private void openIndex(String archivePath) throws IOException {
        searcher = new SuggestIndexSearcher(reader, new StandardAnalyzer());
        //FIXME
        suggest("");
        log.debug(searcher);
    }

    private void openDirectory(String archivePath) throws IOException {
        Path indexPath = Paths.get(archivePath.substring(0, archivePath.length() - 3) + "idx");
        if(indexPath.toFile().exists()) {
            directory = FSDirectory.open(indexPath);
        }
        else {
            directory = new ArchiveFileDirectory(xz, loadFooter());
        }
        reader = DirectoryReader.open(directory);
        log.info(directory);
    }

    public Document doc(int docID) throws IOException {
        try {
            return reader.document(docID);
        }
        catch(IllegalArgumentException ex) {
            throw new FileNotFoundException(Integer.toString(docID));
        }
    }

    public ScoreDoc[] suggest(String queryString) throws IOException {
        TopSuggestDocs res = searcher.suggest("suggest", queryString, MAX_RESULTS);
        return res.scoreDocs;
    }

    public int lookup(String path) throws IOException {
        TopDocs doc = searcher.search(new TermQuery(new Term("title", path)), 1);
        if(doc.totalHits != 0) {
            return doc.scoreDocs[0].doc;
        }
        throw new FileNotFoundException(path);
    }

    public byte[] loadDoc(int docID) throws IOException {
        Document doc = this.doc(docID);
        IndexableField link = doc.getField("link");
        if(link != null) {
            String linkPath = link.stringValue();
            return loadDoc(lookup(linkPath));
        }
        long offset = doc.getField("offset").numericValue().longValue();
        long size = doc.getField("size").numericValue().longValue();
        byte[] contents = new byte[(int) size];

        xz.read(offset, contents, 0, contents.length, false);
        return contents;
    }

    private Map<String, List<Long>> loadFooter() throws IOException {
        InputStream footerEntry = findLastTarEntry();
        Map<String, List<Long>> footer = new HashMap<String, List<Long>>();
        Map<String, List<Long>> tmpFooter = new ObjectMapper().readValue(footerEntry,
                new TypeReference<Map<String, List<Long>>>() {
                });
        for (Entry<String, List<Long>> entry : tmpFooter.entrySet()) {
            String key = entry.getKey();
            key = key.substring(2, key.length());
            footer.put(key, entry.getValue());
        }
        return footer;
    }

    //FIXME
    private InputStream findLastTarEntry() throws IOException {
        SeekableXZInputStream stream = (SeekableXZInputStream)xz.stream();
        
        stream.seekToBlock(stream.getBlockCount() - 2);
        byte[] buffer = new byte[(int)(stream.length() - stream.position())];
        IOUtils.readFully(stream, buffer);
        ByteArrayInputStream memoryStream = new ByteArrayInputStream(buffer);        
        
        TarArchiveInputStream tar = new TarArchiveInputStream(memoryStream);
        for (int i = 0; i < (buffer.length / TarConstants.DEFAULT_RCDSIZE); i++) {
            TarArchiveEntry entry = null;

            memoryStream.reset();
            memoryStream.skip(i * TarConstants.DEFAULT_RCDSIZE);
            tar.reset();
            try {
                entry = tar.getNextTarEntry();
            } catch (IOException ex) {
            }
            if (entry != null && entry.getName().equals(FOOTER_NAME)) {
                return tar;
            }
        }
        throw new IOException("Invalid file format");
    }
}
