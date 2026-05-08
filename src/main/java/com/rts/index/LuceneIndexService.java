package com.rts.index;

import com.rts.model.CoreModels.CandidateObject;
import com.rts.model.CoreModels.NavigationView;
import com.rts.model.CoreModels.ObjectCard;
import com.rts.model.CoreModels.ObjectManifestEntry;
import com.rts.model.CoreModels.ScopeKey;
import com.rts.store.FileSystemProjectionStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.index.Term;
import org.springframework.stereotype.Component;

@Component
public class LuceneIndexService {
    private final FileSystemProjectionStore store;
    private final Analyzer analyzer = new StandardAnalyzer();
    private final ConcurrentMap<String, Object> rebuildLocks = new ConcurrentHashMap<>();

    public LuceneIndexService(FileSystemProjectionStore store) {
        this.store = store;
    }

    public void rebuild(String releaseId) {
        synchronized (rebuildLocks.computeIfAbsent(releaseId, ignored -> new Object())) {
            rebuildLocked(releaseId);
        }
    }

    private void rebuildLocked(String releaseId) {
        Path indexPath = indexPath(releaseId);
        try {
            Files.createDirectories(indexPath);
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            config.setSimilarity(new BM25Similarity());
            try (IndexWriter writer = new IndexWriter(FSDirectory.open(indexPath), config)) {
                for (ObjectManifestEntry entry : store.allObjects(releaseId)) {
                    ObjectCard card = store.getCard(releaseId, entry.uri()).orElse(null);
                    NavigationView navigation = store.navigationViews(releaseId).stream()
                            .filter(view -> view.uri().equals(entry.uri()))
                            .findFirst()
                            .orElse(null);
                    if (card == null) {
                        continue;
                    }
                    writer.addDocument(toDocument(entry, card, navigation));
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot rebuild Lucene index for " + releaseId, ex);
        }
    }

    public List<CandidateObject> search(String releaseId, ScopeKey scope, String queryText, List<String> objectTypes, int limit) {
        Path indexPath = indexPath(releaseId);
        try {
            Files.createDirectories(indexPath);
            if (!DirectoryReader.indexExists(FSDirectory.open(indexPath))) {
                synchronized (rebuildLocks.computeIfAbsent(releaseId, ignored -> new Object())) {
                    if (!DirectoryReader.indexExists(FSDirectory.open(indexPath))) {
                        rebuildLocked(releaseId);
                    }
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot prepare Lucene index for " + releaseId, ex);
        }
        try (DirectoryReader reader = DirectoryReader.open(FSDirectory.open(indexPath))) {
            IndexSearcher searcher = new IndexSearcher(reader);
            searcher.setSimilarity(new BM25Similarity());
            Query textQuery = new MultiFieldQueryParser(
                    new String[]{"searchable_text", "card_text", "l0_text", "l1_text", "target_path", "object_id"},
                    analyzer,
                    Map.of("target_path", 2.5f, "object_id", 3.0f, "card_text", 1.5f))
                    .parse(MultiFieldQueryParser.escape(queryText));
            BooleanQuery.Builder builder = new BooleanQuery.Builder()
                    .add(textQuery, BooleanClause.Occur.MUST)
                    .add(exact("release_id", releaseId), BooleanClause.Occur.FILTER)
                    .add(exact("channel", scope.channel()), BooleanClause.Occur.FILTER)
                    .add(exact("product", scope.product()), BooleanClause.Occur.FILTER)
                    .add(exact("pack", scope.pack()), BooleanClause.Occur.FILTER)
                    .add(exact("domain", scope.domain()), BooleanClause.Occur.FILTER);
            if (objectTypes != null && !objectTypes.isEmpty()) {
                BooleanQuery.Builder types = new BooleanQuery.Builder();
                for (String objectType : objectTypes) {
                    types.add(exact("object_type", objectType), BooleanClause.Occur.SHOULD);
                }
                builder.add(types.build(), BooleanClause.Occur.FILTER);
            }
            TopDocs topDocs = searcher.search(builder.build(), Math.max(1, limit));
            List<CandidateObject> candidates = new ArrayList<>();
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                store.getObject(releaseId, doc.get("uri")).ifPresent(entry -> {
                    if (entry.scope().matches(scope)) {
                        candidates.add(new CandidateObject(
                                entry.uri(),
                                entry.objectType(),
                                scoreDoc.score,
                                List.of("bm25", "scope_filter"),
                                false));
                    }
                });
            }
            return candidates;
        } catch (Exception ex) {
            throw new IllegalStateException("Lucene search failed", ex);
        }
    }

    private Document toDocument(ObjectManifestEntry entry, ObjectCard card, NavigationView navigation) {
        Document doc = new Document();
        doc.add(new StringField("uri", entry.uri(), Field.Store.YES));
        doc.add(new StringField("release_id", entry.releaseId(), Field.Store.YES));
        doc.add(new StringField("canonical_revision", entry.releaseId(), Field.Store.NO));
        doc.add(new StringField("channel", entry.channel(), Field.Store.YES));
        doc.add(new StringField("product", entry.product(), Field.Store.YES));
        doc.add(new StringField("pack", entry.pack(), Field.Store.YES));
        doc.add(new StringField("domain", entry.domain(), Field.Store.YES));
        doc.add(new StringField("object_type", entry.objectType().name(), Field.Store.YES));
        doc.add(new StringField("object_id", entry.objectId(), Field.Store.YES));
        if (entry.targetPath() != null) {
            doc.add(new TextField("target_path", entry.targetPath(), Field.Store.YES));
        }
        doc.add(new TextField("source_anchor", String.join(" ", safe(entry.sourceAnchors())), Field.Store.NO));
        doc.add(new TextField("lookup_id", entry.objectId(), Field.Store.NO));
        doc.add(new TextField("helper_id", entry.objectId(), Field.Store.NO));
        doc.add(new TextField("rule_id", entry.objectId(), Field.Store.NO));
        String cardText = card.searchText() == null ? "" : card.searchText();
        String l0Text = navigation == null || navigation.l0Text() == null ? cardText : navigation.l0Text();
        String l1Text = navigation == null || navigation.searchText() == null ? cardText : navigation.searchText();
        doc.add(new TextField("business_terms", cardText, Field.Store.NO));
        doc.add(new TextField("searchable_text", entry.objectId() + " " + nullToEmpty(entry.targetPath()) + " " + cardText + " " + l0Text + " " + l1Text, Field.Store.NO));
        doc.add(new TextField("card_text", cardText, Field.Store.NO));
        doc.add(new TextField("l0_text", l0Text, Field.Store.NO));
        doc.add(new TextField("l1_text", l1Text, Field.Store.NO));
        return doc;
    }

    private Query exact(String field, String value) {
        return new TermQuery(new Term(field, value));
    }

    private Path indexPath(String releaseId) {
        return store.releaseRoot(releaseId).resolve("index-artifacts").resolve("lucene");
    }

    private List<String> safe(List<String> values) {
        return values == null ? List.of() : values;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
