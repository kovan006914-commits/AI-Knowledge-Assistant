package com.example.aiknowledgeassistant.service;

import com.example.aiknowledgeassistant.dto.KnowledgeRequest;
import com.example.aiknowledgeassistant.exception.ApiProblem;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class KnowledgeImportService {
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final int CHUNK_SIZE = 500;
    private static final Pattern HEADING = Pattern.compile("^ {0,3}#{1,6}\\s+(.+?)\\s*#*\\s*$");
    private static final Pattern FENCE = Pattern.compile("^ {0,3}(`{3,}|~{3,}).*$");
    private final KnowledgeService knowledge;

    public KnowledgeImportService(KnowledgeService knowledge) { this.knowledge = knowledge; }
    private record Section(String heading, String content) {}

    @Transactional
    public int importFile(long ownerId, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new ApiProblem(400, "文件为空，请选择有内容的文件");
        if (file.getSize() > MAX_BYTES) throw new ApiProblem(413, "文件不能超过 5MB");
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) throw new ApiProblem(400, "无法读取文件名");
        String normalized = original.replace('\\', '/');
        String name = normalized.substring(normalized.lastIndexOf('/') + 1).strip();
        String lower = name.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".txt") && !lower.endsWith(".md"))
            throw new ApiProblem(400, "只支持 .txt 和 .md 文件");

        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(file.getBytes())).toString();
        } catch (CharacterCodingException error) {
            throw new ApiProblem(400, "文件不是有效的 UTF-8 编码，请转换后重试");
        } catch (IOException error) {
            throw new ApiProblem(400, "读取文件失败，请重新选择文件后重试");
        }
        if (text.startsWith("\uFEFF")) text = text.substring(1);
        text = text.replace("\r\n", "\n").replace('\r', '\n');
        if (text.isBlank()) throw new ApiProblem(400, "文件没有可导入的文字");

        List<Section> sections = lower.endsWith(".md") ? markdown(text) : plain(text);
        int count = 0;
        for (Section section : sections) {
            String title = name + (section.heading().isBlank() ? "" : " - " + section.heading());
            for (String chunk : chunks(section.content())) {
                knowledge.create(ownerId, new KnowledgeRequest(truncate(title, 120), chunk, truncate(name, 200)));
                count++;
            }
        }
        if (count == 0) throw new ApiProblem(400, "文件没有可导入的正文");
        return count;
    }

    private List<Section> plain(String text) {
        List<Section> result = new ArrayList<>();
        for (String paragraph : text.split("\\n[\\t ]*\\n+"))
            if (!paragraph.isBlank()) result.add(new Section("", paragraph.strip()));
        return result;
    }

    private List<Section> markdown(String text) {
        List<Section> result = new ArrayList<>();
        String heading = "";
        StringBuilder body = new StringBuilder();
        char fenceChar = 0;
        int fenceLength = 0;
        for (String line : text.split("\\n", -1)) {
            Matcher fence = FENCE.matcher(line);
            if (fence.matches()) {
                String marker = fence.group(1);
                if (fenceChar == 0) { fenceChar = marker.charAt(0); fenceLength = marker.length(); }
                else if (marker.charAt(0) == fenceChar && marker.length() >= fenceLength
                        && line.strip().equals(marker)) { fenceChar = 0; fenceLength = 0; }
                body.append(line).append('\n');
                continue;
            }
            Matcher match = HEADING.matcher(line);
            if (fenceChar == 0 && match.matches()) {
                addSection(result, heading, body);
                heading = match.group(1).strip();
                body.setLength(0);
            } else body.append(line).append('\n');
        }
        addSection(result, heading, body);
        return result;
    }

    private void addSection(List<Section> sections, String heading, StringBuilder body) {
        String content = body.toString().strip();
        if (!content.isBlank()) sections.add(new Section(heading, content));
    }

    private List<String> chunks(String content) {
        List<String> result = new ArrayList<>();
        int start = 0;
        while (start < content.length()) {
            int end = start;
            for (int i = 0; i < CHUNK_SIZE && end < content.length(); i++)
                end += Character.charCount(content.codePointAt(end));
            String chunk = content.substring(start, end).strip();
            if (!chunk.isBlank()) result.add(chunk);
            start = end;
        }
        return result;
    }

    private String truncate(String value, int limit) {
        if (value.length() <= limit) return value;
        int end = limit;
        if (Character.isHighSurrogate(value.charAt(end - 1)) && Character.isLowSurrogate(value.charAt(end))) end--;
        return value.substring(0, end).strip();
    }
}
