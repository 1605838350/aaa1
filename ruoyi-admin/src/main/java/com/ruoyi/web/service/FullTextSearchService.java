package com.ruoyi.web.service;

import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.utils.file.DocumentContentExtractor;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 全文搜索服务
 * 基于 Apache Lucene 实现文档内容索引和搜索
 */
@Service
public class FullTextSearchService {

    private static final Logger log = LoggerFactory.getLogger(FullTextSearchService.class);

    @Autowired
    private RuoYiConfig ruoyiConfig;

    private Directory directory;
    private StandardAnalyzer analyzer;
    private IndexWriter indexWriter;
    private SearcherManager searcherManager;
    private String indexPath;

    @PostConstruct
    public void init() throws Exception {
        indexPath = ruoyiConfig.getProfile() + "/lucene-index";
        File indexDir = new File(indexPath);
        if (!indexDir.exists()) {
            indexDir.mkdirs();
        }

        directory = FSDirectory.open(Paths.get(indexPath));
        analyzer = new StandardAnalyzer();

        boolean indexExists = DirectoryReader.indexExists(directory);
        if (!indexExists) {
            log.warn("索引目录为空或损坏，将创建新索引: {}", indexPath);
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            indexWriter = new IndexWriter(directory, config);
            indexWriter.commit();
        } else {
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            indexWriter = new IndexWriter(directory, config);
        }

        // SearcherManager 复用 IndexReader，避免每次搜索都重新打开
        searcherManager = new SearcherManager(indexWriter, null);

        log.info("全文搜索服务初始化完成，索引路径: {}", indexPath);
    }

    @PreDestroy
    public void close() throws Exception {
        if (searcherManager != null) {
            searcherManager.close();
        }
        if (indexWriter != null) {
            indexWriter.close();
        }
        if (directory != null) {
            directory.close();
        }
        log.info("全文搜索服务已关闭");
    }

    /**
     * 获取 IndexSearcher（复用 reader，用完必须 release）
     */
    private IndexSearcher acquireSearcher() throws Exception {
        searcherManager.maybeRefresh();
        return searcherManager.acquire();
    }

    private void releaseSearcher(IndexSearcher searcher) {
        try {
            if (searcher != null) {
                searcherManager.release(searcher);
            }
        } catch (Exception e) {
            // ignore
        }
    }

    /**
     * 将文件添加到全文索引（异步，用于上传后不阻塞响应）
     */
    /**
     * 将文件添加到全文索引（同步版本，用于重建索引等场景）
     */
    public void indexFile(String filePath, String fileName, InputStream contentStream) {
        if (!DocumentContentExtractor.isSupportedDocument(fileName)) {
            return;
        }
        try {
            String relativePath = convertToRelativePath(filePath);
            String content = DocumentContentExtractor.extractContent(contentStream, fileName);
            if (content != null && !content.trim().isEmpty()) {
                addOrUpdateDocument(relativePath, fileName, content, System.currentTimeMillis());
            }
        } catch (Exception e) {
            log.warn("索引文件失败: {} - {}", fileName, e.getMessage());
        }
    }

    /**
     * 将文件添加到全文索引（异步版本，上传后不阻塞响应）
     */
    @Async
    public void indexFileAsync(String filePath, String fileName, InputStream contentStream) {
        try {
            if (!DocumentContentExtractor.isSupportedDocument(fileName)) {
                return;
            }
            String relativePath = convertToRelativePath(filePath);
            String content = DocumentContentExtractor.extractContent(contentStream, fileName);
            if (content != null && !content.trim().isEmpty()) {
                addOrUpdateDocument(relativePath, fileName, content, System.currentTimeMillis());
            }
        } catch (Exception e) {
            log.warn("异步索引失败: {} - {}", fileName, e.getMessage());
        } finally {
            try { contentStream.close(); } catch (Exception ignored) {}
        }
    }

    /**
     * 从索引中删除单个文件
     */
    public void removeFile(String filePath) {
        try {
            String normalized = filePath.replace("\\", "/");
            if (!normalized.startsWith("/")) {
                normalized = "/" + normalized;
            }
            deleteDocument(normalized);
        } catch (Exception e) {
            log.warn("删除索引失败: {} - {}", filePath, e.getMessage());
        }
    }

    /**
     * 从索引中删除指定路径前缀的所有文档（用于文件夹重命名/移动）
     */
    public void removeByPrefix(String pathPrefix) {
        try {
            String normalized = pathPrefix.replace("\\", "/");
            if (!normalized.startsWith("/")) {
                normalized = "/" + normalized;
            }
            if (!normalized.endsWith("/")) {
                normalized = normalized + "/";
            }
            Query query = new PrefixQuery(new Term("path", normalized));
            indexWriter.deleteDocuments(query);
            indexWriter.commit();
            log.info("已按前缀删除索引文档: {}", normalized);
        } catch (Exception e) {
            log.warn("按前缀删除索引失败: {} - {}", pathPrefix, e.getMessage());
        }
    }

    /**
     * 按文件名搜索（纯 Lucene WildcardQuery，毫秒级）
     */
    public List<SearchResult> searchFileByName(String keyword, int maxResults) {
        List<SearchResult> results = new ArrayList<>();
        if (keyword == null || keyword.trim().isEmpty()) return results;
        IndexSearcher searcher = null;
        try {
            searcher = acquireSearcher();
            WildcardQuery query = new WildcardQuery(new Term("fileNameRaw", "*" + keyword + "*"));
            TopDocs topDocs = searcher.search(query, maxResults);
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                SearchResult r = new SearchResult();
                r.setPath(doc.get("path"));
                r.setFileName(doc.get("fileName"));
                String t = doc.get("updateTime");
                if (t != null) r.setUpdateTime(Long.parseLong(t));
                results.add(r);
            }
        } catch (Exception e) {
            log.warn("文件名搜索失败: {} - {}", keyword, e.getMessage());
        } finally {
            releaseSearcher(searcher);
        }
        return results;
    }

    private String convertToRelativePath(String path) {
        if (path == null) return "/";
        String normalized = path.replace("\\", "/");
        String baseDir = RuoYiConfig.getProfile().replace("\\", "/");
        if (normalized.startsWith(baseDir)) {
            normalized = normalized.substring(baseDir.length());
        }
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized;
    }

    /**
     * 添加或更新文档到索引
     */
    public void addOrUpdateDocument(String path, String fileName, String content, long updateTime) throws Exception {
        // 先删除旧文档（如果存在）
        deleteDocument(path);

        // 创建新文档
        Document doc = new Document();
        doc.add(new StringField("path", path, Field.Store.YES));
        doc.add(new TextField("fileName", fileName, Field.Store.YES));
        // 不分词字段，供通配符文件名搜索
        FieldType rawType = new FieldType();
        rawType.setIndexOptions(IndexOptions.DOCS);
        rawType.setStored(false);
        rawType.setTokenized(false);
        rawType.freeze();
        doc.add(new Field("fileNameRaw", fileName, rawType));
        doc.add(new TextField("content", content, Field.Store.YES));
        doc.add(new StoredField("updateTime", updateTime));

        indexWriter.addDocument(doc);
        indexWriter.commit();

        log.debug("文档已添加到索引: {}", fileName);
    }

    /**
     * 从索引删除文档
     */
    public void deleteDocument(String path) throws Exception {
        if (!DirectoryReader.indexExists(directory)) {
            log.debug("索引不存在，无需删除文档: {}", path);
            return;
        }

        Query query = new TermQuery(new Term("path", path));
        indexWriter.deleteDocuments(query);
        indexWriter.commit();
        log.debug("文档已从索引删除: {}", path);
    }

    /**
     * 调试方法：列出索引中的所有路径
     */
    private void listAllIndexedPaths() {
        IndexSearcher searcher = null;
        try {
            searcher = acquireSearcher();
            IndexReader reader = searcher.getIndexReader();
            int maxDocs = reader.maxDoc();
            log.info("索引中共有 {} 个文档", maxDocs);
            for (int i = 0; i < Math.min(maxDocs, 10); i++) {
                try {
                    Document doc = reader.document(i);
                    log.info("索引文档 {}: path={}", i, doc.get("path"));
                } catch (Exception e) {
                    // 忽略已删除文档
                }
            }
        } catch (Exception e) {
            log.warn("列出索引文档失败: {}", e.getMessage());
        } finally {
            releaseSearcher(searcher);
        }
    }

    /**
     * 搜索文档内容
     * @param keyword 搜索关键词
     * @param maxResults 最大返回结果数
     * @return 搜索结果列表
     */
    public List<SearchResult> search(String keyword, int maxResults) throws Exception {
        List<SearchResult> results = new ArrayList<>();

        if (keyword == null || keyword.trim().isEmpty()) {
            return results;
        }

        if (!DirectoryReader.indexExists(directory)) {
            log.debug("索引不存在，返回空结果");
            return results;
        }

        // 转义特殊字符，避免 QueryParser 抛异常
        String escaped = org.apache.lucene.queryparser.classic.QueryParser.escape(keyword.trim());

        IndexSearcher searcher = acquireSearcher();
        try {
            BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();

            QueryParser contentParser = new QueryParser("content", analyzer);
            queryBuilder.add(contentParser.parse(escaped), BooleanClause.Occur.SHOULD);

            QueryParser fileNameParser = new QueryParser("fileName", analyzer);
            queryBuilder.add(fileNameParser.parse(escaped), BooleanClause.Occur.SHOULD);

            Query query = queryBuilder.build();

            TopDocs topDocs = searcher.search(query, maxResults);

            QueryScorer scorer = new QueryScorer(query);
            Fragmenter fragmenter = new SimpleSpanFragmenter(scorer, 150);
            SimpleHTMLFormatter formatter = new SimpleHTMLFormatter("<mark>", "</mark>");
            Highlighter highlighter = new Highlighter(formatter, scorer);
            highlighter.setTextFragmenter(fragmenter);

            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);

                SearchResult result = new SearchResult();
                result.setPath(doc.get("path"));
                result.setFileName(doc.get("fileName"));
                result.setUpdateTime(Long.parseLong(doc.get("updateTime")));

                String content = doc.get("content");
                if (content != null) {
                    TokenStream tokenStream = analyzer.tokenStream("content", content);
                    String snippet = highlighter.getBestFragment(tokenStream, content);
                    if (snippet == null) {
                        snippet = content.length() > 200 ? content.substring(0, 200) + "..." : content;
                    }
                    result.setContentSnippet(snippet);
                }

                results.add(result);
            }
        } finally {
            releaseSearcher(searcher);
        }

        return results;
    }

    /**
     * 检查文档是否已索引
     */
    public boolean isDocumentIndexed(String path) throws Exception {
        IndexSearcher searcher = acquireSearcher();
        try {
            Query query = new TermQuery(new Term("path", path));
            TopDocs results = searcher.search(query, 1);
            return results.totalHits.value > 0;
        } finally {
            releaseSearcher(searcher);
        }
    }

    public int getIndexedDocumentCount() throws Exception {
        if (!DirectoryReader.indexExists(directory)) {
            return 0;
        }
        IndexSearcher searcher = acquireSearcher();
        try {
            return searcher.getIndexReader().numDocs();
        } finally {
            releaseSearcher(searcher);
        }
    }

    /**
     * 清空所有索引
     */
    public void clearAllIndex() throws Exception {
        indexWriter.deleteAll();
        indexWriter.commit();
        log.info("所有索引已清空");
    }

    /**
     * 搜索结果对象
     */
    public static class SearchResult {
        private String path;
        private String fileName;
        private String contentSnippet;
        private long updateTime;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getContentSnippet() {
            return contentSnippet;
        }

        public void setContentSnippet(String contentSnippet) {
            this.contentSnippet = contentSnippet;
        }

        public long getUpdateTime() {
            return updateTime;
        }

        public void setUpdateTime(long updateTime) {
            this.updateTime = updateTime;
        }
    }
}

