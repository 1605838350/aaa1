package com.ruoyi.common.utils.file;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.ss.usermodel.*;

import java.io.InputStream;
import java.util.List;

/**
 * 文档内容提取工具类
 * 支持 Word、PDF、Excel、TXT 文件内容提取
 */
public class DocumentContentExtractor {

    /**
     * 提取 Word 文档内容
     * @param inputStream 文件输入流
     * @param fileName 文件名（用于判断 doc 还是 docx）
     * @return 文档文本内容
     */
    public static String extractWordContent(InputStream inputStream, String fileName) throws Exception {
        if (fileName.toLowerCase().endsWith(".docx")) {
            return extractDocxContent(inputStream);
        } else if (fileName.toLowerCase().endsWith(".doc")) {
            return extractDocContent(inputStream);
        }
        return "";
    }

    /**
     * 提取 .docx 格式 Word 文档内容
     */
    private static String extractDocxContent(InputStream inputStream) throws Exception {
        StringBuilder content = new StringBuilder();
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            for (XWPFParagraph paragraph : paragraphs) {
                String text = paragraph.getText();
                if (text != null && !text.trim().isEmpty()) {
                    content.append(text).append("\n");
                }
            }
        }
        return content.toString();
    }

    /**
     * 提取 .doc 格式 Word 文档内容
     */
    private static String extractDocContent(InputStream inputStream) throws Exception {
        // 先将输入流读取为字节数组，以便失败时可以重试
        byte[] data = org.apache.commons.io.IOUtils.toByteArray(inputStream);

        String rawText = null;
        try {
            try (HWPFDocument document = new HWPFDocument(new java.io.ByteArrayInputStream(data))) {
                WordExtractor extractor = new WordExtractor(document);
                rawText = extractor.getText();
            }
        } catch (Exception e) {
            // 如果 HWPF 解析失败，可能是旧版格式或损坏文件，尝试使用备用方法
            String errorMsg = e.getMessage();
            if (errorMsg != null && (errorMsg.contains("Word95") || errorMsg.contains("corrupt") ||
                    errorMsg.contains("out of bounds") || errorMsg.contains("Table Stream"))) {
                return extractDocAsPlainText(new java.io.ByteArrayInputStream(data));
            }
            throw e;
        }

        // 调试：先返回原始提取文本 + 清洗后文本，看 POI 到底提取出了什么
        String cleaned = cleanDocText(rawText);
        // 如果清洗后中文丢失，返回原始文本供调试
        boolean cleanedHasCJK = cleaned != null && cleaned.matches(".*[\\u4e00-\\u9fff].*");
        boolean rawHasCJK = rawText != null && rawText.matches(".*[\\u4e00-\\u9fff].*");
        if (rawHasCJK && !cleanedHasCJK) {
            // 清洗把中文丢了，返回原始提取文本
            return rawText;
        }
        return cleaned;
    }

    /**
     * 清洗 POI 提取的文本：去掉二进制垃圾/控制字符/HTML标签，保留可读内容
     */
    private static String cleanDocText(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }

        String text = raw;

        // 1. 截断二进制头部：找到 HTML 标签真正的起始位置（而非二进制中碰巧的 < 字节）
        java.util.regex.Matcher htmlStart = java.util.regex.Pattern.compile(
            "<!(?:doctype|--)|<html\\b|<head\\b|<body\\b|<div\\b|<p\\b|<span\\b|<table\\b|<meta\\b|<style\\b|<script\\b|<h[1-6]\\b|<br\\b|<li\\b|<a\\b|<tr\\b|<td\\b",
            java.util.regex.Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (htmlStart.find() && htmlStart.start() > 0 && htmlStart.start() < text.length() / 2) {
            text = text.substring(htmlStart.start());
        }

        // 2. QP 解码：逐字节收集，正确处理 UTF-8 多字节字符
        text = text.replaceAll("=\r?\n", ""); // 软换行
        try {
            java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("=([0-9A-Fa-f]{2})|[^=]+|=").matcher(text);
            while (m.find()) {
                String g = m.group();
                if (g.length() == 3 && g.charAt(0) == '=') {
                    // QP 编码字节 =XX
                    buf.write(Integer.parseInt(g.substring(1), 16));
                } else if (g.equals("=")) {
                    // 单独的 = 号（非 QP 编码）
                    buf.write('=');
                } else {
                    // 普通文本，保持原样（作为 UTF-8 字节写入）
                    buf.write(g.getBytes("UTF-8"));
                }
            }
            text = new String(buf.toByteArray(), "UTF-8");
        } catch (Exception e) {
            // QP 解码失败，保留原文本
        }

        // 3. 去掉 HTML 标签
        text = text.replaceAll("(?s)<style[^>]*>.*?</style>", " ");
        text = text.replaceAll("(?s)<script[^>]*>.*?</script>", " ");
        text = text.replaceAll("<br\\s*/?>", "\n");
        text = text.replaceAll("</?p[^>]*>", "\n");
        text = text.replaceAll("</?div[^>]*>", "\n");
        text = text.replaceAll("</?h\\d[^>]*>", "\n");
        text = text.replaceAll("</?tr[^>]*>", "\n");
        text = text.replaceAll("</?td[^>]*>", " ");
        text = text.replaceAll("</?li[^>]*>", "\n");
        text = text.replaceAll("<[^>]+>", "");

        // 4. 解码 HTML 实体
        text = text.replace("&nbsp;", " ")
                   .replace("&lt;", "<")
                   .replace("&gt;", ">")
                   .replace("&amp;", "&")
                   .replace("&quot;", "\"")
                   .replace("&#160;", " ")
                   .replace("&#xa0;", " ");

        // 5. 去掉残留的 C0 控制字符（保留 \n \r \t 和所有 printable/Unicode 文字）
        StringBuilder cleaned = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n' || c == '\r' || c == '\t') {
                cleaned.append(c);
            } else if (c >= 0x20 && c != 0x7F) {
                cleaned.append(c);
            }
            // 其余控制字符（0x00-0x1F 除掉 \n\r\t，以及 DEL 0x7F）丢弃
        }

        // 6. 压缩空白
        String result = cleaned.toString();
        result = result.replaceAll("[ \t]+\n", "\n");
        result = result.replaceAll("\n[ \t]+", "\n");
        result = result.replaceAll(" {2,}", " ");
        result = result.replaceAll("\n{3,}", "\n\n");

        return result.trim();
    }
    
    /**
     * 尝试以纯文本方式读取 .doc 文件（备用方法）
     */
    private static String extractDocAsPlainText(InputStream inputStream) {
        StringBuilder content = new StringBuilder();
        try {
            // 尝试直接读取可打印字符
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                for (int i = 0; i < bytesRead; i++) {
                    byte b = buffer[i];
                    // 只保留可打印字符
                    if ((b >= 32 && b < 127) || b == '\n' || b == '\r' || b == '\t') {
                        content.append((char) b);
                    } else if ((b & 0xFF) >= 0xC0 && (b & 0xFF) <= 0xDF && i + 1 < bytesRead) {
                        // 尝试识别 UTF-8 中文字符
                        byte b2 = buffer[++i];
                        if ((b2 & 0xC0) == 0x80) {
                            content.append(new String(new byte[]{b, b2}, "UTF-8"));
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 备用方法也失败，返回空字符串
        }
        return content.toString().replaceAll("\\s+", " ").trim();
    }

    /**
     * 提取 PDF 文档内容
     * @param inputStream 文件输入流
     * @return 文档文本内容
     */
    public static String extractPdfContent(InputStream inputStream) throws Exception {
        // 禁用 PDFBox 字体缓存警告
        System.setProperty("pdfbox.font.cache", "false");
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    /**
     * 提取 Excel 文档内容
     * @param inputStream 文件输入流
     * @param fileName 文件名（用于判断 xls 还是 xlsx）
     * @return 文档文本内容
     */
    public static String extractExcelContent(InputStream inputStream, String fileName) throws Exception {
        StringBuilder content = new StringBuilder();
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            int sheetCount = workbook.getNumberOfSheets();
            for (int i = 0; i < sheetCount; i++) {
                Sheet sheet = workbook.getSheetAt(i);
                content.append("Sheet: ").append(sheet.getSheetName()).append("\n");
                
                for (Row row : sheet) {
                    StringBuilder rowContent = new StringBuilder();
                    for (Cell cell : row) {
                        String cellValue = getCellValueAsString(cell);
                        if (cellValue != null && !cellValue.trim().isEmpty()) {
                            rowContent.append(cellValue).append(" ");
                        }
                    }
                    if (rowContent.length() > 0) {
                        content.append(rowContent.toString().trim()).append("\n");
                    }
                }
                content.append("\n");
            }
        }
        return content.toString();
    }

    /**
     * 获取单元格内容作为字符串
     */
    private static String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    /**
     * 提取 TXT 文本文件内容
     * @param inputStream 文件输入流
     * @return 文档文本内容
     */
    public static String extractTxtContent(InputStream inputStream) throws Exception {
        StringBuilder content = new StringBuilder();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            content.append(new String(buffer, 0, bytesRead, "UTF-8"));
        }
        return content.toString();
    }

    /**
     * 根据文件扩展名判断是否为支持的文档类型
     */
    public static boolean isSupportedDocument(String fileName) {
        String lowerName = fileName.toLowerCase();
        return lowerName.endsWith(".doc") || 
               lowerName.endsWith(".docx") || 
               lowerName.endsWith(".pdf") || 
               lowerName.endsWith(".xls") || 
               lowerName.endsWith(".xlsx") || 
               lowerName.endsWith(".txt");
    }

    /**
     * 通用提取方法，根据文件类型自动选择提取器
     */
    public static String extractContent(InputStream inputStream, String fileName) throws Exception {
        String lowerName = fileName.toLowerCase();
        
        if (lowerName.endsWith(".doc") || lowerName.endsWith(".docx")) {
            return extractWordContent(inputStream, fileName);
        } else if (lowerName.endsWith(".pdf")) {
            return extractPdfContent(inputStream);
        } else if (lowerName.endsWith(".xls") || lowerName.endsWith(".xlsx")) {
            return extractExcelContent(inputStream, fileName);
        } else if (lowerName.endsWith(".txt")) {
            return extractTxtContent(inputStream);
        }
        
        return "";
    }
}
