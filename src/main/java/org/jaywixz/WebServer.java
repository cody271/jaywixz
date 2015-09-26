package org.jaywixz;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.search.ScoreDoc;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import org.jaywixz.archive.ArchiveFile;


public class WebServer {

    private static final int DEFAULT_PORT = 9090;
    private static final String DEFAULT_IFACE = "0.0.0.0";
    private static final String ASSETS_PREFIX = "/web";

    private static final CharsetEncoder US_ASCII = StandardCharsets.US_ASCII.newEncoder();
    private static final Logger log = Logger.getLogger(WebServer.class);

    private ObjectWriter jsonWriter = new ObjectMapper().writerWithDefaultPrettyPrinter();
    private Vertx vertx;
    private CountDownLatch quitLatch;

    private ArchiveFile archive;

    public WebServer(ArchiveFile archive) {
        vertx = VertxFactory.newVertx();
        quitLatch = new CountDownLatch(1);
        this.archive = archive;
    }

    public void start() throws Exception {
        vertx.runOnContext(new Handler<Void>() {
            @Override
            public void handle(Void arg0) {
                started();
            }
        });
        quitLatch.await();
    }

    private void started() {
        RouteMatcher routes = new RouteMatcher();
        routes.get("/suggest", new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest req) {
                handleSuggest(req);
            }
        });
        routes.noMatch(new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest req) {
                handleDoc(req);
            }
        });
        vertx.createHttpServer().requestHandler(routes).listen(DEFAULT_PORT, DEFAULT_IFACE);
        log.info("http://" + DEFAULT_IFACE + ":" + DEFAULT_PORT);
    }

    private byte[] loadJarAsset(String path) throws IOException {
        InputStream stream = Jaywixz.class.getResourceAsStream(path);
        if (stream == null) return null;
        byte[] contents = new byte[stream.available()];
        IOUtils.readFully(stream, contents);
        return contents;
    }

    private void handleSuggest(HttpServerRequest req) {
        try {
            ScoreDoc[] res = archive.suggest(req.params().contains("q")? req.params().get("q"): "");
            List<Map<String, Object>> docs = new ArrayList<Map<String, Object>>();
            for (ScoreDoc doc : res) {
                String title = archive.doc(doc.doc).getField("title").stringValue();
                Map<String, Object> jsonDoc = new HashMap<String, Object>();
                jsonDoc.put("docid",  doc.doc);
                jsonDoc.put("title", ArchiveFile.titleToSuggest(title));
                docs.add(jsonDoc);
            }
            req.response()
                    .putHeader("Content-Type", "application/json")
                    .end(jsonWriter.writeValueAsString(docs));
        } catch (IOException ex) {
            serveStackTrace(req, ex);
        }
    }

    private void handleDoc(HttpServerRequest req) {
        try {
            String path = req.path();   
            if(path.equals("/")) {
                path = "/index.html";
            }
            byte[] asset = loadJarAsset(ASSETS_PREFIX + path);
            if(asset != null) {
                req.response().end(new Buffer(asset));
                return;
            }
            int docID = -1;
            try {
                docID = Integer.parseInt(path.substring(1));
            } catch (NumberFormatException intEx) {
                if(path.equals("/favicon.ico"))
                    path = "/-/favicon";
                if(path.length() >= 3 && path.charAt(2) != '/') {
                    path = path.substring(1);
                    String decodePath = URLDecoder.decode(path, "UTF-8");
                    String prefixSlash = "/";
                    if(!US_ASCII.canEncode(decodePath))
                        prefixSlash = "";
                    path = prefixSlash + "A/" + decodePath;
                }
                docID = archive.lookup(path);
            }
            serveDoc(req, docID);
        } catch (FileNotFoundException ex) {
            serveNotFound(req);
        } catch (IOException ex) {
            serveStackTrace(req, ex);
        }
    }

    private void serveDoc(HttpServerRequest req, int docID) throws IOException {
        byte[] contents = archive.loadDoc(docID);
        req.response().end(new Buffer(contents));
    }

    private void serveNotFound(HttpServerRequest req) {
        req.response().putHeader("Content-Type", "text/plain").setStatusCode(404).end("File not found");
    }

    private void serveStackTrace(HttpServerRequest req, Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String stackTrace = sw.toString();
        req.response().putHeader("Content-Type", "text/plain").setStatusCode(500).end(stackTrace);
    }
}
