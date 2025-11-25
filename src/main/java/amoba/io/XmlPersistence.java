package amoba.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import amoba.board.Board;

public final class XmlPersistence {

    private XmlPersistence() { }

    public static void saveToXml(Board board, Path path) throws IOException {
        int estimated = board.rows() * (board.cols() + 20) + 64;
        StringBuilder sb = new StringBuilder(estimated);
        sb.append("<game rows=\"").append(board.rows()).append("\" cols=\"").append(board.cols()).append("\">\n");
        for (String line : board.toCharLines()) {
            sb.append("  <row>").append(escape(line)).append("</row>\n");
        }
        sb.append("</game>\n");
        Files.writeString(path, sb.toString());
    }

    public static Board loadFromXml(Path path) throws IOException {
        String xml = Files.readString(path);
        int headerEnd = findHeaderEnd(xml);
        String header = xml.substring(xml.indexOf("<game"), headerEnd);
        int rows = parseAttr(header, "rows");
        int cols = parseAttr(header, "cols");
        Board b = new Board(rows, cols);
        List<String> rowValues = extractRows(xml, headerEnd + 1, rows, cols);
        b.loadFromCharLines(rowValues);
        return b;
    }

    private static int findHeaderEnd(String xml) throws IOException {
        int gameStart = xml.indexOf("<game");
        if (gameStart < 0) {
            throw new IOException("Hiányzó <game> fejléc");
        }
        int headerEnd = xml.indexOf('>', gameStart);
        if (headerEnd < 0) {
            throw new IOException("Hiányzó '>' a game fejlécben");
        }
        return headerEnd;
    }

    private static List<String> extractRows(String xml, int startIdx, int expectedRows, int expectedCols) throws IOException {
        List<String> lines = new ArrayList<>();
        int idx = startIdx;
        while (true) {
            int rowTag = xml.indexOf("<row>", idx);
            if (rowTag < 0) {
                break;
            }
            int rowEnd = xml.indexOf("</row>", rowTag);
            if (rowEnd < 0) {
                throw new IOException("Hiányzó </row>");
            }
            String content = unescape(xml.substring(rowTag + 5, rowEnd));
            if (content.length() != expectedCols) {
                throw new IOException("Hibás sor hossza XML-ben");
            }
            lines.add(content);
            idx = rowEnd + 6;
        }
        if (lines.size() != expectedRows) {
            throw new IOException("Hibás sor szám XML-ben");
        }
        return lines;
    }

    private static int parseAttr(String header, String name) throws IOException {
        String key = name + "=\"";
        int i = header.indexOf(key);
        if (i < 0) {
            throw new IOException("Hiányzó attribútum: " + name);
        }
        int j = header.indexOf('"', i + key.length());
        if (j < 0) {
            throw new IOException("Rossz attribútum zárás: " + name);
        }
        return Integer.parseInt(header.substring(i + key.length(), j));
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String unescape(String s) {
        return s.replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
    }
}